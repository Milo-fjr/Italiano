package com.italiano.vocab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 单词表 */
@Data
@TableName("word")
public class Word {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 单词（唯一） */
    private String word;

    /** 词性，如 s.m. / s.f. / v. / agg. */
    private String pos;

    /** 中文释义 */
    private String meaning;

    /** 主题分类 */
    private String category;

    /** 名词性别 m/f（可空） */
    private String gender;

    /** 定冠词：il/lo/la/l'/i/gli/le（可空） */
    private String article;

    /** 动词变位 JSON（可空） */
    private String conjugation;

    /** 形容词性数变化 JSON：{ms,fs,mp,fp}（可空） */
    private String adjForms;

    /** 名词复数形式（可空） */
    private String plural;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
