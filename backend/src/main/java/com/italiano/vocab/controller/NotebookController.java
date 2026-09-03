package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.service.NotebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 错题本：测验/拼写答错的词自动进本，背熟后手动移出（与三套复习体系零耦合） */
@RestController
@RequestMapping("/api/notebook")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;

    /** 本内词列表（随机顺序） */
    @GetMapping
    public ApiResponse<Map<String, Object>> list() {
        return ApiResponse.ok(notebookService.getWords());
    }

    /** 学会了：移出错题本 */
    @PostMapping("/{id}/learn")
    public ApiResponse<Void> learn(@PathVariable Long id) {
        notebookService.learn(id);
        return ApiResponse.ok(null);
    }

    /** 撤销学会：放回错题本（防手滑） */
    @PostMapping("/{id}/undo")
    public ApiResponse<Void> undo(@PathVariable Long id) {
        notebookService.undo(id);
        return ApiResponse.ok(null);
    }

    /** 全部学会：清空错题本 */
    @PostMapping("/learn-all")
    public ApiResponse<Integer> learnAll() {
        return ApiResponse.ok(notebookService.learnAll());
    }
}
