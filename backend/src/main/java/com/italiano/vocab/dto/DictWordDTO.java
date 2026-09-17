package com.italiano.vocab.dto;

import lombok.Data;

import java.util.List;

/**
 * 听写模式列表项：含单词原文（前端 TTS 朗读用，页面不显示——听写靠听不靠看）。
 * 释义不直接下发，改为 4 选 1 选项（meaningOptions 含正确释义 + 随机干扰项，已打乱）。
 */
@Data
public class DictWordDTO {

    private Long wordId;

    /** 单词原文：仅用于前端语音播放，界面不展示 */
    private String word;

    private String pos;

    private String category;

    /** 语法形式不规则标记（红色标签；null=常规不显示） */
    private String irregular;

    /** 中文释义 4 选项（打乱顺序，含 1 个正确释义 + 3 个随机干扰项；用户点选，避免手打误判） */
    private List<String> meaningOptions;
}
