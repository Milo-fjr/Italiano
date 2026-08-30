package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 拼写模式（中→意产出复习），独立于学习（extract_count）与测验（box）的第三套体系：
 * 只认 spell_box / spell_next_review_at——答对升盒、答错归 0 明天再拼，不动另外两套的任何字段。
 * <p>
 * 防撞规则（同一词一天只出现在一种模式）：拼写队列额外排除——
 * ① 认识测验当天欠账的词（next_review_at <= 今天，测验优先级更高，也避免拼写提前泄题）；
 * ② 今天在测验模式答过的词（last_quiz_at = 今天，防短期记忆助攻）；
 * ③ 今天在学习模式完成过的词（completed_at >= 今天 0 点，刚背完的不算）。
 * 从未拼写过的词（spell_next_review_at 为 NULL）视为到期——无需冷启动迁移，由防撞规则自然节流。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpellService {

    /** 附加填写类型：现在时 io 形式 */
    private static final String EXTRA_PRESENT_IO = "presentIo";
    /** 附加填写类型：名词复数 */
    private static final String EXTRA_PLURAL = "plural";

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final ObjectMapper objectMapper;

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
                        .or().lt(WordProgress::getCompletedAt, today.atStartOfDay())));

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
     * 答题判分 + 拼写 SRS 推进。
     * 单词与附加形式全部正确才算过（任一错误 → spell_box 归 0 明天再拼）；
     * 返回正确答案供结果页对照。
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
        Extra extra = resolveExtra(w, tag);

        boolean wordCorrect = normalize(wordInput).equals(normalize(w.getWord()));
        Boolean extraCorrect = null;
        if (extra != null) {
            extraCorrect = normalize(extraInput).equals(normalize(extra.answer()));
        }
        boolean passed = wordCorrect && (extra == null || extraCorrect);

        LocalDate today = LocalDate.now();
        int box = p.getSpellBox() == null ? 0 : p.getSpellBox();
        if (passed) {
            int next = Math.min(box + 1, WordService.REVIEW_INTERVALS.length);
            p.setSpellBox(next);
            p.setSpellNextReviewAt(today.plusDays(WordService.REVIEW_INTERVALS[next - 1]));
        } else {
            p.setSpellBox(0);
            p.setSpellNextReviewAt(today.plusDays(1));
        }
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

    /** 组装队列项：中文释义 + 附加填写提示（不含答案） */
    private SpellWordDTO toDTO(Word w) {
        SpellWordDTO dto = new SpellWordDTO();
        dto.setWordId(w.getId());
        dto.setMeaning(w.getMeaning());
        dto.setPos(w.getPos());
        dto.setCategory(w.getCategory());
        String tag = ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender());
        dto.setIrregular(tag);
        Extra extra = resolveExtra(w, tag);
        if (extra != null) {
            dto.setExtraType(extra.type());
            dto.setExtraLabel(extra.label());
        }
        return dto;
    }

    /**
     * 附加填写判定：不规则动词（现在时不规则/音变）→ 现在时 io 形式（conosco/vado/viaggio，
     * 覆盖 -isc、双写、音变陷阱）；不规则名词（不规则复数/复数不变/音变）→ 复数。
     * 形容词与性别类标签不考（斜杠多形式难校验/非拼写范畴）；只在其他时态不规则的动词无附加题。
     */
    private Extra resolveExtra(Word w, String tag) {
        if (tag == null) {
            return null;
        }
        if (w.getPos() != null && w.getPos().startsWith("v.")
                && (tag.contains("现在时不规则") || tag.contains("音变"))) {
            String io = extractPresentIo(w.getConjugation());
            if (io != null) {
                return new Extra(EXTRA_PRESENT_IO, "现在时 io 形式", io);
            }
            return null;
        }
        if (ItalianGrammarUtil.isNounPos(w.getPos())
                && w.getPlural() != null && !w.getPlural().isBlank()
                && (tag.contains("不规则复数") || tag.contains("复数不变") || tag.contains("音变"))) {
            return new Extra(EXTRA_PLURAL, "复数形式", w.getPlural());
        }
        return null;
    }

    /** 从变位 JSON 取现在时 io 形式；缺失或占位符（—）返回 null */
    private String extractPresentIo(String conjugationJson) {
        if (conjugationJson == null || conjugationJson.isBlank()) {
            return null;
        }
        try {
            var node = objectMapper.readTree(conjugationJson).path("present").path("io");
            if (node.isMissingNode()) {
                return null;
            }
            String v = node.asText();
            return (v == null || v.isBlank() || "—".equals(v)) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    /** 输入归一化：trim + 小写 + NFD 去重音（è→e、à→a）+ 折叠连续空格——中文键盘打不出重音符号 */
    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ");
    }

    /** 附加填写项（type/label 对外提示，answer 仅在判分后返回） */
    private record Extra(String type, String label, String answer) {}
}
