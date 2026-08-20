package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.service.ExtractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 学习批次（原"今日单词"，改为手动刷新，不再按日期自动轮换） */
@RestController
@RequestMapping("/api/today")
@RequiredArgsConstructor
public class TodayController {

    private final ExtractService extractService;

    /** 获取当前批次（不自动抽取；从未刷新过则返回空批次） */
    @GetMapping
    public ApiResponse<Map<String, Object>> today() {
        return ApiResponse.ok(extractService.getCurrent());
    }

    /** 换一批：上一批未完成的词保留进新批次，其余名额重新抽取 */
    @PostMapping("/extract")
    public ApiResponse<Map<String, Object>> extract() {
        return ApiResponse.ok(extractService.refresh());
    }
}
