package com.italiano.vocab.dto;

import lombok.Data;

import java.util.List;

/**
 * 名词专考题目（一词一题三关：单数定冠词 4选1 → 复数拼写 → 复数定冠词 4选1）。
 * 题面即单词本身（无泄题问题）；选项 DTO 只含选项、不含正确项标识；
 * 复数拼写答案不随题目下发（判分时服务端现场推导）。
 */
@Data
public class NounQuestionDTO {

    private Long wordId;

    private String word;

    private String pos;

    private String meaning;

    private String category;

    /** 语法形式不规则标记（红色标签；null=常规不显示） */
    private String irregular;

    /** 关1是否有：单数定冠词 4 选 1（性别可由词尾直推的词无此关——考了没意义） */
    private Boolean hasArticleGate;

    /** 关1：单数定冠词 4 选 1 选项（正确项 + 3 个干扰项，整体打乱） */
    private List<String> articleOptions;

    /** 是否有关2：复数拼写关（复数=原词的不变形词、不可数词无此关） */
    private Boolean hasPluralSpell;

    /** 是否有关3：复数定冠词关（复数真值推不出时无此关） */
    private Boolean hasPluralArticle;

    /** 关3：复数定冠词 4 选 1 选项（i/gli/le/l' 四形态，含 l' 作干扰项） */
    private List<String> pluralArticleOptions;
}
