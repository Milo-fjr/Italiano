package com.italiano.vocab.dto;

import lombok.Data;

import java.util.Map;

/** 编辑单词请求体（均为可选字段，仅更新传入的字段） */
@Data
public class WordUpdateDTO {

    private String pos;
    private String meaning;
    private String category;
    /** m / f / 空字符串（清空） */
    private String gender;
    private String article;
    /** 名词复数形式（空字符串清空） */
    private String plural;
    /** 四时态变位 {present, passatoProssimo, imperfetto, futuro} → 六人称 */
    private Map<String, Map<String, String>> conjugation;
    /** 形容词性数变化 {ms, fs, mp, fp} */
    private Map<String, String> adjForms;
}
