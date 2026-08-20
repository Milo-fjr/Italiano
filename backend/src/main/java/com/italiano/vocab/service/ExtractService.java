package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.italiano.vocab.dto.TodayWordDTO;
import com.italiano.vocab.entity.DailyExtract;
import com.italiano.vocab.entity.Setting;
import com.italiano.vocab.entity.Word;
import com.italiano.vocab.entity.WordProgress;
import com.italiano.vocab.mapper.DailyExtractMapper;
import com.italiano.vocab.mapper.WordMapper;
import com.italiano.vocab.mapper.WordProgressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学习批次（核心业务）：
 * 1. 批次只在用户手动「换一批」时生成，不按日期自动轮换——没背完的批次一直保留，学完为止
 * 2. 换一批时：上一批未完成的单词保留进入新批次，剩余名额按算法补新词
 * 3. 新词第一优先抽「从未抽取过」的词（随机抽取）；第二优先「累计完成次数少」的词
 *    （extract_count 升序 → 最近抽取时间升序 → id 升序）
 * 4. 冷却期内（最近 cooldown_days 天抽取过）的词不参与新词候选，候选不足时放宽
 * <p>
 * 词库总量仅千级，进度表全量加载后内存筛选排序即可，无需复杂 SQL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final DailyExtractMapper dailyExtractMapper;
    private final SettingService settingService;

    /** 获取当前批次（不自动抽取：从未刷新过则返回空批次，由用户手动抽第一批） */
    public Map<String, Object> getCurrent() {
        List<DailyExtract> records = dailyExtractMapper.selectList(
                new LambdaQueryWrapper<DailyExtract>().orderByAsc(DailyExtract::getId));
        return assemble(records);
    }

    /** 手动换一批：上一批未完成的词保留进新批次（学完为止），其余名额重新抽取 */
    @Transactional
    public Map<String, Object> refresh() {
        LocalDate today = LocalDate.now();
        List<DailyExtract> current = dailyExtractMapper.selectList(null);
        List<Long> carried = current.stream()
                .filter(de -> de.getStatus() == null || de.getStatus() == 0)
                .map(DailyExtract::getWordId)
                .toList();
        if (!current.isEmpty()) {
            dailyExtractMapper.delete(new LambdaQueryWrapper<>());
        }
        List<DailyExtract> records = extract(today, carried);
        log.info("换一批完成：保留未完成 {} 个，本批共 {} 个词（每日 {} / 冷却 {} 天）",
                carried.size(), records.size(), settingService.getSetting().getDailyCount(),
                settingService.getSetting().getCooldownDays());
        return assemble(records);
    }

    /**
     * 执行抽取：carried 为保留的未完成词（优先占位），再按算法补足名额。
     * 写入 daily_extract(status=0)，同时为无进度的词创建 progress 记录
     * （status=1 已抽取未完成），并刷新 last_extracted_at。
     */
    private List<DailyExtract> extract(LocalDate today, List<Long> carried) {
        Setting setting = settingService.getSetting();
        int need = setting.getDailyCount();
        int cooldownDays = setting.getCooldownDays();

        List<WordProgress> allProgress = progressMapper.selectList(null);
        Set<Long> progressedIds = allProgress.stream()
                .map(WordProgress::getWordId).collect(Collectors.toSet());
        Map<Long, WordProgress> progressById = allProgress.stream()
                .collect(Collectors.toMap(WordProgress::getWordId, Function.identity(), (a, b) -> a));
        Set<Long> picked = new LinkedHashSet<>(carried);

        // ① 新词：从未抽取过的词（无 progress 记录），随机抽取
        if (picked.size() < need) {
            LambdaQueryWrapper<Word> qw = new LambdaQueryWrapper<Word>()
                    .last("ORDER BY RAND() LIMIT " + (need - picked.size()));
            if (!progressedIds.isEmpty()) {
                qw.notIn(Word::getId, progressedIds);
            }
            if (!picked.isEmpty()) {
                qw.notIn(Word::getId, picked);
            }
            wordMapper.selectList(qw).forEach(w -> picked.add(w.getId()));
        }

        // ② 第二优先：已有进度且不在冷却期的词（last_extracted_at <= 今天-冷却天数）
        //    冷却期自动排除了本批已保留/刚抽过的词，避免连续重复
        LocalDate cooldownBoundary = today.minusDays(cooldownDays);
        if (picked.size() < need) {
            allProgress.stream()
                    .filter(p -> !picked.contains(p.getWordId()))
                    .filter(p -> p.getLastExtractedAt() == null
                            || !p.getLastExtractedAt().isAfter(cooldownBoundary))
                    .sorted(progressComparator())
                    .limit((long) need - picked.size())
                    .forEach(p -> picked.add(p.getWordId()));
        }

        // ③ 补足：候选不足时放宽冷却限制，仍按完成次数最少优先
        if (picked.size() < need) {
            allProgress.stream()
                    .filter(p -> !picked.contains(p.getWordId()))
                    .sorted(progressComparator())
                    .limit((long) need - picked.size())
                    .forEach(p -> picked.add(p.getWordId()));
        }

        // ④ 写入批次记录 + 创建/刷新进度
        List<DailyExtract> records = new ArrayList<>();
        for (Long wordId : picked) {
            DailyExtract de = new DailyExtract();
            de.setExtractDate(today);
            de.setWordId(wordId);
            de.setStatus(0);
            dailyExtractMapper.insert(de);
            records.add(de);

            WordProgress p = progressById.get(wordId);
            if (p == null) {
                p = new WordProgress();
                p.setWordId(wordId);
                p.setExtractCount(0);
                p.setStatus(WordProgress.STATUS_EXTRACTED); // 已抽取未完成
                p.setLastExtractedAt(today);
                progressMapper.insert(p);
            } else {
                p.setLastExtractedAt(today);
                progressMapper.updateById(p);
            }
        }
        return records;
    }

    /** 进度排序：完成次数升序 → 最近抽取时间升序（最久未抽优先）→ id 升序 */
    private Comparator<WordProgress> progressComparator() {
        return Comparator.comparingInt(WordProgress::getExtractCount)
                .thenComparing(WordProgress::getLastExtractedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(WordProgress::getWordId);
    }

    /** 组装返回：批次日期 + 批次单词列表（含进度信息）；空批次日期为今天 */
    private Map<String, Object> assemble(List<DailyExtract> records) {
        List<Long> wordIds = records.stream().map(DailyExtract::getWordId).toList();
        Map<Long, Word> words = wordIds.isEmpty() ? Map.of()
                : wordMapper.selectBatchIds(wordIds).stream()
                    .collect(Collectors.toMap(Word::getId, Function.identity()));
        Map<Long, WordProgress> progress = wordIds.isEmpty() ? Map.of()
                : progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                            .in(WordProgress::getWordId, wordIds))
                    .stream().collect(Collectors.toMap(WordProgress::getWordId, Function.identity()));

        List<TodayWordDTO> list = new ArrayList<>();
        for (DailyExtract de : records) {
            Word w = words.get(de.getWordId());
            if (w == null) {
                continue;
            }
            TodayWordDTO dto = new TodayWordDTO();
            dto.setWordId(w.getId());
            dto.setWord(w.getWord());
            dto.setPos(w.getPos());
            dto.setMeaning(w.getMeaning());
            dto.setCategory(w.getCategory());
            dto.setDailyStatus(de.getStatus());
            WordProgress p = progress.get(de.getWordId());
            if (p != null) {
                dto.setExtractCount(p.getExtractCount());
                dto.setLastExtractedAt(p.getLastExtractedAt());
                dto.setProgressStatus(p.getStatus());
            } else {
                dto.setExtractCount(0);
                dto.setProgressStatus(WordProgress.STATUS_NEVER);
            }
            list.add(dto);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        LocalDate date = records.isEmpty() ? LocalDate.now() : records.get(0).getExtractDate();
        result.put("date", date.toString());
        result.put("total", list.size());
        result.put("completed", list.stream().filter(d -> d.getDailyStatus() != null && d.getDailyStatus() == 1).count());
        result.put("words", list);
        return result;
    }
}
