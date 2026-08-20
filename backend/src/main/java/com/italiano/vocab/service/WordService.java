package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.italiano.vocab.dto.WordDetailDTO;
import com.italiano.vocab.dto.WordListItemDTO;
import com.italiano.vocab.dto.WordUpdateDTO;
import com.italiano.vocab.entity.DailyExtract;
import com.italiano.vocab.entity.Word;
import com.italiano.vocab.entity.WordProgress;
import com.italiano.vocab.mapper.DailyExtractMapper;
import com.italiano.vocab.mapper.WordMapper;
import com.italiano.vocab.mapper.WordProgressMapper;
import com.italiano.vocab.util.ItalianGrammarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 单词服务：词库查询、详情、编辑、完成/撤销 */
@Service
@RequiredArgsConstructor
public class WordService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final DailyExtractMapper dailyExtractMapper;
    private final ObjectMapper objectMapper;

    /**
     * 单词列表：分页 + 分类/状态筛选 + 关键词搜索（单词/释义模糊）。
     * 状态：0=未学（无进度记录）1=已抽取未完成 2=已完成
     */
    public Map<String, Object> listWords(int page, int size, String category, Integer status, String keyword) {
        List<WordProgress> allProgress = progressMapper.selectList(null);
        Map<Long, WordProgress> progressMap = allProgress.stream()
                .collect(Collectors.toMap(WordProgress::getWordId, p -> p, (a, b) -> a));

        LambdaQueryWrapper<Word> qw = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            qw.eq(Word::getCategory, category);
        }
        if (keyword != null && !keyword.isBlank()) {
            qw.and(q -> q.like(Word::getWord, keyword).or().like(Word::getMeaning, keyword));
        }
        if (status != null) {
            if (status == WordProgress.STATUS_NEVER) {
                // 未学 = 无进度记录
                if (!progressMap.isEmpty()) {
                    qw.notIn(Word::getId, progressMap.keySet());
                }
            } else {
                Set<Long> ids = allProgress.stream()
                        .filter(p -> p.getStatus() != null && p.getStatus() == status)
                        .map(WordProgress::getWordId).collect(Collectors.toSet());
                if (ids.isEmpty()) {
                    return emptyPage(page, size);
                }
                qw.in(Word::getId, ids);
            }
        }
        qw.orderByAsc(Word::getId);

        Page<Word> result = wordMapper.selectPage(Page.of(page, size), qw);
        List<WordListItemDTO> items = result.getRecords().stream().map(w -> {
            WordListItemDTO dto = new WordListItemDTO();
            dto.setId(w.getId());
            dto.setWord(w.getWord());
            dto.setPos(w.getPos());
            dto.setMeaning(w.getMeaning());
            dto.setCategory(w.getCategory());
            WordProgress p = progressMap.get(w.getId());
            if (p != null) {
                dto.setExtractCount(p.getExtractCount());
                dto.setProgressStatus(p.getStatus());
                dto.setLastExtractedAt(p.getLastExtractedAt());
            } else {
                dto.setExtractCount(0);
                dto.setProgressStatus(WordProgress.STATUS_NEVER);
            }
            return dto;
        }).toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        data.put("items", items);
        return data;
    }

    private Map<String, Object> emptyPage(int page, int size) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", 0L);
        data.put("page", page);
        data.put("size", size);
        data.put("items", List.of());
        return data;
    }

    /** 单词详情：基本信息 + 定冠词（单/复数）+ 变位表 + 学习进度 */
    public WordDetailDTO getDetail(Long id) {
        Word w = wordMapper.selectById(id);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在：id=" + id);
        }
        WordDetailDTO dto = new WordDetailDTO();
        dto.setId(w.getId());
        dto.setWord(w.getWord());
        dto.setPos(w.getPos());
        dto.setMeaning(w.getMeaning());
        dto.setCategory(w.getCategory());
        dto.setGender(w.getGender());
        dto.setArticle(w.getArticle());
        dto.setArticlePlural(ItalianGrammarUtil.pluralArticle(w.getArticle(), w.getGender()));
        dto.setArticleIndefinite(ItalianGrammarUtil.indefiniteArticle(w.getWord(), w.getPos(), w.getGender()));
        dto.setPlural(w.getPlural());
        dto.setConjugation(parseConjugation(w.getConjugation()));
        dto.setAdjForms(parseFlatMap(w.getAdjForms()));

        WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getWordId, id));
        if (p != null) {
            dto.setExtractCount(p.getExtractCount());
            dto.setLastExtractedAt(p.getLastExtractedAt());
            dto.setProgressStatus(p.getStatus());
            dto.setCompletedAt(p.getCompletedAt());
        } else {
            dto.setExtractCount(0);
            dto.setProgressStatus(WordProgress.STATUS_NEVER);
        }
        return dto;
    }

    /** 解析四时态嵌套变位 JSON：{时态 → {人称 → 变位}} */
    private Map<String, Map<String, String>> parseConjugation(String json) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            root.fields().forEachRemaining(tense -> {
                Map<String, String> persons = new LinkedHashMap<>();
                tense.getValue().fields().forEachRemaining(p -> persons.put(p.getKey(), p.getValue().asText()));
                result.put(tense.getKey(), persons);
            });
        } catch (Exception e) {
            // JSON 结构异常时返回空表，交由编辑功能重建
        }
        return result;
    }

    /** 解析扁平键值 JSON（形容词变化等） */
    private Map<String, String> parseFlatMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructMapType(LinkedHashMap.class, String.class, String.class));
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /** 编辑单词详情（释义/词性/分类/性别/定冠词/变位），仅更新传入字段 */
    @Transactional
    public WordDetailDTO updateWord(Long id, WordUpdateDTO body) {
        Word w = wordMapper.selectById(id);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在：id=" + id);
        }
        if (body.getPos() != null) {
            w.setPos(body.getPos());
        }
        if (body.getMeaning() != null) {
            w.setMeaning(body.getMeaning());
        }
        if (body.getCategory() != null) {
            w.setCategory(body.getCategory());
        }
        if (body.getGender() != null) {
            w.setGender(body.getGender().isBlank() ? null : body.getGender());
        }
        if (body.getArticle() != null) {
            w.setArticle(body.getArticle().isBlank() ? null : body.getArticle());
        }
        if (body.getPlural() != null) {
            w.setPlural(body.getPlural().isBlank() ? null : body.getPlural());
        }
        if (body.getConjugation() != null) {
            try {
                w.setConjugation(objectMapper.writeValueAsString(body.getConjugation()));
            } catch (Exception e) {
                throw new IllegalArgumentException("变位数据保存失败");
            }
        }
        if (body.getAdjForms() != null) {
            try {
                w.setAdjForms(objectMapper.writeValueAsString(body.getAdjForms()));
            } catch (Exception e) {
                throw new IllegalArgumentException("形容词变化数据保存失败");
            }
        }
        wordMapper.updateById(w);
        return getDetail(id);
    }

    /** 标记完成：抽取次数 +1、进度置已完成；今日抽取记录置已完成 */
    @Transactional
    public WordDetailDTO complete(Long id) {
        WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getWordId, id));
        if (p == null) {
            p = new WordProgress();
            p.setWordId(id);
            p.setExtractCount(1);
            p.setStatus(WordProgress.STATUS_COMPLETED);
            p.setLastExtractedAt(LocalDate.now());
            p.setCompletedAt(LocalDateTime.now());
            progressMapper.insert(p);
        } else {
            p.setExtractCount(p.getExtractCount() == null ? 1 : p.getExtractCount() + 1);
            p.setStatus(WordProgress.STATUS_COMPLETED);
            p.setCompletedAt(LocalDateTime.now());
            progressMapper.updateById(p);
        }
        updateTodayRecord(id, 1);
        return getDetail(id);
    }

    /** 撤销完成：抽取次数 -1（下限 0）、状态回退；今日抽取记录回退为未完成 */
    @Transactional
    public WordDetailDTO undo(Long id) {
        WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getWordId, id));
        if (p == null || p.getExtractCount() == null || p.getExtractCount() <= 0) {
            throw new IllegalArgumentException("该单词没有可撤销的完成记录");
        }
        p.setExtractCount(p.getExtractCount() - 1);
        if (p.getExtractCount() == 0) {
            // 一次都没完成过 → 回退为"已抽取未完成"
            p.setStatus(WordProgress.STATUS_EXTRACTED);
            p.setCompletedAt(null);
        }
        progressMapper.updateById(p);
        updateTodayRecord(id, 0);
        return getDetail(id);
    }

    /** 同步更新今日抽取记录的完成状态（若存在） */
    private void updateTodayRecord(Long wordId, int status) {
        DailyExtract de = dailyExtractMapper.selectOne(new LambdaQueryWrapper<DailyExtract>()
                .eq(DailyExtract::getExtractDate, LocalDate.now())
                .eq(DailyExtract::getWordId, wordId));
        if (de != null) {
            de.setStatus(status);
            de.setCompletedAt(status == 1 ? LocalDateTime.now() : null);
            dailyExtractMapper.updateById(de);
        }
    }
}
