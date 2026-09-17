package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.dto.DictAnswerDTO;
import com.italiano.vocab.dto.IrregularAnswerDTO;
import com.italiano.vocab.dto.SpellAnswerDTO;
import com.italiano.vocab.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 加练模式：纯练习，不碰 SRS 盒子，答错只进错题本。
 * type: quiz（认识翻卡）/ spell（中→意拼写）/ dict（听音写词）/ irregular（不规则变化专考）
 */
@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    /** 抽题：type=quiz|spell|dict|irregular, count=数量（irregular 为词数，每词多个考点） */
    @GetMapping
    public ApiResponse<Map<String, Object>> draw(
            @RequestParam String type,
            @RequestParam(defaultValue = "20") int count) {
        return ApiResponse.ok(practiceService.draw(type, count));
    }

    /** 认识/不认识判分（quiz 模式） */
    @PostMapping("/{id}/know")
    public ApiResponse<Map<String, Object>> know(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "false") boolean know) {
        return ApiResponse.ok(practiceService.answer("quiz", id, null, null, know));
    }

    /** 拼写判分（spell 模式） */
    @PostMapping("/{id}/spell-answer")
    public ApiResponse<Map<String, Object>> spellAnswer(@PathVariable Long id,
                                                        @RequestBody SpellAnswerDTO body) {
        return ApiResponse.ok(practiceService.answer("spell", id, body.getWord(), null, false));
    }

    /** 听写第一关释义预检（选错立即判错，不让继续拼写；只判断不落库） */
    @PostMapping("/{id}/dict-check-meaning")
    public ApiResponse<Map<String, Object>> dictCheckMeaning(@PathVariable Long id,
                                                             @RequestBody DictAnswerDTO body) {
        return ApiResponse.ok(Map.of("correct", practiceService.checkDictMeaning(id, body.getMeaning())));
    }

    /** 听写判分（dict 模式） */
    @PostMapping("/{id}/dict-answer")
    public ApiResponse<Map<String, Object>> dictAnswer(@PathVariable Long id,
                                                       @RequestBody DictAnswerDTO body) {
        return ApiResponse.ok(practiceService.answer("dict", id,
                body.getWord(), body.getMeaning(), false));
    }

    /** 不规则变化判分（irregular 模式）：考点描述 + 用户输入，现场推导答案比对 */
    @PostMapping("/{id}/irregular-answer")
    public ApiResponse<Map<String, Object>> irregularAnswer(@PathVariable Long id,
                                                            @RequestBody IrregularAnswerDTO body) {
        return ApiResponse.ok(practiceService.answerIrregular(id, body.getType(), body.getPerson(),
                body.getContextNoun(), body.getContextGender(), body.getContextPlural(), body.getInput()));
    }
}
