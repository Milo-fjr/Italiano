package com.italiano.vocab.dto;

import lombok.Data;

/** 不规则加练答题请求：考点描述（与题目接口 points 项字段一致）+ 用户输入 */
@Data
public class IrregularAnswerDTO {

    /** 考点类型：present/pp/futuro/plural/bello/adjMp/adjInv */
    private String type;

    /** 人称（仅 present/futuro） */
    private String person;

    /** bello 型语境名词原文 */
    private String contextNoun;

    /** bello 型语境名词性别 */
    private String contextGender;

    /** bello 型语境名词是否复数 */
    private Boolean contextPlural;

    /** 用户输入 */
    private String input;
}
