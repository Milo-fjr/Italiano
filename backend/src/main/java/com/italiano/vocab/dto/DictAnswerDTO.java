package com.italiano.vocab.dto;

import lombok.Data;

/** 听写模式答题请求：单词 + 附加形式 + 中文释义（释义是听写考察的产出之一） */
@Data
public class DictAnswerDTO {

    /** 用户输入的意大利语单词 */
    private String word;

    /** 用户输入的附加形式（现在时 io 形式 / 名词复数；无附加题为 null） */
    private String extra;

    /** 用户输入的中文释义 */
    private String meaning;
}
