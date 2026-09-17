package com.italiano.vocab.dto;

import lombok.Data;

/**
 * 不规则加练考点（不含答案——判分接口现场推导返回，题面不泄题）。
 * type: present=现在时人称 / pp=过去分词 / futuro=简单将来时人称 / plural=名词复数 /
 *       bello=冠词式定语形式（语境名词见 context 字段）/ adjMp=形容词阳性复数 / adjInv=不变形容词复数
 */
@Data
public class IrregularPointDTO {

    private String type;

    /** 人称（仅 present/futuro：io/tu/lui/lei/noi/voi/loro） */
    private String person;

    /** 题面标签（如「现在时 · lui/lei」「过去分词」「定语形式」） */
    private String label;

    /** bello 型语境名词原文（如 libro/libri/amica） */
    private String contextNoun;

    /** 语境名词中文释义（题面提示用） */
    private String contextMeaning;

    /** 语境名词性别（m/f） */
    private String contextGender;

    /** 语境名词是否复数 */
    private Boolean contextPlural;
}
