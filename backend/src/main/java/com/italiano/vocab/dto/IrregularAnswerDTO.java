package com.italiano.vocab.dto;

import lombok.Data;

/** 不规则加练答题请求：考点描述（与题目接口 points 项字段一致回传）+ 各关作答 */
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

    /** 所选中文释义（听力第一关；null 判错） */
    private String meaning;

    /** 与题目 points 项一致回传：是否先出「选人称时态」关 */
    private Boolean personChoice;

    /** 与题目 points 项一致回传：是否纯听辨点（选对人称时态即过，不拼写） */
    private Boolean listenOnly;

    /** 所选时态（personChoice/listenOnly 点回传：present/futuro） */
    private String chosenTense;

    /** 所选人称（personChoice/listenOnly 点回传） */
    private String chosenPerson;

    /** 拼写输入（listenOnly 点不传） */
    private String input;
}
