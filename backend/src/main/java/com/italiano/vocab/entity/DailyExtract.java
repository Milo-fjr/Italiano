package com.italiano.vocab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 每日抽取记录表 */
@Data
@TableName("daily_extract")
public class DailyExtract {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 抽取日期 */
    private LocalDate extractDate;

    /** 单词 ID */
    private Long wordId;

    /** 0=未完成 1=已完成 */
    private Integer status;

    /** 完成时间 */
    private LocalDateTime completedAt;
}
