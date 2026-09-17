package com.italiano.vocab.dto;

import lombok.Data;

/** 拼写模式列表项（不含任何答案字段——单词与附加形式由答题接口判分后返回） */
@Data
public class SpellWordDTO {

    private Long wordId;

    private String meaning;

    private String pos;

    private String category;

    /** 语法形式不规则标记（红色标签；null=常规不显示） */
    private String irregular;
}
