package com.italiano.vocab.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PracticeService.matchesAny 判分测试（包可见静态方法，无需起 Spring）：
 * 不规则变化专考的拼写判分——"/" 分隔多形式任答其一 + 归一化容错。
 */
class PracticeServiceMatchesAnyTest {

    @Test
    void 单形式_归一化容错() {
        assertTrue(PracticeService.matchesAny("amici", "amici"));
        assertTrue(PracticeService.matchesAny("AMICI", "amici"));
        assertTrue(PracticeService.matchesAny("citta", "città"));
        assertTrue(PracticeService.matchesAny("facce", "facce"));
    }

    @Test
    void 多形式_任答其一() {
        // 双性别并列复数：collega→colleghi/colleghe、pilota→piloti/pilote、turista→turisti/turiste
        assertTrue(PracticeService.matchesAny("colleghi", "colleghi/colleghe"));
        assertTrue(PracticeService.matchesAny("colleghe", "colleghi/colleghe"));
        assertTrue(PracticeService.matchesAny("piloti", "piloti/pilote"));
        assertTrue(PracticeService.matchesAny("pilote", "piloti/pilote"));
        assertTrue(PracticeService.matchesAny("turisti", "turisti/turiste"));
        assertTrue(PracticeService.matchesAny("turiste", "turisti/turiste"));
    }

    @Test
    void 判错场景() {
        // 答案子串不算对（colle 不命中 colleghi/colleghe）
        assertFalse(PracticeService.matchesAny("colle", "colleghi/colleghe"));
        assertFalse(PracticeService.matchesAny("amico", "amici"));
        // 空输入判错（「不会」路径）
        assertFalse(PracticeService.matchesAny("", "amici"));
        assertFalse(PracticeService.matchesAny("   ", "amici"));
        assertFalse(PracticeService.matchesAny(null, "amici"));
    }
}
