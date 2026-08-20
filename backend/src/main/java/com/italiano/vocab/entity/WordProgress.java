package com.italiano.vocab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 学习进度表（每词一行） */
@Data
@TableName("word_progress")
public class WordProgress {

    public static final int STATUS_NEVER = 0;      // 从未抽取（一般无记录行）
    public static final int STATUS_EXTRACTED = 1;  // 已抽取未完成
    public static final int STATUS_COMPLETED = 2;  // 已完成

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 单词 ID（唯一） */
    private Long wordId;

    /** 累计抽取次数（标记完成后 +1） */
    private Integer extractCount;

    /** 最近一次被抽取的日期 */
    private LocalDate lastExtractedAt;

    /** 0=从未抽取 1=已抽取未完成 2=已完成 */
    private Integer status;

    /** 最近完成时间 */
    private LocalDateTime completedAt;
}
