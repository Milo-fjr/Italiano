package com.italiano.vocab.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/** 单词详情（含定冠词单/复数、变位表、学习进度） */
@Data
public class WordDetailDTO {

    private Long id;
    private String word;
    private String pos;
    private String meaning;
    private String category;

    /** 名词性别 m/f */
    private String gender;

    /** 定冠词（单数；复数名词则为复数冠词） */
    private String article;

    /** 复数定冠词（由单数冠词推导，展示用） */
    private String articlePlural;

    /** 不定冠词（un/uno/una/un'，由规则推导，展示用） */
    private String articleIndefinite;

    /** 名词复数形式 */
    private String plural;

    /** 不规则标记（与卡片红标同源：动词为顿号拼接的时态清单，如"近过去时不规则、将来时不规则"） */
    private String irregular;

    /** 动词变位：{present, passatoProssimo, imperfetto, futuro} → {io, tu, lui/lei, noi, voi, loro} */
    private Map<String, Map<String, String>> conjugation;

    /** 形容词性数变化：{ms 阳单, fs 阴单, mp 阳复, fp 阴复} */
    private Map<String, String> adjForms;

    /** 例句：{it 意语例句, zh 中文翻译}（可空） */
    private Map<String, String> example;

    private Integer extractCount;
    private LocalDate lastExtractedAt;
    private Integer progressStatus;
    private LocalDateTime completedAt;

    /** SRS 盒子级别 0-5 */
    private Integer box;

    /** 下次复习日期 */
    private LocalDate nextReviewAt;
}
