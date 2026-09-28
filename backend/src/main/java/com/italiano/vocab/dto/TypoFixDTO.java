package com.italiano.vocab.dto;

import lombok.Data;

/** 误触改判请求：判错响应里返回的「答错前快照」原样回传（盒子等级 + 错题本状态；加练无 SRS 只用后者） */
@Data
public class TypoFixDTO {

    /** 答错前的盒子等级（拼写/听写；加练为 null） */
    private Integer boxBefore;

    /** 答错前是否已在错题本（true=原本就在本里，改判不动它） */
    private Boolean notebookBefore;
}
