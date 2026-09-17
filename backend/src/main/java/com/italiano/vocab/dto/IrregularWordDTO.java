package com.italiano.vocab.dto;

import lombok.Data;

import java.util.List;

/**
 * 不规则加练列表项：单词本身即题面（要变位/变形必须先给出原词），points 为该词全部考点。
 * 整词入队逐点作答（用户确认：单形式随机怕覆盖不全，一个词的不规则点一次练全）。
 */
@Data
public class IrregularWordDTO {

    private Long wordId;

    /** 单词原文（题面：考点围绕它展开） */
    private String word;

    private String pos;

    private String meaning;

    private String category;

    /** 语法形式不规则标记（红色标签；null=常规不显示） */
    private String irregular;

    /** 该词全部不规则考点（不含答案） */
    private List<IrregularPointDTO> points;
}
