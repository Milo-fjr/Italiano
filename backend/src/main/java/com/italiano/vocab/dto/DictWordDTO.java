package com.italiano.vocab.dto;

import lombok.Data;

/**
 * 听写模式列表项：含单词原文（前端 TTS 朗读用，页面不显示——听写靠听不靠看）。
 * 不含中文释义（释义是听写要考察的产出之一，队列不下发答案）。
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

    /** 附加填写类型：null=无附加题 / "presentIo"=现在时 io 形式 / "plural"=名词复数 */
    private String extraType;

    /** 附加填写输入框标签（与 extraType 同空同有） */
    private String extraLabel;
}
