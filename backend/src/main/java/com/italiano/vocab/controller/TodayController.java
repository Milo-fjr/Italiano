package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.service.ExtractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 今日单词 */
@RestController
@RequestMapping("/api/today")
@RequiredArgsConstructor
public class TodayController {

    private final ExtractService extractService;

    /** 获取今日抽取的单词列表（当天无记录时自动抽取） */
    @GetMapping
    public ApiResponse<Map<String, Object>> today() {
        return ApiResponse.ok(extractService.getToday());
    }

    /** 手动触发今日抽取（幂等：当天已有记录则返回现有列表） */
    @PostMapping("/extract")
    public ApiResponse<Map<String, Object>> extract() {
        return ApiResponse.ok(extractService.manualExtract());
    }
}
