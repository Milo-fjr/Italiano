package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.italiano.vocab.dto.DictWordDTO;
import com.italiano.vocab.entity.Setting;
import com.italiano.vocab.entity.Word;
import com.italiano.vocab.entity.WordProgress;
import com.italiano.vocab.mapper.WordMapper;
import com.italiano.vocab.mapper.WordProgressMapper;
import com.italiano.vocab.util.ItalianGrammarUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 听写模式（听音→写词 + 释义 4 选 1 产出复习），独立于学习（extract_count）/测验（box）/拼写（spell_box）的体系：
 * 只认 dict_box / dict_next_review_at——全对升盒、有错归 0 明天再听，不动另外三套的任何字段。
 * 答题产出：意大利语单词手写（听音拼写，归一化容错同拼写）+ 中文释义选择题（4 选项随机，点选判定，
 * 消除手打中文的错别字/格式误判）；不规则变化由加练模式第 4 题型专考，不再出附加题。
 * <p>
 * 防撞规则（同一词一天只出现在一种模式）：听写队列额外排除——
 * ① 认识测验当天欠账的词（next_review_at <= 今天，测验优先级更高）；
 * ② 今天在测验模式答过的词（last_quiz_at = 今天）；
 * ③ 今天在学习模式完成过的词（completed_at >= 今天 0 点）；
 * ④ 今天在拼写模式答过的词（last_spell_at = 今天，刚拼过的隔天再来）。
 * 从未听写的词（dict_next_review_at 为 NULL）视为到期——由防撞规则自然节流。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final SettingService settingService;

    /** 到期听写队列（含单词原文供前端 TTS）+ 最近未来听写到期日（空状态提示）。
     * 每日配额（setting.dict_daily_limit，0=不限制）：按「今天已答数」（last_dict_at = 今天）计，
     * 只钳制自动首载——all=false 答满 limit 即返回空队列（剩余到期词保持到期状态明天继续）；
     * all=true 为前端「重新加载」的手动续池：无视上限补齐全部到期词（今天听写过的已由防撞出池）。
     * 未答满时按最欠账优先抽取（到期日最早先出，同日随机；从未听写过的 NULL 池垫底）。
     * 返回 poolTotal / answeredToday / dailyLimit / quotaReached，供前端提示与导航红点同口径。 */
    public Map<String, Object> getDueWords(boolean all) {
        LocalDate today = LocalDate.now();
        Setting cfg = settingService.getSetting();
        int limit = cfg.getDictDailyLimit() == null ? 0 : cfg.getDictDailyLimit();
        long answeredToday = progressMapper.selectCount(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getLastDictAt, today));
        boolean quotaReached = limit > 0 && answeredToday >= limit;
        long poolTotal = progressMapper.selectCount(new LambdaQueryWrapper<WordProgress>()
                .gt(WordProgress::getExtractCount, 0)
                .and(q -> q.isNull(WordProgress::getDictNextReviewAt)
                        .or().le(WordProgress::getDictNextReviewAt, today))
                .and(q -> q.isNull(WordProgress::getNextReviewAt)
                        .or().gt(WordProgress::getNextReviewAt, today))
                .and(q -> q.isNull(WordProgress::getLastQuizAt)
                        .or().lt(WordProgress::getLastQuizAt, today))
                .and(q -> q.isNull(WordProgress::getCompletedAt)
                        .or().lt(WordProgress::getCompletedAt, today.atStartOfDay()))
                .and(q -> q.isNull(WordProgress::getLastSpellAt)
                        .or().lt(WordProgress::getLastSpellAt, today)));

        List<WordProgress> due;
        if (poolTotal == 0 || (!all && quotaReached)) {
            due = List.of();
        } else {
            LambdaQueryWrapper<WordProgress> query = new LambdaQueryWrapper<WordProgress>()
                    .gt(WordProgress::getExtractCount, 0)
                    .and(q -> q.isNull(WordProgress::getDictNextReviewAt)
                            .or().le(WordProgress::getDictNextReviewAt, today))
                    .and(q -> q.isNull(WordProgress::getNextReviewAt)
                            .or().gt(WordProgress::getNextReviewAt, today))
                    .and(q -> q.isNull(WordProgress::getLastQuizAt)
                            .or().lt(WordProgress::getLastQuizAt, today))
                    .and(q -> q.isNull(WordProgress::getCompletedAt)
                            .or().lt(WordProgress::getCompletedAt, today.atStartOfDay()))
                    .and(q -> q.isNull(WordProgress::getLastSpellAt)
                            .or().lt(WordProgress::getLastSpellAt, today));
            if (all) {
                // 手动续池：无视每日上限补齐全部到期词（NULL 池垫底同序）
                query.last("ORDER BY dict_next_review_at IS NULL ASC, dict_next_review_at ASC, RAND()");
            } else {
                int remaining = limit > 0 ? (int) Math.max(0, limit - answeredToday) : Integer.MAX_VALUE;
                if (remaining < poolTotal) {
                    // 最欠账优先：NULL 池（从未听写过）垫底，有到期日的按日期升序，同日随机
                    query.last("ORDER BY dict_next_review_at IS NULL ASC, dict_next_review_at ASC, RAND() LIMIT " + remaining);
                }
            }
            due = progressMapper.selectList(query);
        }

        List<DictWordDTO> words = new ArrayList<>();
        if (!due.isEmpty()) {
            Map<Long, Word> wordById = wordMapper.selectBatchIds(due.stream()
                            .map(WordProgress::getWordId).toList()).stream()
                    .collect(Collectors.toMap(Word::getId, Function.identity()));
            for (WordProgress p : due) {
                Word w = wordById.get(p.getWordId());
                if (w == null) {
                    continue;
                }
                words.add(toDTO(w));
            }
        }
        if (limit <= 0) {
            Collections.shuffle(words); // 听写顺序随机，避免按位置记忆（限流时已按欠账排序、同日随机）
        }

        WordProgress next = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                        .gt(WordProgress::getDictNextReviewAt, today)
                        .orderByAsc(WordProgress::getDictNextReviewAt)
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", words.size());
        result.put("poolTotal", poolTotal);
        result.put("answeredToday", answeredToday);
        result.put("dailyLimit", limit);
        result.put("quotaReached", quotaReached);
        result.put("nextDueAt", next == null ? null : next.getDictNextReviewAt().toString());
        result.put("words", words);
        return result;
    }

    /**
     * 释义预检（两段式第一关）：只判对错、不动 SRS——选对才进入拼写关，选错由前端再调 answer 落判错。
     */
    public Map<String, Object> checkMeaning(Long id, String meaningInput) {
        Word w = wordMapper.selectById(id);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correct", ExtraFormService.matchMeaning(meaningInput, w.getMeaning()));
        return result;
    }

    /**
     * 答题判分 + 听写 SRS 推进（单词 + 释义全对才升盒；不规则变化由加练模式第 4 题型专考）。
     * 判错自动进错题本；返回正确答案供结果页对照。
     */
    @Transactional
    public Map<String, Object> answer(Long id, String wordInput, String meaningInput) {
        Word w = wordMapper.selectById(id);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getWordId, id));
        if (p == null) {
            throw new IllegalArgumentException("该单词还没有学习记录");
        }

        String tag = ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender());
        boolean wordCorrect = ExtraFormService.normalize(wordInput).equals(ExtraFormService.normalize(w.getWord()));
        boolean meaningCorrect = ExtraFormService.matchMeaning(meaningInput, w.getMeaning());
        boolean passed = wordCorrect && meaningCorrect;

        LocalDate today = LocalDate.now();
        int box = p.getDictBox() == null ? 0 : p.getDictBox();
        int boxBefore = box; // 答错前快照（供「手滑了，改判对」完全恢复）
        boolean notebookBefore = Boolean.TRUE.equals(p.getInNotebook());
        if (passed) {
            int next = Math.min(box + 1, WordService.REVIEW_INTERVALS.length);
            p.setDictBox(next);
            p.setDictNextReviewAt(today.plusDays(WordService.REVIEW_INTERVALS[next - 1]));
        } else {
            p.setDictBox(0);
            p.setDictNextReviewAt(today.plusDays(1));
            p.setInNotebook(true); // 听错进错题本（幂等：已在本的词保持不动）
        }
        p.setLastDictAt(today); // 拼写防撞：今天听写过的词不进拼写队列
        progressMapper.updateById(p);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", passed);
        result.put("wordCorrect", wordCorrect);
        result.put("meaningCorrect", meaningCorrect);
        result.put("word", w.getWord());
        result.put("meaning", w.getMeaning());
        result.put("pos", w.getPos());
        result.put("category", w.getCategory());
        result.put("irregular", tag);
        result.put("boxBefore", boxBefore);
        result.put("notebookBefore", notebookBefore);
        return result;
    }

    /**
     * 误触改判：把一次「手滑打错」的判错恢复成答对（仅限拼写关误触——释义是点选不存在误触）——
     * 盒子按答错前等级 +1、下次复习按新等级排期、错题本还原到答错前状态。快照值来自判错响应（零历史表）。
     */
    @Transactional
    public Map<String, Object> typoFix(Long id, Integer boxBefore, Boolean notebookBefore) {
        Word w = wordMapper.selectById(id);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getWordId, id));
        if (p == null) {
            throw new IllegalArgumentException("该单词还没有学习记录");
        }

        LocalDate today = LocalDate.now();
        int box = Math.min((boxBefore == null ? 0 : boxBefore) + 1, WordService.REVIEW_INTERVALS.length);
        p.setDictBox(box);
        p.setDictNextReviewAt(today.plusDays(WordService.REVIEW_INTERVALS[box - 1]));
        if (notebookBefore != null) {
            p.setInNotebook(notebookBefore);
        }
        p.setLastDictAt(today); // 今天答过的既成事实保留（防撞口径不变）
        progressMapper.updateById(p);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", true);
        result.put("wordCorrect", true);
        result.put("meaningCorrect", true);
        result.put("word", w.getWord());
        result.put("meaning", w.getMeaning());
        result.put("pos", w.getPos());
        result.put("category", w.getCategory());
        result.put("irregular", ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
        return result;
    }

    /** 组装队列项：单词原文（TTS 用）+ 释义 4 选 1 选项（不含答案） */
    private DictWordDTO toDTO(Word w) {
        DictWordDTO dto = new DictWordDTO();
        dto.setWordId(w.getId());
        dto.setWord(w.getWord());
        dto.setPos(w.getPos());
        dto.setCategory(w.getCategory());
        dto.setMeaningOptions(buildMeaningOptions(w));
        dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
        return dto;
    }

    /**
     * 释义选项：正确释义 + 3 个随机干扰项（从词库随机抽，归一化后与正确释义及彼此不重复），整体打乱顺序。
     */
    private List<String> buildMeaningOptions(Word w) {
        List<String> options = new ArrayList<>();
        options.add(w.getMeaning());

        List<Word> candidates = wordMapper.selectList(new LambdaQueryWrapper<Word>()
                .select(Word::getMeaning)
                .ne(Word::getId, w.getId())
                .last("ORDER BY RAND() LIMIT 20"));
        for (Word c : candidates) {
            if (options.size() >= 4) {
                break;
            }
            String m = c.getMeaning();
            boolean dup = options.stream().anyMatch(o -> ExtraFormService.normalizeMeaning(o).equals(ExtraFormService.normalizeMeaning(m)));
            if (!dup) {
                options.add(m);
            }
        }
        Collections.shuffle(options);
        return options;
    }
}
