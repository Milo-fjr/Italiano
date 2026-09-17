package com.italiano.vocab.dto;

import lombok.Data;

/** 拼写答题请求体：word=拼写的单词 */
@Data
public class SpellAnswerDTO {

    private String word;
}
