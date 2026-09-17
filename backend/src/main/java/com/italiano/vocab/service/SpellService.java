package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.italiano.vocab.dto.SpellWordDTO;
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
 * 拼写模式（中→意产出复习），独立于学习（extract_count）与测验（box）的体系：
 * 只认 spell_box / spell_next_review_at——答对升盒、答错归 0 明天再拼，不动另外两套的任何字段。
 * <p>
 * 防撞规则（同一词一天只出现在一种模式）：拼写队列额外排除——
 * ① 认识测验当天欠账的词（next_review_at <= 今天，测验优先级更高，也避免拼写提前泄题）；
 * ② 今天在测验模式答过的词（last_quiz_at = 今天，防短期记忆助攻）；
 * ③ 今天在学习模式完成过的词（completed_at >= 今天 0 点，刚背完的不算）；
 * ④ 今天在听写模式答过的词（last_dict_at = 今天，刚听写完的隔天再来）。
 * 从未拼写过的词（spell_next_review_at 为 NULL）视为到期——无需冷启动迁移，由防撞规则自然节流。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpellService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;

    /** 到期拼写队列（随机排序，不含答案）+ 最近未来拼写到期日（空状态提示） */
    public Map<String, Object> getDueWords() {
        LocalDate today = LocalDate.now();
        List<WordProgress> due = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                .gt(WordProgress::getExtractCount, 0)
                .and(q -> q.isNull(WordProgress::getSpellNextReviewAt)
                        .or().le(WordProgress::getSpellNextReviewAt, today))
                .and(q -> q.isNull(WordProgress::getNextReviewAt)
                        .or().gt(WordProgress::getNextReviewAt, today))
                .and(q -> q.isNull(WordProgress::getLastQuizAt)
                        .or().lt(WordProgress::getLastQuizAt, today))
                .and(q -> q.isNull(WordProgress::getCompletedAt)
                        .or().lt(WordProgress::getCompletedAt, today.atStartOfDay()))
                .and(q -> q.isNull(WordProgress::getLastDictAt)
                        .or().lt(WordProgress::getLastDictAt, today)));

        List<SpellWordDTO> words = new ArrayList<>();
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
        Collections.shuffle(words); // 拼写顺序随机，避免按位置记忆

        WordProgress next = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                        .gt(WordProgress::getSpellNextReviewAt, today)
                        .orderByAsc(WordProgress::getSpellNextReviewAt)
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", words.size());
        result.put("nextDueAt", next == null ? null : next.getSpellNextReviewAt().toString());
        result.put("words", words);
        return result;
    }

    /**
     * 答题判分 + 拼写 SRS 推进（只考单词本身；不规则变化由加练模式第 4 题型专考）。
     * 拼对升盒，拼错 → spell_box 归 0 明天再拼；返回正确答案供结果页对照。
     */
    @Transactional
    public Map<String, Object> answer(Long id, String wordInput) {
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
        boolean passed = wordCorrect;

        LocalDate today = LocalDate.now();
        int box = p.getSpellBox() == null ? 0 : p.getSpellBox();
        if (passed) {
            int next = Math.min(box + 1, WordService.REVIEW_INTERVALS.length);
            p.setSpellBox(next);
            p.setSpellNextReviewAt(today.plusDays(WordService.REVIEW_INTERVALS[next - 1]));
        } else {
            p.setSpellBox(0);
            p.setSpellNextReviewAt(today.plusDays(1));
            p.setInNotebook(true); // 拼错进错题本（幂等：已在本的词保持不动）
        }
        p.setLastSpellAt(today); // 听写防撞：今天拼过的词不进听写队列
        progressMapper.updateById(p);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", passed);
        result.put("wordCorrect", wordCorrect);
        result.put("word", w.getWord());
        result.put("meaning", w.getMeaning());
        result.put("pos", w.getPos());
        result.put("category", w.getCategory());
        result.put("irregular", tag);
        return result;
    }

    /** 组装队列项：中文释义（不含答案） */
    private SpellWordDTO toDTO(Word w) {
        SpellWordDTO dto = new SpellWordDTO();
        dto.setWordId(w.getId());
        dto.setMeaning(w.getMeaning());
        dto.setPos(w.getPos());
        dto.setCategory(w.getCategory());
        dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
        return dto;
    }
}
