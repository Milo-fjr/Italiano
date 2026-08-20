package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.italiano.vocab.dto.StatsDTO;
import com.italiano.vocab.entity.DailyExtract;
import com.italiano.vocab.entity.Word;
import com.italiano.vocab.entity.WordProgress;
import com.italiano.vocab.mapper.DailyExtractMapper;
import com.italiano.vocab.mapper.WordMapper;
import com.italiano.vocab.mapper.WordProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 学习统计服务 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final DailyExtractMapper dailyExtractMapper;

    public StatsDTO getStats() {
        List<Word> words = wordMapper.selectList(null);
        List<WordProgress> progresses = progressMapper.selectList(null);
        Map<Long, WordProgress> progressMap = progresses.stream()
                .collect(Collectors.toMap(WordProgress::getWordId, p -> p, (a, b) -> a));

        StatsDTO dto = new StatsDTO();
        dto.setTotalWords(words.size());

        // 覆盖 = 至少完成过一次（extract_count > 0）
        long covered = progresses.stream()
                .filter(p -> p.getExtractCount() != null && p.getExtractCount() > 0).count();
        dto.setCoveredWords(covered);
        dto.setCoverageRate(words.isEmpty() ? 0 : Math.round(covered * 1000.0 / words.size()) / 10.0);
        dto.setTotalExtractCount(progresses.stream()
                .mapToLong(p -> p.getExtractCount() == null ? 0 : p.getExtractCount()).sum());

        // 今日进度
        List<DailyExtract> today = dailyExtractMapper.selectList(new LambdaQueryWrapper<DailyExtract>()
                .eq(DailyExtract::getExtractDate, LocalDate.now()));
        dto.setTodayTotal(today.size());
        dto.setTodayCompleted((int) today.stream().filter(t -> t.getStatus() != null && t.getStatus() == 1).count());

        // 分类分布：词数与已至少完成一次的词数
        Map<String, List<Word>> byCategory = words.stream()
                .collect(Collectors.groupingBy(w -> w.getCategory() == null ? "未分类" : w.getCategory(),
                        LinkedHashMap::new, Collectors.toList()));
        List<StatsDTO.CategoryStat> categoryStats = byCategory.entrySet().stream().map(e -> {
            StatsDTO.CategoryStat cs = new StatsDTO.CategoryStat();
            cs.setCategory(e.getKey());
            cs.setTotal(e.getValue().size());
            cs.setCompleted(e.getValue().stream().filter(w -> {
                WordProgress p = progressMap.get(w.getId());
                return p != null && p.getExtractCount() != null && p.getExtractCount() > 0;
            }).count());
            return cs;
        }).collect(Collectors.toList());
        dto.setCategoryStats(categoryStats);

        // 抽取次数分布：0 次 / 1 次 / 2-3 次 / 4 次及以上
        List<StatsDTO.CountBucket> dist = new ArrayList<>();
        dist.add(bucket("0 次", words.size() - covered));
        dist.add(bucket("1 次", progresses.stream().filter(p -> p.getExtractCount() != null && p.getExtractCount() == 1).count()));
        dist.add(bucket("2-3 次", progresses.stream().filter(p -> p.getExtractCount() != null && p.getExtractCount() >= 2 && p.getExtractCount() <= 3).count()));
        dist.add(bucket("4 次及以上", progresses.stream().filter(p -> p.getExtractCount() != null && p.getExtractCount() >= 4).count()));
        dto.setExtractCountDistribution(dist);
        return dto;
    }

    private StatsDTO.CountBucket bucket(String label, long count) {
        StatsDTO.CountBucket b = new StatsDTO.CountBucket();
        b.setLabel(label);
        b.setCount(count);
        return b;
    }
}
