package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.dto.DictAnswerDTO;
import com.italiano.vocab.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 听写模式：听音→写词产出复习（独立听写盒子，防撞规则见 DictService） */
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    /** 到期听写队列（随机顺序，含单词原文供 TTS；不含释义——释义是考察产出）+ 最近未来到期日 */
    @GetMapping
    public ApiResponse<Map<String, Object>> due() {
        return ApiResponse.ok(dictService.getDueWords());
    }

    /** 两段式第一关：释义选项预检（只判对错，不推进 SRS；选对后前端进入拼写关） */
    @PostMapping("/{id}/check-meaning")
    public ApiResponse<Map<String, Object>> checkMeaning(@PathVariable Long id, @RequestBody DictAnswerDTO body) {
        return ApiResponse.ok(dictService.checkMeaning(id, body.getMeaning()));
    }

    /** 答题判分 + 听写 SRS 推进（单词 + 释义 + 附加形式全对才升盒），返回正确答案供结果页对照 */
    @PostMapping("/{id}/answer")
    public ApiResponse<Map<String, Object>> answer(@PathVariable Long id, @RequestBody DictAnswerDTO body) {
        return ApiResponse.ok(dictService.answer(id, body.getWord(), body.getExtra(), body.getMeaning()));
    }
}
