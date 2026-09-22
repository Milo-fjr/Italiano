package com.italiano.vocab.dto;

import lombok.Data;

import java.util.List;

/**
 * 不规则加练列表项：points 为该词全部考点（含纯听辨点），word/meaning 保留供结果对照。
 * 听力改造（2026-09-22）：题面藏词——答题阶段前端只播形式、不渲染 word/meaning，听音辨词。
 * 整词入队逐点作答（用户确认：单形式随机怕覆盖不全，一个词的不规则点一次练全）。
 */
@Data
public class IrregularWordDTO {

    private Long wordId;

    /** 单词原文（答题阶段不渲染，判分结果返回对照） */
    private String word;

    private String pos;

    private String meaning;

    private String category;

    /** 语法形式不规则标记（红色标签；null=常规不显示） */
    private String irregular;

    /** 听力第一关释义 4 选 1 选项（同词性优先取干扰项；正确项 + 3 个随机干扰，整体打乱） */
    private List<String> meaningOptions;

    /** 该词全部考点（不含判分答案） */
    private List<IrregularPointDTO> points;
}
