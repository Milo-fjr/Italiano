package com.italiano.vocab.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判分归一化与中文释义判分测试（2026-09-23 由 DictService/PracticeService 两份私有拷贝上收）。
 * normalize 是全模式判分地基；matchMeaning 为点选口径——输入永远是选项原文，
 * 用例只覆盖点选真实路径与选项去重语义，不测手打中文时代的历史容错。
 */
class ExtraFormServiceTest {

    // ===== normalize：意语输入归一化 =====

    @Test
    void 归一化_重音大小写空格容错() {
        assertEquals("citta", ExtraFormService.normalize("città"));
        assertEquals("citta", ExtraFormService.normalize("CITTÀ"));
        assertEquals("citta", ExtraFormService.normalize("  città  "));
        assertEquals("perche", ExtraFormService.normalize("perché"));
        assertEquals("e", ExtraFormService.normalize("è"));
        // 连续空格折叠为单个（自反动词 mi  alzo → mi alzo）
        assertEquals("mi alzo", ExtraFormService.normalize("mi   alzo"));
        assertEquals("mi alzo", ExtraFormService.normalize("  Mi  ALZO "));
        // null/空安全
        assertEquals("", ExtraFormService.normalize(null));
        assertEquals("", ExtraFormService.normalize("   "));
    }

    @Test
    void 归一化_判分等价场景() {
        // 拼写判分的真实输入形态：不带重音 ≡ 带重音 ≡ 全大写
        assertTrue(ExtraFormService.normalize("citta").equals(ExtraFormService.normalize("città")));
        assertTrue(ExtraFormService.normalize("perche").equals(ExtraFormService.normalize("perché")));
        // 反身短语判分：mi sono svegliato 各种写法
        assertEquals(ExtraFormService.normalize("mi sono svegliato"),
                ExtraFormService.normalize("MI SONO SVEGLIATO"));
    }

    // ===== matchMeaning：释义点选判分 =====

    @Test
    void 释义判分_点对单段() {
        assertTrue(ExtraFormService.matchMeaning("睡觉", "睡觉"));
        assertTrue(ExtraFormService.matchMeaning("朋友", "朋友"));
    }

    @Test
    void 释义判分_多段释义整体点选() {
        // 点选传入整个选项文本（听写题卡的选项就是完整释义串）
        assertTrue(ExtraFormService.matchMeaning("上；登上", "上；登上"));
        // 选项文本与词库释义的分隔符混用（中英文分号等价拆段）
        assertTrue(ExtraFormService.matchMeaning("上;登上", "上；登上"));
    }

    @Test
    void 释义判分_点了干扰项判错() {
        // 陷阱 10 的真实值：sera=傍晚；晚上 / notte=夜里——「夜里」不是 sera 的释义
        assertFalse(ExtraFormService.matchMeaning("夜里", "傍晚；晚上"));
        assertFalse(ExtraFormService.matchMeaning("重量", "品质；质量"));
        assertFalse(ExtraFormService.matchMeaning("床罩", "床单"));
        // 部分匹配不算对（子段必须整段相等）
        assertFalse(ExtraFormService.matchMeaning("床", "床单"));
    }

    @Test
    void 释义判分_未选就放弃判错() {
        // 真实路径：释义关没选直接按 0 放弃，前端传空串/null
        assertFalse(ExtraFormService.matchMeaning("", "睡觉"));
        assertFalse(ExtraFormService.matchMeaning(null, "睡觉"));
    }

    // ===== normalizeMeaning：选项去重语义（当前唯一活用途）=====

    @Test
    void 释义归一化_选项去重() {
        // 防「好」「好的」「好的（非正式）」同现两个选项——归一化后应相等
        assertEquals(ExtraFormService.normalizeMeaning("好"), ExtraFormService.normalizeMeaning("好的"));
        assertEquals(ExtraFormService.normalizeMeaning("好的"), ExtraFormService.normalizeMeaning("好的（非正式）"));
        // 括号注解剔除（中英文括号）+ 空格折叠（全空格剔除）
        assertEquals("ragazzo", ExtraFormService.normalizeMeaning(" ragazzo (男孩)"));
        assertEquals(ExtraFormService.normalizeMeaning("朋友"), ExtraFormService.normalizeMeaning("朋友（复数）"));
        // 尾部虚词剥离列表为「的了呀啊吧呢吗地」——锁定列表边界
        assertEquals("好", ExtraFormService.normalizeMeaning("好的呀"));
        // null/空白安全
        assertEquals("", ExtraFormService.normalizeMeaning(null));
        assertEquals("", ExtraFormService.normalizeMeaning("   "));
    }
}
