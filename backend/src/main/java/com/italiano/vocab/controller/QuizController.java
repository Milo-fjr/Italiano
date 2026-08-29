package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.dto.WordDetailDTO;
import com.italiano.vocab.service.QuizService;
import com.italiano.vocab.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 测验模式：SRS 到期复习（与学习批次独立，只认盒子不动完成次数） */
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final WordService wordService;

    /** 到期测验词（随机顺序）+ 最近未来到期日 */
    @GetMapping
    public ApiResponse<Map<String, Object>> due() {
        return ApiResponse.ok(quizService.getDueWords());
    }

    /** 测验「认识」：盒 +1（不动完成次数与批次状态） */
    @PostMapping("/{id}/know")
    public ApiResponse<WordDetailDTO> know(@PathVariable Long id) {
        return ApiResponse.ok(wordService.reviewKnow(id));
    }

    /** 测验「不认识」：盒归 0 明天再测（复用学习侧 forget，同样不动批次） */
    @PostMapping("/{id}/forget")
    public ApiResponse<WordDetailDTO> forget(@PathVariable Long id) {
        return ApiResponse.ok(wordService.forget(id));
    }
}
