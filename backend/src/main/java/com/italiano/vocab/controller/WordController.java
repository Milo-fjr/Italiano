package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.dto.WordDetailDTO;
import com.italiano.vocab.dto.WordUpdateDTO;
import com.italiano.vocab.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 单词库 */
@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    /** 单词列表：分页 + 按分类/状态筛选 + 关键词搜索 */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(wordService.listWords(page, size, category, status, keyword));
    }

    /** 单词详情（含变位、定冠词、进度） */
    @GetMapping("/{id}")
    public ApiResponse<WordDetailDTO> detail(@PathVariable Long id) {
        return ApiResponse.ok(wordService.getDetail(id));
    }

    /** 编辑单词详情（补充/修改变位、定冠词、性别等） */
    @PutMapping("/{id}")
    public ApiResponse<WordDetailDTO> update(@PathVariable Long id, @RequestBody WordUpdateDTO body) {
        return ApiResponse.ok(wordService.updateWord(id, body));
    }

    /** 标记单词完成（抽取次数 +1） */
    @PostMapping("/{id}/complete")
    public ApiResponse<WordDetailDTO> complete(@PathVariable Long id) {
        return ApiResponse.ok(wordService.complete(id));
    }

    /** 撤销完成（抽取次数 -1，状态回退） */
    @PostMapping("/{id}/undo")
    public ApiResponse<WordDetailDTO> undo(@PathVariable Long id) {
        return ApiResponse.ok(wordService.undo(id));
    }
}
