package com.italiano.vocab.dto;

import lombok.Data;

import java.time.LocalDate;

/** 单词库列表项 */
@Data
public class WordListItemDTO {

    private Long id;
    private String word;
    private String pos;
    private String meaning;
    private String category;

    /** 累计完成次数（标记完成后 +1） */
    private Integer extractCount;

    /** 学习状态：0=未学 1=已抽取未完成 2=已完成 */
    private Integer progressStatus;

    private LocalDate lastExtractedAt;
}
