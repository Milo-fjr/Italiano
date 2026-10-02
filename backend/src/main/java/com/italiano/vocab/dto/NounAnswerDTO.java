package com.italiano.vocab.dto;

import lombok.Data;

/** 名词专考答题请求：各关作答回传（服务端现场推导真值比对，无状态判分；不存在的关传空） */
@Data
public class NounAnswerDTO {

    /** 关1所选单数定冠词（il/lo/la/l'/i/gli/le 之一） */
    private String article;

    /** 关2拼写输入（无此关或未到该关传空） */
    private String plural;

    /** 关3所选复数定冠词（i/gli/le/l' 之一；无此关传空） */
    private String pluralArticle;
}
