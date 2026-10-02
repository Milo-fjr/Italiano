package com.italiano.vocab.service;

import com.italiano.vocab.service.PracticeService.NounGates;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 名词专考考点推导（纯函数，不起 Spring 不连 DB）——只考两类纯记忆项：-e 性别 + 完全不规则复数 */
class PracticeServiceNounGatesTest {

    private static final String NO_VALUE = null;

    @Test
    void 规则族整词跳过() {
        // 直推复数与直推冠词
        assertNull(PracticeService.deriveNounGates("libro", "s.m.", "m", "il", "libri"));
        assertNull(PracticeService.deriveNounGates("casa", "s.f.", "f", "la", "case"));
        // 拼写陷阱（加 h 规则族）
        assertNull(PracticeService.deriveNounGates("pesca", "s.f.", "f", "la", "pesche"));
        assertNull(PracticeService.deriveNounGates("banca", "s.f.", "f", "la", NO_VALUE));
        assertNull(PracticeService.deriveNounGates("bosco", "s.m.", "m", "il", NO_VALUE));
        // lo/l'→gli 规则族
        assertNull(PracticeService.deriveNounGates("albero", "s.m.", "m", "l'", "alberi"));
        assertNull(PracticeService.deriveNounGates("aereo", "s.m.", "m", "l'", "aerei"));
        // 不变复数（film 类 + 月份 giugno）
        assertNull(PracticeService.deriveNounGates("film", "s.m.", "m", "il", "film"));
        assertNull(PracticeService.deriveNounGates("giugno", "s.m.", "m", "il", "giugno"));
        // -io 双 i 规则族
        assertNull(PracticeService.deriveNounGates("zio", "s.m.", "m", "lo", "zii"));
    }

    @Test
    void e结尾名词只考性别关() {
        NounGates g = PracticeService.deriveNounGates("dottore", "s.m.", "m", "il", NO_VALUE);
        assertNotNull(g);
        assertTrue(g.hasArticleGate());
        assertEquals("il", g.article());
        assertFalse(g.hasPluralSpell());
        assertFalse(g.hasPluralArticle());
        NounGates f = PracticeService.deriveNounGates("febbre", "s.f.", "f", "la", NO_VALUE);
        assertTrue(f.hasArticleGate());
        assertFalse(f.hasPluralSpell());
        assertFalse(f.hasPluralArticle());
    }

    @Test
    void 性别反常词考性别关() {
        // -a 阳性 / -o 阴性：词尾骗人
        NounGates p = PracticeService.deriveNounGates("problema", "s.m.", "m", "il", "problemi");
        assertTrue(p.hasArticleGate());
        assertFalse(p.hasPluralSpell());
        assertFalse(p.hasPluralArticle());
        NounGates m = PracticeService.deriveNounGates("mano", "s.f.", "f", "la", "mani");
        assertTrue(m.hasArticleGate());
        assertFalse(m.hasPluralSpell()); // mani 是直推形，性别才是考点
        assertFalse(m.hasPluralArticle());
    }

    @Test
    void 完全不规则复数考拼写() {
        NounGates u = PracticeService.deriveNounGates("uomo", "s.m.", "m", "l'", "uomini");
        assertFalse(u.hasArticleGate());
        assertTrue(u.hasPluralSpell());
        assertEquals("uomini", u.plural());
        assertFalse(u.hasPluralArticle()); // l'→gli 非性别漂移口径
        NounGates d = PracticeService.deriveNounGates("dio", "s.m.", "m", "il", "dei");
        assertTrue(d.hasPluralSpell());
        assertFalse(d.hasPluralArticle()); // il→i 直推
    }

    @Test
    void 复数性别漂移考关3() {
        NounGates b = PracticeService.deriveNounGates("braccio", "s.m.", "m", "il", "braccia");
        assertFalse(b.hasArticleGate());
        assertTrue(b.hasPluralSpell());
        assertTrue(b.hasPluralArticle());
        assertEquals("le", b.pluralArticle());
        NounGates g = PracticeService.deriveNounGates("ginocchio", "s.m.", "m", "il", "ginocchia");
        assertTrue(g.hasPluralSpell());
        assertTrue(g.hasPluralArticle());
        assertEquals("le", g.pluralArticle());
        NounGates l = PracticeService.deriveNounGates("lenzuolo", "s.m.", "m", "il", "lenzuola");
        assertTrue(l.hasPluralSpell());
        assertTrue(l.hasPluralArticle());
        assertEquals("le", l.pluralArticle());
    }

    @Test
    void 双形式复数任答一() {
        NounGates c = PracticeService.deriveNounGates("collega", "s.f.", "f", "la", "colleghi/colleghe");
        assertTrue(c.hasPluralSpell());
        assertEquals("colleghi/colleghe", c.plural());
        assertFalse(c.hasPluralArticle());
    }

    @Test
    void 双性别与双冠词整词跳过() {
        assertNull(PracticeService.deriveNounGates("autista", "s.m./s.f.", NO_VALUE, NO_VALUE, NO_VALUE));
        assertNull(PracticeService.deriveNounGates("turista", "s.m./s.f.", "m", "il/la", NO_VALUE));
    }

    @Test
    void 关1与关3选项组装() {
        List<String> a = PracticeService.articleOptionsFor("il");
        assertEquals(4, a.size());
        assertTrue(a.contains("il"));
        assertEquals(4, a.stream().distinct().count());
        List<String> pa = PracticeService.pluralArticleOptionsFor("i");
        assertEquals(4, pa.size());
        assertTrue(pa.containsAll(List.of("i", "gli", "le", "l'")));
    }
}
