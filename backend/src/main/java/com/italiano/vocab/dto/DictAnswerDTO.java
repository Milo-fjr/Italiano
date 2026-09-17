package com.italiano.vocab.dto;

import lombok.Data;

/** 听写模式答题请求：单词 + 中文释义（释义是听写考察的产出之一） */
@Data
public class DictAnswerDTO {

    /** 用户输入的意大利语单词 */
    private String word;

    /** 用户输入的中文释义 */
    private String meaning;
}
