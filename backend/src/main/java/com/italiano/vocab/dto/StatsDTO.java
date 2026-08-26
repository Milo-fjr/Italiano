package com.italiano.vocab.dto;

import lombok.Data;

import java.util.List;

/** 学习统计 */
@Data
public class StatsDTO {

    /** 总词数 */
    private long totalWords;

    /** 已覆盖词数（至少完成一次，extract_count > 0） */
    private long coveredWords;

    /** 覆盖率（百分比，保留一位小数） */
    private double coverageRate;

    /** 今日抽取词数 */
    private int todayTotal;

    /** 今日已完成词数 */
    private int todayCompleted;

    /** 累计完成总次数 */
    private long totalExtractCount;

    /** 今日到期复习词数（SRS：box > 0 且 next_review_at <= 今天） */
    private long dueReviewCount;

    /** SRS 盒子分布（Box 0-5） */
    private List<CountBucket> boxDistribution;

    /** 分类分布 */
    private List<CategoryStat> categoryStats;

    /** 抽取次数分布 */
    private List<CountBucket> extractCountDistribution;

    @Data
    public static class CategoryStat {
        private String category;
        /** 该分类词数 */
        private long total;
        /** 该分类已至少完成一次的词数 */
        private long completed;
    }

    @Data
    public static class CountBucket {
        private String label;
        private long count;
    }
}
