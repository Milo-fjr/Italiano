package com.italiano.vocab.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    /** SRS 盒子级别 0-5（0=未进入复习，越高越熟练） */
    private Integer box;

    /** 下次复习日期（Leitner：认识升级加长间隔，不认识归零明天再复习）；允许置空（撤销时作废） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate nextReviewAt;

    /** 拼写盒子级别 0-5（独立于认识盒子：答对升盒，答错归 0 明天再拼） */
    private Integer spellBox;

    /** 下次拼写复习日期；NULL=从未拼过（学过的词视为到期，由防撞规则节流） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate spellNextReviewAt;

    /** 最近一次认识测验答题日期（拼写防撞：当天测过认识的词不进拼写队列） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate lastQuizAt;

    /** 错题本标记：测验答错/拼写判错自动置 true，背熟后手动移出；与三套复习体系零耦合 */
    private Boolean inNotebook;

    /** 变位错题本标记：加练变位题型答错自动置 true（独立于词本，一个词可同时在两本） */
    private Boolean inConjNotebook;

    /** 听写盒子级别 0-5（独立体系：听音写词，全对升盒/有错归 0，不动认识盒和拼写盒） */
    private Integer dictBox;

    /** 下次听写复习日期；NULL=从未听写过（学过的词视为到期，由防撞规则节流） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate dictNextReviewAt;

    /** 最近一次拼写答题日期（听写防撞：当天拼过的词不进听写队列） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate lastSpellAt;

    /** 最近一次听写答题日期（拼写防撞：当天听写过的词不进拼写队列） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate lastDictAt;
}
