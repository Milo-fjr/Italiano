package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.italiano.vocab.dto.DictWordDTO;
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
 * 听写模式（听音→写词产出复习），独立于学习（extract_count）/测验（box）/拼写（spell_box）的体系：
 * 只认 dict_box / dict_next_review_at——全对升盒、有错归 0 明天再听，不动另外三套的任何字段。
 * 判分口径与拼写模式完全一致（共用 ExtraFormService：归一化容错 + 不规则附加形式）。
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
    private final ExtraFormService extraFormService;

    /** 到期听写队列（随机顺序；含单词原文供前端 TTS）+ 最近未来听写到期日（空状态提示） */
    public Map<String, Object> getDueWords() {
        LocalDate today = LocalDate.now();
        List<WordProgress> due = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
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
        Collections.shuffle(words); // 听写顺序随机，避免按位置记忆

        WordProgress next = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                        .gt(WordProgress::getDictNextReviewAt, today)
                        .orderByAsc(WordProgress::getDictNextReviewAt)
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", words.size());
        result.put("nextDueAt", next == null ? null : next.getDictNextReviewAt().toString());
        result.put("words", words);
        return result;
    }

    /**
     * 答题判分 + 听写 SRS 推进（判分口径同拼写：归一化容错，全对才升盒）。
     * 判错自动进错题本；返回正确答案供结果页对照（含拼写对照，听完才知道写成什么样）。
     */
    @Transactional
    public Map<String, Object> answer(Long id, String wordInput, String extraInput) {
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
        ExtraFormService.Extra extra = extraFormService.resolve(w, tag);

        boolean wordCorrect = ExtraFormService.normalize(wordInput).equals(ExtraFormService.normalize(w.getWord()));
        Boolean extraCorrect = null;
        if (extra != null) {
            extraCorrect = ExtraFormService.normalize(extraInput).equals(ExtraFormService.normalize(extra.answer()));
        }
        boolean passed = wordCorrect && (extra == null || extraCorrect);

        LocalDate today = LocalDate.now();
        int box = p.getDictBox() == null ? 0 : p.getDictBox();
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
        result.put("extraCorrect", extraCorrect);
        result.put("word", w.getWord());
        result.put("meaning", w.getMeaning());
        result.put("pos", w.getPos());
        result.put("category", w.getCategory());
        result.put("irregular", tag);
        result.put("extraLabel", extra == null ? null : extra.label());
        result.put("extraAnswer", extra == null ? null : extra.answer());
        return result;
    }

    /** 组装队列项：单词原文（TTS 用）+ 中文释义 + 附加填写提示（不含附加答案） */
    private DictWordDTO toDTO(Word w) {
        DictWordDTO dto = new DictWordDTO();
        dto.setWordId(w.getId());
        dto.setWord(w.getWord());
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
}
