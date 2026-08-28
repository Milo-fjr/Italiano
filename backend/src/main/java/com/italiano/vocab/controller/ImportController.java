package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 词库导入 / 导出（备份） */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    /** 从词库 JSON 导入（幂等，已存在的词跳过） */
    @PostMapping("/import")
    public ApiResponse<Map<String, Integer>> importWords() {
        int[] r = importService.importFromJson();
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("inserted", r[0]);
        data.put("skipped", r[1]);
        return ApiResponse.ok(data);
    }

    /** 导出词库到 vocab_data.json（数据库 -> 种子文件，含手动编辑内容，git 可追踪） */
    @PostMapping("/export")
    public ApiResponse<Map<String, Object>> exportWords() {
        return ApiResponse.ok(importService.exportToJson());
    }
}
