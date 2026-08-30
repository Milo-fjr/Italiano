package com.italiano.vocab.dto;

import lombok.Data;

/** 拼写答题请求体：word=拼写的单词，extra=附加形式（现在时 io / 复数，无附加题为 null） */
@Data
public class SpellAnswerDTO {

    private String word;

    private String extra;
}
