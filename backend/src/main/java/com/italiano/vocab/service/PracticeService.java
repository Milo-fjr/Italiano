package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.italiano.vocab.dto.DictWordDTO;
import com.italiano.vocab.dto.SpellWordDTO;
import com.italiano.vocab.dto.TodayWordDTO;
import com.italiano.vocab.entity.Word;
import com.italiano.vocab.entity.WordProgress;
import com.italiano.vocab.mapper.WordMapper;
import com.italiano.vocab.mapper.WordProgressMapper;
import com.italiano.vocab.util.ItalianGrammarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 加练模式：纯练习，不碰任何 SRS 盒子/完成次数。
 * 三题型：quiz（认识）、spell（拼写）、dict（听写）。
 * 选词范围：已学过的词（extract_count > 0），随机抽取。
 * 答错只进错题本，答对什么都不记——答到一半退出也没记录（零持久化负担）。
 */
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final ExtraFormService extraFormService;

    /** 从已学词里随机抽 count 个，按题型组装 DTO */
    public Map<String, Object> draw(String type, int count) {
        if (count <= 0) count = 20;
        if (count > 100) count = 100;

        // 已学过的词（有学习记录且 extract_count > 0）
        List<WordProgress> learned = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                .gt(WordProgress::getExtractCount, 0));
        if (learned.isEmpty()) {
            return emptyResult(type, "还没有学过的词，先去学习模式背一些吧。");
        }

        // 随机打乱取前 count 个
        Collections.shuffle(learned);
        List<WordProgress> picked = learned.subList(0, Math.min(count, learned.size()));

        List<Long> ids = picked.stream().map(WordProgress::getWordId).toList();
        Map<Long, Word> wordById = wordMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Word::getId, Function.identity()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", picked.size());

        switch (type) {
            case "quiz" -> {
                List<TodayWordDTO> words = new ArrayList<>();
                for (WordProgress p : picked) {
                    Word w = wordById.get(p.getWordId());
                    if (w == null) continue;
                    TodayWordDTO dto = new TodayWordDTO();
                    dto.setWordId(w.getId());
                    dto.setWord(w.getWord());
                    dto.setPos(w.getPos());
                    dto.setMeaning(w.getMeaning());
                    dto.setCategory(w.getCategory());
                    dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
                    words.add(dto);
                }
                result.put("words", words);
            }
            case "spell" -> {
                List<SpellWordDTO> words = new ArrayList<>();
                for (WordProgress p : picked) {
                    Word w = wordById.get(p.getWordId());
                    if (w == null) continue;
                    words.add(buildSpellDTO(w));
                }
                result.put("words", words);
            }
            case "dict" -> {
                List<DictWordDTO> words = new ArrayList<>();
                for (WordProgress p : picked) {
                    Word w = wordById.get(p.getWordId());
                    if (w == null) continue;
                    words.add(buildDictDTO(w));
                }
                result.put("words", words);
            }
            default -> throw new IllegalArgumentException("未知题型：" + type);
        }
        return result;
    }

    /**
     * 判分：答错 → 进错题本；答对 → 什么都不记。不碰 SRS 盒子、不记答题时间。
     * 返回正确答案结构（与 spell/dict/quiz 判分返回字段一致，便于前端复用结果展示）。
     */
    @Transactional
    public Map<String, Object> answer(String type, Long wordId, String wordInput,
                                      String extraInput, String meaningInput, boolean know) {
        Word w = wordMapper.selectById(wordId);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        String tag = ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender());
        ExtraFormService.Extra extra = extraFormService.resolve(w, tag);

        boolean passed;
        boolean wordCorrect = true;
        boolean meaningCorrect = true;
        Boolean extraCorrect = null;

        switch (type) {
            case "quiz" -> passed = know;
            case "spell" -> {
                wordCorrect = norm(wordInput).equals(norm(w.getWord()));
                if (extra != null) {
                    extraCorrect = norm(extraInput).equals(norm(extra.answer()));
                }
                passed = wordCorrect && (extra == null || extraCorrect);
            }
            case "dict" -> {
                wordCorrect = norm(wordInput).equals(norm(w.getWord()));
                meaningCorrect = matchMeaning(meaningInput, w.getMeaning());
                if (extra != null) {
                    extraCorrect = norm(extraInput).equals(norm(extra.answer()));
                }
                passed = wordCorrect && meaningCorrect && (extra == null || extraCorrect);
            }
            default -> throw new IllegalArgumentException("未知题型：" + type);
        }

        // 唯一副作用：答错进错题本（幂等）
        if (!passed) {
            WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                    .eq(WordProgress::getWordId, wordId));
            if (p != null && !Boolean.TRUE.equals(p.getInNotebook())) {
                p.setInNotebook(true);
                progressMapper.updateById(p);
            }
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("passed", passed);
        r.put("wordCorrect", wordCorrect);
        r.put("meaningCorrect", meaningCorrect);
        r.put("extraCorrect", extraCorrect);
        r.put("word", w.getWord());
        r.put("meaning", w.getMeaning());
        r.put("pos", w.getPos());
        r.put("category", w.getCategory());
        r.put("irregular", tag);
        r.put("extraLabel", extra == null ? null : extra.label());
        r.put("extraAnswer", extra == null ? null : extra.answer());
        return r;
    }

    /**
     * 听写第一关释义预检（与 DictService.checkMeaning 同口径）：只判断不落库、不返回正确答案，
     * 选错由前端拿所选释义走正式 dict-answer 判错。
     */
    public boolean checkDictMeaning(Long wordId, String meaningInput) {
        Word w = wordMapper.selectById(wordId);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        return matchMeaning(meaningInput, w.getMeaning());
    }

    // ===== 私有工具（复用 DictService/ExtraFormService 逻辑，避免循环依赖/重复代码）=====

    private SpellWordDTO buildSpellDTO(Word w) {
        SpellWordDTO dto = new SpellWordDTO();
        dto.setWordId(w.getId());
        dto.setMeaning(w.getMeaning());
        dto.setPos(w.getPos());
        dto.setCategory(w.getCategory());
        String tag = ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender());
        dto.setIrregular(tag);
        ExtraFormService.Extra extra = extraFormService.resolve(w, tag);
        if (extra != null) {
            dto.setExtraType(extra.type());
            dto.setExtraLabel(extra.label());
        }
        return dto;
    }

    private DictWordDTO buildDictDTO(Word w) {
        DictWordDTO dto = new DictWordDTO();
        dto.setWordId(w.getId());
        dto.setWord(w.getWord());
        dto.setPos(w.getPos());
        dto.setCategory(w.getCategory());
        String tag = ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender());
        dto.setIrregular(tag);
        ExtraFormService.Extra extra = extraFormService.resolve(w, tag);
        if (extra != null) {
            dto.setExtraType(extra.type());
            dto.setExtraLabel(extra.label());
        }
        dto.setMeaningOptions(buildMeaningOptions(w));
        return dto;
    }

    /** 释义 4 选 1：正确释义 + 3 个随机干扰项（与 DictService 一致） */
    private List<String> buildMeaningOptions(Word w) {
        List<String> options = new ArrayList<>();
        options.add(w.getMeaning());
        List<Word> candidates = wordMapper.selectList(new LambdaQueryWrapper<Word>()
                .select(Word::getMeaning)
                .ne(Word::getId, w.getId())
                .last("ORDER BY RAND() LIMIT 20"));
        for (Word c : candidates) {
            if (options.size() >= 4) break;
            String m = c.getMeaning();
            boolean dup = options.stream().anyMatch(o -> normMeaning(o).equals(normMeaning(m)));
            if (!dup) options.add(m);
        }
        Collections.shuffle(options);
        return options;
    }

    private static boolean matchMeaning(String input, String answer) {
        if (input == null) return false;
        for (String in : input.split("[；;]")) {
            String ni = normMeaning(in);
            if (ni.isEmpty()) continue;
            for (String alt : answer.split("[；;]")) {
                if (normMeaning(alt).equals(ni)) return true;
            }
        }
        return false;
    }

    private static String normMeaning(String s) {
        if (s == null) return "";
        String t = s.trim()
                .replaceAll("（[^）]*）|\\([^)]*\\)", "")
                .replaceAll("\\s+", "");
        while (!t.isEmpty() && "的了呀啊吧呢吗地".indexOf(t.charAt(t.length() - 1)) >= 0) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ");
    }

    private Map<String, Object> emptyResult(String type, String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", 0);
        r.put("words", switch (type) {
            case "quiz", "spell", "dict" -> List.of();
            default -> List.of();
        });
        r.put("message", msg);
        return r;
    }
}
