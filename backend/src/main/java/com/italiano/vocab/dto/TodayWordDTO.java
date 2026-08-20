package com.italiano.vocab.dto;

import lombok.Data;

import java.time.LocalDate;

/** 今日单词列表项 */
@Data
public class TodayWordDTO {

    private Long wordId;
    private String word;
    private String pos;
    private String meaning;
    private String category;

    /** 今日完成状态：0=未完成 1=已完成（来自 daily_extract） */
    private Integer dailyStatus;

    /** 累计完成次数（标记完成后 +1） */
    private Integer extractCount;

    /** 最近抽取日期 */
    private LocalDate lastExtractedAt;

    /** 学习状态：0=从未抽取 1=已抽取未完成 2=已完成 */
    private Integer progressStatus;
}
