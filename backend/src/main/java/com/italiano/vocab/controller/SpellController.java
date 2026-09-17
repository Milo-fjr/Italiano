package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.dto.SpellAnswerDTO;
import com.italiano.vocab.service.SpellService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 拼写模式：中→意产出复习（独立拼写盒子，防撞规则见 SpellService） */
@RestController
@RequestMapping("/api/spell")
@RequiredArgsConstructor
public class SpellController {

    private final SpellService spellService;

    /** 到期拼写队列（随机顺序，不含答案）+ 最近未来到期日 */
    @GetMapping
    public ApiResponse<Map<String, Object>> due() {
        return ApiResponse.ok(spellService.getDueWords());
    }

    /** 答题判分 + 拼写 SRS 推进，返回正确答案供结果页对照 */
    @PostMapping("/{id}/answer")
    public ApiResponse<Map<String, Object>> answer(@PathVariable Long id, @RequestBody SpellAnswerDTO body) {
        return ApiResponse.ok(spellService.answer(id, body.getWord()));
    }
}
