package com.italiano.vocab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 设置表（单行，id=1） */
@Data
@TableName("setting")
public class Setting {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 每日抽取数量，默认 35 */
    private Integer dailyCount;

    /** 冷却天数，默认 7 */
    private Integer cooldownDays;

    /** 测验模式每日抽题上限（0=不限制），默认 80 */
    private Integer quizDailyLimit;

    /** 拼写模式每日抽题上限（0=不限制），默认 80 */
    private Integer spellDailyLimit;

    /** 听写模式每日抽题上限（0=不限制），默认 80 */
    private Integer dictDailyLimit;
}
