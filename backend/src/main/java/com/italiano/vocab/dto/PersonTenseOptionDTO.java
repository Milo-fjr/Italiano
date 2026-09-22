package com.italiano.vocab.dto;

import lombok.Data;

/** 「选人称时态」选项：label 供题面展示，tense/person 供判分比对（选项组不含正确项标识） */
@Data
public class PersonTenseOptionDTO {

    /** 题面标签（如「现在时 · noi」） */
    private String label;

    /** 时态：present/futuro */
    private String tense;

    /** 人称：io/tu/lui/lei/noi/voi/loro */
    private String person;
}
