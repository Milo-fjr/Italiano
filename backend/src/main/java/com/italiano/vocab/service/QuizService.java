package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.italiano.vocab.dto.TodayWordDTO;
import com.italiano.vocab.entity.Setting;
import com.italiano.vocab.entity.Word;
import com.italiano.vocab.entity.WordProgress;
import com.italiano.vocab.mapper.WordMapper;
import com.italiano.vocab.mapper.WordProgressMapper;
import com.italiano.vocab.util.ItalianGrammarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 测验模式（SRS 到期复习），与学习批次是两套独立体系：
 * 学习模式按完成次数（extract_count）流转抽词，测验模式只认盒子——next_review_at 到期即测。
 * 测验答题只动盒子不动完成次数（认识 → 盒 +1；不认识 → 盒归 0 明天再测）。
 */
@Service
@RequiredArgsConstructor
public class QuizService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final SettingService settingService;

    /**
     * 到期测验词（next_review_at <= 今天，不筛 box——测验答错的词归 0 后明天到期也能回来）；
     * 附带最近一次未来到期日（无到期词时的空状态提示）。
     * 每日配额（setting.quiz_daily_limit，0=不限制）：按「今天已答数」（last_quiz_at = 今天）计，
     * 答满 limit 即返回空队列——剩余到期词保持到期状态明天继续（真·每日配额，不是每次抽题上限）；
     * 未答满时按最欠账优先抽取（到期日最早先出，同日随机）。
     * 返回 poolTotal（到期池全量）/ answeredToday（今日已答）/ dailyLimit / quotaReached，供前端提示与导航红点同口径。
     */
    public Map<String, Object> getDueWords() {
        LocalDate today = LocalDate.now();
        Setting cfg = settingService.getSetting();
        int limit = cfg.getQuizDailyLimit() == null ? 0 : cfg.getQuizDailyLimit();
        long answeredToday = progressMapper.selectCount(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getLastQuizAt, today));
        boolean quotaReached = limit > 0 && answeredToday >= limit;
        long poolTotal = progressMapper.selectCount(new LambdaQueryWrapper<WordProgress>()
                .isNotNull(WordProgress::getNextReviewAt)
                .le(WordProgress::getNextReviewAt, today));

        List<WordProgress> due;
        if (quotaReached || poolTotal == 0) {
            due = List.of();
        } else {
            int remaining = limit > 0 ? (int) Math.max(0, limit - answeredToday) : Integer.MAX_VALUE;
            LambdaQueryWrapper<WordProgress> query = new LambdaQueryWrapper<WordProgress>()
                    .isNotNull(WordProgress::getNextReviewAt)
                    .le(WordProgress::getNextReviewAt, today);
            if (remaining < poolTotal) {
                query.last("ORDER BY next_review_at ASC, RAND() LIMIT " + remaining); // 最欠账优先，同日随机
            }
            due = progressMapper.selectList(query);
        }

        List<TodayWordDTO> words = new ArrayList<>();
        if (!due.isEmpty()) {
            Map<Long, Word> wordById = wordMapper.selectBatchIds(due.stream()
                            .map(WordProgress::getWordId).toList()).stream()
                    .collect(Collectors.toMap(Word::getId, Function.identity()));
            for (WordProgress p : due) {
                Word w = wordById.get(p.getWordId());
                if (w == null) {
                    continue;
                }
                TodayWordDTO dto = new TodayWordDTO();
                dto.setWordId(w.getId());
                dto.setWord(w.getWord());
                dto.setPos(w.getPos());
                dto.setMeaning(w.getMeaning());
                dto.setCategory(w.getCategory());
                dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
                words.add(dto);
            }
        }
        if (limit <= 0) {
            Collections.shuffle(words); // 测验顺序随机，避免按位置记忆（限流时已按到期日排序、同日随机）
        }

        WordProgress next = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                        .gt(WordProgress::getNextReviewAt, today)
                        .orderByAsc(WordProgress::getNextReviewAt)
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", words.size());
        result.put("poolTotal", poolTotal);
        result.put("answeredToday", answeredToday);
        result.put("dailyLimit", limit);
        result.put("quotaReached", quotaReached);
        result.put("nextDueAt", next == null ? null : next.getNextReviewAt().toString());
        result.put("words", words);
        return result;
    }
}
