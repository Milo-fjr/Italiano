package com.italiano.vocab.dto;

import lombok.Data;

import java.util.List;

/**
 * 不规则加练考点（判分答案由服务端现场推导，题面不渲染答案）。
 * type: present=现在时人称 / pp=过去分词 / futuro=简单将来时人称 / plural=名词复数 /
 *       bello=冠词式定语形式（语境名词见 context 字段）/ adjMp=形容词阳性复数 / adjInv=不变形容词复数
 * 听力改造（2026-09-22）：题面藏词听形式——form 为播报文本（"/"多形式前端播第一个）；
 * present/futuro 考点若无同形歧义先出「选人称时态」关（personTenseOptions 4 选 1）再拼写；
 * listenOnly=纯听辨点（规则形式采样，选对人称时态即过不拼写，同形歧义组合不入选）。
 */
@Data
public class IrregularPointDTO {

    private String type;

    /** 人称（仅 present/futuro：io/tu/lui/lei/noi/voi/loro） */
    private String person;

    /** 题面标签（如「现在时 · lui/lei」「过去分词」；选人称关开启时由前端隐藏，防泄答案） */
    private String label;

    /** 播报文本（听力题面：变位形式/裸分词/复数/定语形式） */
    private String form;

    /** 是否先出「选人称时态」关（仅 present/futuro 且形式在该时态内无同形歧义） */
    private Boolean personChoice;

    /** 是否纯听辨点（选对人称时态即过，无拼写关） */
    private Boolean listenOnly;

    /** 「选人称时态」4 选 1 选项（正确项 + 3 个随机干扰项，整体打乱） */
    private List<PersonTenseOptionDTO> personTenseOptions;

    /** bello 型语境名词原文（如 libro/libri/amica） */
    private String contextNoun;

    /** 语境名词中文释义（题面提示用） */
    private String contextMeaning;

    /** 语境名词性别（m/f） */
    private String contextGender;

    /** 语境名词是否复数 */
    private Boolean contextPlural;
}
