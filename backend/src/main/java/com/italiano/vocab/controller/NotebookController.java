package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.service.NotebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 错题本（双本制）：测验/拼写/听写答错进词本，加练变位题型答错进变位本，背熟后手动移出。
 * book=main（默认，词本）/ conj（变位本），与三套复习体系零耦合。
 */
@RestController
@RequestMapping("/api/notebook")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;

    /** 本内词列表（进本先后稳定排序） */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "main") String book) {
        return ApiResponse.ok(notebookService.getWords(book));
    }

    /** 学会了：移出本 */
    @PostMapping("/{id}/learn")
    public ApiResponse<Void> learn(@PathVariable Long id,
                                   @RequestParam(defaultValue = "main") String book) {
        notebookService.learn(book, id);
        return ApiResponse.ok(null);
    }

    /** 撤销学会：放回本（防手滑） */
    @PostMapping("/{id}/undo")
    public ApiResponse<Void> undo(@PathVariable Long id,
                                  @RequestParam(defaultValue = "main") String book) {
        notebookService.undo(book, id);
        return ApiResponse.ok(null);
    }

    /** 全部学会：清空本 */
    @PostMapping("/learn-all")
    public ApiResponse<Integer> learnAll(@RequestParam(defaultValue = "main") String book) {
        return ApiResponse.ok(notebookService.learnAll(book));
    }
}
