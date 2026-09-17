package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.italiano.vocab.dto.DictWordDTO;
import com.italiano.vocab.dto.IrregularPointDTO;
import com.italiano.vocab.dto.IrregularWordDTO;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 加练模式：纯练习，不碰任何 SRS 盒子/完成次数。
 * 四题型：quiz（认识）、spell（拼写）、dict（听写）、irregular（不规则变化专考）。
 * 选词范围：已学过的词（extract_count > 0），随机抽取。
 * 答错只进错题本，答对什么都不记——答到一半退出也没记录（零持久化负担）。
 * <p>
 * 不规则题型：考点由语法引擎枚举（例外表 ∪ -isc 型，逐人称与规则推导比对过滤规则形式），
 * 题面不含答案；判分时现场推导正确答案——DB 手动编辑值（变位/复数/形容词 JSON）优先，
 * 引擎推导兜底。未完成时不考（用户尚未学习，学后补，见 AGENTS.md TODO）。
 * 不规则变化已从拼写/听写/加练拼写/加练听写的附加题中撤下，统一由本题型专考。
 */
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final ObjectMapper objectMapper;

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

        // 随机打乱
        Collections.shuffle(learned);

        // 不规则题型：只有带不规则考点的词才有题，从全量已学词里筛够 count 个（整词入队，考点全出）
        if ("irregular".equals(type)) {
            Map<Long, Word> wordById = wordMapper.selectBatchIds(
                            learned.stream().map(WordProgress::getWordId).toList()).stream()
                    .collect(Collectors.toMap(Word::getId, Function.identity()));
            List<IrregularWordDTO> words = new ArrayList<>();
            for (WordProgress p : learned) {
                Word w = wordById.get(p.getWordId());
                if (w == null) {
                    continue;
                }
                List<IrregularPointDTO> points = buildIrregularPoints(w);
                if (points.isEmpty()) {
                    continue;
                }
                words.add(buildIrregularDTO(w, points));
                if (words.size() >= count) {
                    break;
                }
            }
            if (words.isEmpty()) {
                return emptyResult(type, "已学词里还没有带不规则变化的词。");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", words.size());
            result.put("words", words);
            return result;
        }

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
     * 判分（quiz/spell/dict）：答错 → 进错题本；答对 → 什么都不记。不碰 SRS 盒子、不记答题时间。
     * 返回正确答案结构（与 spell/dict/quiz 判分返回字段一致，便于前端复用结果展示）。
     */
    @Transactional
    public Map<String, Object> answer(String type, Long wordId, String wordInput,
                                      String meaningInput, boolean know) {
        Word w = wordMapper.selectById(wordId);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        String tag = ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender());

        boolean passed;
        boolean wordCorrect = true;
        boolean meaningCorrect = true;

        switch (type) {
            case "quiz" -> passed = know;
            case "spell" -> {
                wordCorrect = ExtraFormService.normalize(wordInput).equals(ExtraFormService.normalize(w.getWord()));
                passed = wordCorrect;
            }
            case "dict" -> {
                wordCorrect = ExtraFormService.normalize(wordInput).equals(ExtraFormService.normalize(w.getWord()));
                meaningCorrect = matchMeaning(meaningInput, w.getMeaning());
                passed = wordCorrect && meaningCorrect;
            }
            default -> throw new IllegalArgumentException("未知题型：" + type);
        }

        // 唯一副作用：答错进错题本（幂等）
        if (!passed) {
            markNotebook(wordId);
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("passed", passed);
        r.put("wordCorrect", wordCorrect);
        r.put("meaningCorrect", meaningCorrect);
        r.put("word", w.getWord());
        r.put("meaning", w.getMeaning());
        r.put("pos", w.getPos());
        r.put("category", w.getCategory());
        r.put("irregular", tag);
        return r;
    }

    /**
     * 不规则加练判分：题目接口不含答案，此处按考点描述现场推导正确答案比对。
     * 答错只进错题本，不碰任何 SRS 盒子/次数；返回正确答案供结果对照。
     */
    @Transactional
    public Map<String, Object> answerIrregular(Long wordId, String type, String person,
                                               String contextNoun, String contextGender,
                                               Boolean contextPlural, String input) {
        Word w = wordMapper.selectById(wordId);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        String answer = resolveIrregularAnswer(w, type, person, contextNoun, contextGender, contextPlural);
        boolean passed = answer != null && matchesAny(input, answer);
        if (!passed) {
            markNotebook(wordId);
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("passed", passed);
        r.put("answer", answer);
        r.put("word", w.getWord());
        r.put("meaning", w.getMeaning());
        r.put("pos", w.getPos());
        r.put("category", w.getCategory());
        r.put("irregular", ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
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

    // ===== 不规则考点枚举与答案推导 =====

    /**
     * 枚举一个词的全部不规则考点（不含答案）：
     * - 动词：现在时逐人称、过去分词、简单将来时逐人称（与规则推导比对，规则形式不考）
     * - 名词：不规则复数（-ca/-ga/-cia/-gia 拼写陷阱词不在此列，规则可推导）
     * - 形容词：bello 型定语形式（BELLO_PRACTICE 语境名词）、-co/-go 硬软音阳性复数、不变形容词复数
     * 不考：未完成时（未学）、阴阳性特殊/性别需记（非变形考点）
     */
    private List<IrregularPointDTO> buildIrregularPoints(Word w) {
        List<IrregularPointDTO> points = new ArrayList<>();
        String tag = ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender());
        String lw = w.getWord().toLowerCase();
        if (w.getPos() != null && w.getPos().startsWith("v.")) {
            boolean reflexive = lw.endsWith("si");
            String infinitive = reflexive ? lw.substring(0, lw.length() - 2) + "e" : lw;
            addPersonPoints(points, "present", "现在时",
                    ItalianGrammarUtil.irregularPresent(infinitive),
                    ItalianGrammarUtil.regularPresent(infinitive), reflexive);
            if (tag != null && tag.contains("近过去时不规则")) {
                points.add(point("pp", "过去分词"));
            }
            addPersonPoints(points, "futuro", "简单将来时",
                    ItalianGrammarUtil.irregularFuturo(infinitive),
                    ItalianGrammarUtil.regularFuturo(infinitive), reflexive);
        } else if (ItalianGrammarUtil.isNounPos(w.getPos())) {
            if (tag != null && tag.contains("不规则复数")) {
                points.add(point("plural", "复数形式"));
            }
        } else if (w.getPos() != null && w.getPos().contains("agg.")) {
            if (tag != null && tag.contains("冠词式变化")) {
                for (String[] ctx : ItalianGrammarUtil.BELLO_PRACTICE) {
                    boolean plural = "pl".equals(ctx[3]);
                    String answer = ItalianGrammarUtil.belloAttributive(lw, ctx[0], ctx[2], plural);
                    // 规则式复数（buono→buoni）无加练价值，跳过
                    if (answer == null || isRegularPlural(lw, answer)) {
                        continue;
                    }
                    IrregularPointDTO p = point("bello", "定语形式");
                    p.setContextNoun(ctx[0]);
                    p.setContextMeaning(ctx[1]);
                    p.setContextGender(ctx[2]);
                    p.setContextPlural(plural);
                    points.add(p);
                }
            } else if (ItalianGrammarUtil.isInvariantAdjective(lw)) {
                points.add(point("adjInv", "复数形式（性数不变）"));
            } else if (tag != null && tag.contains("不规则变化")) {
                points.add(point("adjMp", "阳性复数"));
            }
        }
        return points;
    }

    /** 动词逐人称筛考点：不规则形式与规则推导相同的人称不考（prendere 整表、andare 的 noi/voi 被过滤） */
    private static void addPersonPoints(List<IrregularPointDTO> points, String type, String tenseLabel,
                                        String[] irregular, String[] regular, boolean reflexive) {
        if (irregular == null || regular == null) {
            return;
        }
        for (int i = 0; i < ItalianGrammarUtil.PERSONS.length; i++) {
            if (!irregular[i].equals(regular[i])) {
                String person = ItalianGrammarUtil.PERSONS[i];
                IrregularPointDTO p = point(type, tenseLabel + " · " + person);
                p.setPerson(person);
                if (reflexive) {
                    p.setLabel(p.getLabel() + "（含自反代词）");
                }
                points.add(p);
            }
        }
    }

    /** 现场推导考点正确答案：DB 手动编辑值（变位/复数/形容词 JSON）优先，缺失回退引擎推导 */
    private String resolveIrregularAnswer(Word w, String type, String person,
                                          String contextNoun, String contextGender, Boolean contextPlural) {
        boolean plural = Boolean.TRUE.equals(contextPlural);
        return switch (type) {
            case "present" -> conjugationForm(w, "present", person);
            case "pp" -> participleFromDb(w);
            case "futuro" -> conjugationForm(w, "futuro", person);
            case "plural" -> w.getPlural() != null && !w.getPlural().isBlank()
                    ? w.getPlural()
                    : ItalianGrammarUtil.buildPlural(w.getWord(), w.getPos());
            case "bello" -> ItalianGrammarUtil.belloAttributive(w.getWord(), contextNoun, contextGender, plural);
            case "adjMp" -> adjectiveFormFromDb(w, "mp");
            case "adjInv" -> w.getWord();
            default -> throw new IllegalArgumentException("未知考点类型：" + type);
        };
    }

    /** 变位真值：DB 变位 JSON 优先（含手动修正），缺失回退引擎推导（反身代词由引擎统一处理） */
    private String conjugationForm(Word w, String tense, String person) {
        if (w.getConjugation() != null && !w.getConjugation().isBlank()) {
            try {
                var node = objectMapper.readTree(w.getConjugation()).path(tense).path(person);
                if (!node.isMissingNode()) {
                    String v = node.asText();
                    if (!v.isBlank() && !"—".equals(v)) {
                        return v;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        Map<String, Map<String, String>> conj = ItalianGrammarUtil.buildConjugation(w.getWord(), w.getPos());
        if (conj != null) {
            Map<String, String> tenseMap = conj.get(tense);
            if (tenseMap != null) {
                String f = tenseMap.get(person);
                if (f != null && !f.isBlank()) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * 过去分词真值：DB 近过去 io 形式（ho preso / sono andato/a / mi sono alzato/a / ho/sono vissuto）
     * 剥助动词与性数配合标注取裸分词；缺失回退引擎分词表。
     */
    private String participleFromDb(Word w) {
        String pp = conjugationForm(w, "passatoProssimo", "io");
        if (pp != null) {
            String[] parts = pp.trim().split("\\s+");
            String participle = parts[parts.length - 1].split("/")[0];
            if (!participle.isBlank() && !"—".equals(participle)) {
                return participle;
            }
        }
        String lw = w.getWord().toLowerCase();
        boolean reflexive = lw.endsWith("si");
        return ItalianGrammarUtil.pastParticiple(reflexive ? lw.substring(0, lw.length() - 2) + "e" : lw);
    }

    /** 形容词形式真值：DB adjForms JSON 优先（含手动修正），缺失回退引擎推导 */
    private String adjectiveFormFromDb(Word w, String key) {
        if (w.getAdjForms() != null && !w.getAdjForms().isBlank()) {
            try {
                var node = objectMapper.readTree(w.getAdjForms()).path(key);
                if (!node.isMissingNode()) {
                    String v = node.asText();
                    if (!v.isBlank() && !"—".equals(v)) {
                        return v;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        Map<String, String> forms = ItalianGrammarUtil.buildAdjectiveForms(w.getWord(), w.getPos());
        return forms == null ? null : forms.get(key);
    }

    /** 判分：输入归一化后与答案比对；"/" 分隔的多形式（colleghi/colleghe、piloti/pilote）任一命中即对 */
    private static boolean matchesAny(String input, String answer) {
        String normalized = ExtraFormService.normalize(input);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String alt : answer.split("/")) {
            if (normalized.equals(ExtraFormService.normalize(alt))) {
                return true;
            }
        }
        return false;
    }

    /** bello 型语境答案若等于规则式复数（buono→buoni）则无加练价值 */
    private static boolean isRegularPlural(String adjective, String answer) {
        return answer.equals(adjective.substring(0, adjective.length() - 1) + "i");
    }

    private static IrregularPointDTO point(String type, String label) {
        IrregularPointDTO p = new IrregularPointDTO();
        p.setType(type);
        p.setLabel(label);
        return p;
    }

    private IrregularWordDTO buildIrregularDTO(Word w, List<IrregularPointDTO> points) {
        IrregularWordDTO dto = new IrregularWordDTO();
        dto.setWordId(w.getId());
        dto.setWord(w.getWord());
        dto.setPos(w.getPos());
        dto.setMeaning(w.getMeaning());
        dto.setCategory(w.getCategory());
        dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
        dto.setPoints(points);
        return dto;
    }

    // ===== 通用工具（quiz/spell/dict 三题型）=====

    /** 答错进错题本（幂等：已在本的词保持不动） */
    private void markNotebook(Long wordId) {
        WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getWordId, wordId));
        if (p != null && !Boolean.TRUE.equals(p.getInNotebook())) {
            p.setInNotebook(true);
            progressMapper.updateById(p);
        }
    }

    private SpellWordDTO buildSpellDTO(Word w) {
        SpellWordDTO dto = new SpellWordDTO();
        dto.setWordId(w.getId());
        dto.setMeaning(w.getMeaning());
        dto.setPos(w.getPos());
        dto.setCategory(w.getCategory());
        dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
        return dto;
    }

    private DictWordDTO buildDictDTO(Word w) {
        DictWordDTO dto = new DictWordDTO();
        dto.setWordId(w.getId());
        dto.setWord(w.getWord());
        dto.setPos(w.getPos());
        dto.setCategory(w.getCategory());
        dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
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

    private Map<String, Object> emptyResult(String type, String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", 0);
        r.put("words", List.of());
        r.put("message", msg);
        return r;
    }
}
