package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 词库导入 */
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
}
