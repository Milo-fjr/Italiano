package com.italiano.vocab.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 语法引擎回归测试：锁死所有已查证（Treccani/Crusca/权威词典）与踩坑修正过的结论。
 * 改例外表/规则推导/红标判定前先跑本测试——succo 复数、lago 误标红、avere 分词
 * 三次事故若当时有这层回归网都会当场报警（见 AGENTS.md 陷阱与事故记录）。
 */
class ItalianGrammarUtilTest {

    private static void assertForm(String[] actual, String... expected) {
        assertArrayEquals(expected, actual);
    }

    // ===== 现在时 =====

    @Test
    void 规则现在时_三族变位() {
        assertForm(ItalianGrammarUtil.regularPresent("parlare"), "parlo", "parli", "parla", "parliamo", "parlate", "parlano");
        assertForm(ItalianGrammarUtil.regularPresent("credere"), "credo", "credi", "crede", "crediamo", "credete", "credono");
        assertForm(ItalianGrammarUtil.regularPresent("dormire"), "dormo", "dormi", "dorme", "dormiamo", "dormite", "dormono");
    }

    @Test
    void 规则现在时_拼写音变() {
        // -care/-gare 保硬音加 h（cercare→cerchi）；-iare 去 i（mangiare→mangi）
        assertForm(ItalianGrammarUtil.regularPresent("cercare"), "cerco", "cerchi", "cerca", "cerchiamo", "cercate", "cercano");
        assertForm(ItalianGrammarUtil.regularPresent("pagare"), "pago", "paghi", "paga", "paghiamo", "pagate", "pagano");
        assertForm(ItalianGrammarUtil.regularPresent("mangiare"), "mangio", "mangi", "mangia", "mangiamo", "mangiate", "mangiano");
    }

    @Test
    void 不规则现在时_例外表() {
        assertForm(ItalianGrammarUtil.irregularPresent("essere"), "sono", "sei", "è", "siamo", "siete", "sono");
        assertForm(ItalianGrammarUtil.irregularPresent("avere"), "ho", "hai", "ha", "abbiamo", "avete", "hanno");
        assertForm(ItalianGrammarUtil.irregularPresent("andare"), "vado", "vai", "va", "andiamo", "andate", "vanno");
        assertForm(ItalianGrammarUtil.irregularPresent("venire"), "vengo", "vieni", "viene", "veniamo", "venite", "vengono");
        assertForm(ItalianGrammarUtil.irregularPresent("uscire"), "esco", "esci", "esce", "usciamo", "uscite", "escono");
        assertForm(ItalianGrammarUtil.irregularPresent("bere"), "bevo", "bevi", "beve", "beviamo", "bevete", "bevono");
        assertForm(ItalianGrammarUtil.irregularPresent("salire"), "salgo", "sali", "sale", "saliamo", "salite", "salgono");
        // -iare 重音在 i 上 tu 保留双 i（ringrazii），通用规则会错误地给 ringrazi
        assertForm(ItalianGrammarUtil.irregularPresent("ringraziare"),
                "ringrazio", "ringrazii", "ringrazia", "ringraziamo", "ringraziate", "ringraziano");
    }

    @Test
    void isc型_现在时与归属() {
        // -isc 型是 -ire 的规则子模式：capire→capisco 由引擎推导，pulire 同型
        assertForm(ItalianGrammarUtil.irregularPresent("capire"), "capisco", "capisci", "capisce", "capiamo", "capite", "capiscono");
        assertForm(ItalianGrammarUtil.irregularPresent("pulire"), "pulisco", "pulisci", "pulisce", "puliamo", "pulite", "puliscono");
        // 普通型 -ire：dormo 型不在例外推导里
        assertNull(ItalianGrammarUtil.irregularPresent("dormire"));
        assertNull(ItalianGrammarUtil.irregularPresent("parlare"));
    }

    @Test
    void 考点过滤依据_逐人称与规则推导比对() {
        // 加练考点枚举靠「不规则 != 规则推导」筛考点——锁定这些边界词的推导差异：
        // andare 的 noi/voi 与规则推导相同（被自然过滤），io 不同（出考点）
        String[] andare = ItalianGrammarUtil.irregularPresent("andare");
        String[] andareReg = ItalianGrammarUtil.regularPresent("andare");
        assertEquals(andareReg[3], andare[3]); // andiamo
        assertEquals(andareReg[4], andare[4]); // andate
        assertFalse(andare[0].equals(andareReg[0])); // vado vs ando
        // potere/volere 仅 voi 与规则相同（potete/volete）
        assertEquals(ItalianGrammarUtil.regularPresent("potere")[4], ItalianGrammarUtil.irregularPresent("potere")[4]);
        assertEquals(ItalianGrammarUtil.regularPresent("volere")[4], ItalianGrammarUtil.irregularPresent("volere")[4]);
        // prendere 整表与规则推导一致（现在时整表被过滤，不出现在时考点）
        assertArrayEquals(ItalianGrammarUtil.regularPresent("prendere"), ItalianGrammarUtil.irregularPresent("prendere"));
    }

    // ===== 简单将来时 =====

    @Test
    void 规则将来时() {
        assertForm(ItalianGrammarUtil.regularFuturo("parlare"), "parlerò", "parlerai", "parlerà", "parleremo", "parlerete", "parleranno");
        assertForm(ItalianGrammarUtil.regularFuturo("dormire"), "dormirò", "dormirai", "dormirà", "dormiremo", "dormirete", "dormiranno");
        // -care 加 h 保硬音（giocherò）；-ciare 去 i（mangerò）；普通 -iare 保留 i（cambierò）
        assertForm(ItalianGrammarUtil.regularFuturo("giocare"), "giocherò", "giocherai", "giocherà", "giocheremo", "giocherete", "giocheranno");
        assertForm(ItalianGrammarUtil.regularFuturo("mangiare"), "mangerò", "mangerai", "mangerà", "mangeremo", "mangerete", "mangeranno");
        assertForm(ItalianGrammarUtil.regularFuturo("cambiare"), "cambierò", "cambierai", "cambierà", "cambieremo", "cambierete", "cambieranno");
    }

    @Test
    void 不规则将来时_词干表() {
        assertForm(ItalianGrammarUtil.irregularFuturo("avere"), "avrò", "avrai", "avrà", "avremo", "avrete", "avranno");
        assertForm(ItalianGrammarUtil.irregularFuturo("essere"), "sarò", "sarai", "sarà", "saremo", "sarete", "saranno");
        assertForm(ItalianGrammarUtil.irregularFuturo("venire"), "verrò", "verrai", "verrà", "verremo", "verrete", "verranno");
        assertForm(ItalianGrammarUtil.irregularFuturo("potere"), "potrò", "potrai", "potrà", "potremo", "potrete", "potranno");
        // -sciare 保留 i 维持 /ʃ/（scierò），通用 -ciare 去音规则会错成 scerò
        assertForm(ItalianGrammarUtil.irregularFuturo("sciare"), "scierò", "scierai", "scierà", "scieremo", "scierete", "scieranno");
        assertNull(ItalianGrammarUtil.irregularFuturo("parlare"));
    }

    // ===== 过去分词 =====

    @Test
    void 过去分词_规则三族() {
        assertEquals("parlato", ItalianGrammarUtil.pastParticiple("parlare"));
        assertEquals("creduto", ItalianGrammarUtil.pastParticiple("credere"));
        assertEquals("dormito", ItalianGrammarUtil.pastParticiple("dormire"));
    }

    @Test
    void 过去分词_avere清洁回归() {
        // 2026-09-20 用户发现 avere 误标「近过去时不规则」后的全表核对结论：
        // avere→avuto / stare→stato / dare→dato 恰好符合规则后缀，走规则推导，不在例外表
        assertEquals("avuto", ItalianGrammarUtil.pastParticiple("avere"));
        assertEquals("stato", ItalianGrammarUtil.pastParticiple("stare"));
        assertEquals("dato", ItalianGrammarUtil.pastParticiple("dare"));
        // essere→stato 真不规则（规则推 essuto），保留例外表
        assertEquals("stato", ItalianGrammarUtil.pastParticiple("essere"));
    }

    @Test
    void 过去分词_例外表抽查() {
        assertEquals("fatto", ItalianGrammarUtil.pastParticiple("fare"));
        assertEquals("detto", ItalianGrammarUtil.pastParticiple("dire"));
        assertEquals("preso", ItalianGrammarUtil.pastParticiple("prendere"));
        assertEquals("messo", ItalianGrammarUtil.pastParticiple("mettere"));
        assertEquals("bevuto", ItalianGrammarUtil.pastParticiple("bere"));
        assertEquals("venuto", ItalianGrammarUtil.pastParticiple("venire"));
        assertEquals("visto", ItalianGrammarUtil.pastParticiple("vedere"));
        assertEquals("perso", ItalianGrammarUtil.pastParticiple("perdere"));
        assertEquals("pianto", ItalianGrammarUtil.pastParticiple("piangere"));
    }

    // ===== 名词复数 =====

    @Test
    void 复数_ca_ga_加h() {
        assertEquals("amiche", ItalianGrammarUtil.buildPlural("amica", "s.f."));
        assertEquals("banche", ItalianGrammarUtil.buildPlural("banca", "s.f."));
        assertEquals("righe", ItalianGrammarUtil.buildPlural("riga", "s.f."));
    }

    @Test
    void 复数_cia_gia_看前一字母() {
        // 辅音前（含双写）→ 去 i（faccia→facce）；元音后 → 保 i（camicia→camicie）
        // 2026-09-18 全核对过的 11 词（AGENTS 陷阱 15）
        assertEquals("facce", ItalianGrammarUtil.buildPlural("faccia", "s.f."));
        assertEquals("arance", ItalianGrammarUtil.buildPlural("arancia", "s.f."));
        assertEquals("piogge", ItalianGrammarUtil.buildPlural("pioggia", "s.f."));
        assertEquals("spiagge", ItalianGrammarUtil.buildPlural("spiaggia", "s.f."));
        assertEquals("mance", ItalianGrammarUtil.buildPlural("mancia", "s.f."));
        assertEquals("camicie", ItalianGrammarUtil.buildPlural("camicia", "s.f."));
        assertEquals("farmacie", ItalianGrammarUtil.buildPlural("farmacia", "s.f."));
        assertEquals("bugie", ItalianGrammarUtil.buildPlural("bugia", "s.f."));
        assertEquals("valigie", ItalianGrammarUtil.buildPlural("valigia", "s.f."));
        assertEquals("fiducie", ItalianGrammarUtil.buildPlural("fiducia", "s.f."));
        assertEquals("ciliegie", ItalianGrammarUtil.buildPlural("ciliegia", "s.f."));
    }

    @Test
    void 复数_co_go_全查例外表() {
        // 加 h 型（piana 重音，12 词已移除红标）；succo 为 2026-09-18 查证修正值（原误写 succi）
        assertEquals("laghi", ItalianGrammarUtil.buildPlural("lago", "s.m."));
        assertEquals("fuochi", ItalianGrammarUtil.buildPlural("fuoco", "s.m."));
        assertEquals("succhi", ItalianGrammarUtil.buildPlural("succo", "s.m."));
        assertEquals("cuochi", ItalianGrammarUtil.buildPlural("cuoco", "s.m."));
        assertEquals("parchi", ItalianGrammarUtil.buildPlural("parco", "s.m."));
        assertEquals("funghi", ItalianGrammarUtil.buildPlural("fungo", "s.m."));
        // 软音型（sdrucciola 重音文本不可判，全靠表）
        assertEquals("amici", ItalianGrammarUtil.buildPlural("amico", "s.m."));
        assertEquals("medici", ItalianGrammarUtil.buildPlural("medico", "s.m."));
        assertEquals("stomaci", ItalianGrammarUtil.buildPlural("stomaco", "s.m."));
        assertEquals("traffici", ItalianGrammarUtil.buildPlural("traffico", "s.m."));
        // 表外 -co/-go 重音不可知，返回 null 留手动编辑（表照存、推导不猜）
        assertNull(ItalianGrammarUtil.buildPlural("fiasco", "s.m."));
    }

    @Test
    void 复数_强不规则与特殊() {
        assertEquals("braccia", ItalianGrammarUtil.buildPlural("braccio", "s.m."));
        assertEquals("uova", ItalianGrammarUtil.buildPlural("uovo", "s.m."));
        assertEquals("mani", ItalianGrammarUtil.buildPlural("mano", "s.f."));
        assertEquals("dita", ItalianGrammarUtil.buildPlural("dito", "s.m."));
        assertEquals("zii", ItalianGrammarUtil.buildPlural("zio", "s.m."));
        assertEquals("uomini", ItalianGrammarUtil.buildPlural("uomo", "s.m."));
        // lenzuolo 事件结论（Crusca 权威确认）：i lenzuoli / le lenzuola 双重复数均正确，表取后者
        assertEquals("lenzuola", ItalianGrammarUtil.buildPlural("lenzuolo", "s.m."));
        // 双性别并列形式「/」
        assertEquals("colleghi/colleghe", ItalianGrammarUtil.buildPlural("collega", "s.m./s.f."));
        assertEquals("turisti/turiste", ItalianGrammarUtil.buildPlural("turista", "s.m./s.f."));
    }

    @Test
    void 复数_规则与不变() {
        assertEquals("notti", ItalianGrammarUtil.buildPlural("notte", "s.f."));
        assertEquals("mari", ItalianGrammarUtil.buildPlural("mare", "s.m."));
        assertEquals("figli", ItalianGrammarUtil.buildPlural("figlio", "s.m."));
        assertEquals("uffici", ItalianGrammarUtil.buildPlural("ufficio", "s.m."));
        assertEquals("mogli", ItalianGrammarUtil.buildPlural("moglie", "s.f."));
        // 希腊词源 -ma 阳性
        assertEquals("problemi", ItalianGrammarUtil.buildPlural("problema", "s.m."));
        assertEquals("climi", ItalianGrammarUtil.buildPlural("clima", "s.m."));
        // 重音结尾与不变复数名词 → 原词
        assertEquals("città", ItalianGrammarUtil.buildPlural("città", "s.f."));
        assertEquals("caffè", ItalianGrammarUtil.buildPlural("caffè", "s.m."));
        assertEquals("bar", ItalianGrammarUtil.buildPlural("bar", "s.m."));
        assertEquals("film", ItalianGrammarUtil.buildPlural("film", "s.m."));
        // 不可数名词无复数 → null
        assertNull(ItalianGrammarUtil.buildPlural("fame", "s.f."));
        assertNull(ItalianGrammarUtil.buildPlural("latte", "s.m."));
        assertNull(ItalianGrammarUtil.buildPlural("denaro", "s.m."));
    }

    // ===== 红标哲学（irregularTag）=====

    @Test
    void 红标_动词_精确标注时态() {
        assertEquals("现在时不规则、将来时不规则", ItalianGrammarUtil.irregularTag("avere", "v.", null));
        assertEquals("现在时不规则、将来时不规则", ItalianGrammarUtil.irregularTag("stare", "v.", null));
        assertEquals("现在时不规则、将来时不规则", ItalianGrammarUtil.irregularTag("dare", "v.", null));
        // essere（ero）/fare（facevo）在未完成时例外表内，标签含未完成时不规则
        assertEquals("现在时不规则、近过去时不规则、未完成时不规则、将来时不规则", ItalianGrammarUtil.irregularTag("essere", "v.", null));
        assertEquals("现在时不规则、近过去时不规则、未完成时不规则、将来时不规则", ItalianGrammarUtil.irregularTag("fare", "v.", null));
        assertEquals("现在时不规则、近过去时不规则、未完成时不规则、将来时不规则", ItalianGrammarUtil.irregularTag("bere", "v.", null));
    }

    @Test
    void 红标_ire动词_模式标注不叠加() {
        // -isc 型是规则子模式 → 浅绿模式标注；真不规则 -ire 只有红标、不叠加模式
        assertEquals("-isc 型", ItalianGrammarUtil.irregularTag("pulire", "v.", null));
        assertEquals("-isc 型", ItalianGrammarUtil.irregularTag("capire", "v.", null));
        assertEquals("普通型", ItalianGrammarUtil.irregularTag("dormire", "v.", null));
        assertEquals("现在时不规则", ItalianGrammarUtil.irregularTag("uscire", "v.", null));
        assertEquals("现在时不规则", ItalianGrammarUtil.irregularTag("riuscire", "v.", null));
        // 规则动词与拼写音变类（-care/-gare）不标——io 形式规则，无记忆价值
        assertNull(ItalianGrammarUtil.irregularTag("parlare", "v.", null));
        assertNull(ItalianGrammarUtil.irregularTag("cercare", "v.", null));
        assertNull(ItalianGrammarUtil.irregularTag("mangiare", "v.", null));
    }

    @Test
    void 红标_名词_加h型不标_例外表照存() {
        // 加 h 型 -co/-go（laghi/banche 同招可推导）不标红（2026-09-18 lago 事件结论）
        assertNull(ItalianGrammarUtil.irregularTag("lago", "s.m.", "m"));
        assertNull(ItalianGrammarUtil.irregularTag("succo", "s.m.", "m"));
        assertNull(ItalianGrammarUtil.irregularTag("fuoco", "s.m.", "m"));
        assertNull(ItalianGrammarUtil.irregularTag("parco", "s.m.", "m"));
        // 软音型与强不规则保留红标
        assertEquals("不规则复数", ItalianGrammarUtil.irregularTag("amico", "s.m.", "m"));
        assertEquals("不规则复数", ItalianGrammarUtil.irregularTag("medico", "s.m.", "m"));
        assertEquals("不规则复数", ItalianGrammarUtil.irregularTag("braccio", "s.m.", "m"));
        // 标签叠加：mano 复数不规则且 -o 阴性
        assertEquals("不规则复数、阴阳性特殊", ItalianGrammarUtil.irregularTag("mano", "s.f.", "f"));
        // 拼写陷阱类（-ca/-ga/-cia/-gia）「音变」标签已删，不标
        assertNull(ItalianGrammarUtil.irregularTag("faccia", "s.f.", "f"));
        assertNull(ItalianGrammarUtil.irregularTag("banca", "s.f.", "f"));
        assertNull(ItalianGrammarUtil.irregularTag("camicia", "s.f.", "f"));
        // -e 结尾性别需记（mouse 复数不变标签已删，只留性别）；普通名词不标
        assertEquals("性别需记", ItalianGrammarUtil.irregularTag("mare", "s.m.", "m"));
        assertEquals("性别需记", ItalianGrammarUtil.irregularTag("mouse", "s.m.", "m"));
        assertNull(ItalianGrammarUtil.irregularTag("computer", "s.m.", "m"));
    }

    @Test
    void 红标_形容词() {
        assertEquals("冠词式变化", ItalianGrammarUtil.irregularTag("bello", "agg.", null));
        assertEquals("冠词式变化", ItalianGrammarUtil.irregularTag("buono", "agg.", null));
        assertEquals("冠词式变化", ItalianGrammarUtil.irregularTag("quello", "agg.", null));
        assertEquals("不规则变化", ItalianGrammarUtil.irregularTag("antico", "agg.", null));
        assertEquals("不规则变化", ItalianGrammarUtil.irregularTag("simpatico", "agg.", null));
        assertEquals("不规则变化", ItalianGrammarUtil.irregularTag("blu", "agg.", null));
        assertNull(ItalianGrammarUtil.irregularTag("grande", "agg.", null));
    }

    // ===== bello 型定语（BELLO_PRACTICE 全语境）=====

    @Test
    void bello定语_七语境全覆盖() {
        assertEquals("bel", ItalianGrammarUtil.belloAttributive("bello", "libro", "m", false));
        assertEquals("bello", ItalianGrammarUtil.belloAttributive("bello", "studente", "m", false));
        assertEquals("bell'", ItalianGrammarUtil.belloAttributive("bello", "amico", "m", false));
        assertEquals("bei", ItalianGrammarUtil.belloAttributive("bello", "libri", "m", true));
        assertEquals("begli", ItalianGrammarUtil.belloAttributive("bello", "studenti", "m", true));
        assertEquals("begli", ItalianGrammarUtil.belloAttributive("bello", "amici", "m", true));
        assertEquals("bell'", ItalianGrammarUtil.belloAttributive("bello", "amica", "f", false));
        assertEquals("bella", ItalianGrammarUtil.belloAttributive("bello", "casa", "f", false));
        assertEquals("belle", ItalianGrammarUtil.belloAttributive("bello", "case", "f", true));
    }

    @Test
    void buono定语_截断式无省音符() {
        assertEquals("buon", ItalianGrammarUtil.belloAttributive("buono", "libro", "m", false));
        assertEquals("buono", ItalianGrammarUtil.belloAttributive("buono", "studente", "m", false));
        // 元音开头用截断式 buon（无省音符），与 bello 型的 bell' 不同
        assertEquals("buon", ItalianGrammarUtil.belloAttributive("buono", "amico", "m", false));
        assertEquals("buon'", ItalianGrammarUtil.belloAttributive("buono", "amica", "f", false));
        assertEquals("buoni", ItalianGrammarUtil.belloAttributive("buono", "libri", "m", true));
        assertEquals("quel", ItalianGrammarUtil.belloAttributive("quello", "libro", "m", false));
        // 非 bello 型返回 null
        assertNull(ItalianGrammarUtil.belloAttributive("grande", "libro", "m", false));
    }

    // ===== 形容词性数变化 =====

    @Test
    void 形容词四格() {
        Map<String, String> antico = ItalianGrammarUtil.buildAdjectiveForms("antico", "agg.");
        assertEquals("antico", antico.get("ms"));
        assertEquals("antica", antico.get("fs"));
        assertEquals("antichi", antico.get("mp"));
        assertEquals("antiche", antico.get("fp"));
        // 软音复数不加 h
        Map<String, String> simpatico = ItalianGrammarUtil.buildAdjectiveForms("simpatico", "agg.");
        assertEquals("simpatici", simpatico.get("mp"));
        assertEquals("simpatiche", simpatico.get("fp"));
        // -e 结尾二式
        Map<String, String> grande = ItalianGrammarUtil.buildAdjectiveForms("grande", "agg.");
        assertEquals("grandi", grande.get("mp"));
        assertEquals("grandi", grande.get("fp"));
        // -io 结尾不加 h
        Map<String, String> vecchio = ItalianGrammarUtil.buildAdjectiveForms("vecchio", "agg.");
        assertEquals("vecchi", vecchio.get("mp"));
        assertEquals("vecchie", vecchio.get("fp"));
        // bello 型返回定语形式表
        Map<String, String> bello = ItalianGrammarUtil.buildAdjectiveForms("bello", "agg.");
        assertEquals("bel / bello / bell'", bello.get("ms"));
        assertEquals("bei / begli", bello.get("mp"));
        // 不变形容词返回 null
        assertNull(ItalianGrammarUtil.buildAdjectiveForms("blu", "agg."));
    }

    @Test
    void 不变形容词考点枚举() {
        assertTrue(ItalianGrammarUtil.isInvariantAdjective("blu"));
        assertTrue(ItalianGrammarUtil.isInvariantAdjective("arancione"));
        // ogni/qualche/nessuno 本身无复数形式，不进考点
        assertFalse(ItalianGrammarUtil.isInvariantAdjective("ogni"));
        assertFalse(ItalianGrammarUtil.isInvariantAdjective("qualche"));
        assertFalse(ItalianGrammarUtil.isInvariantAdjective("nessuno"));
    }

    // ===== 四时态变位组装（buildConjugation）=====

    @Test
    void 变位_规则动词四时态() {
        Map<String, Map<String, String>> conj = ItalianGrammarUtil.buildConjugation("parlare", "v.");
        assertNotNull(conj);
        assertEquals("parlo", conj.get("present").get("io"));
        assertEquals("ho parlato", conj.get("passatoProssimo").get("io"));
        assertEquals("hanno parlato", conj.get("passatoProssimo").get("loro"));
        assertEquals("parlavo", conj.get("imperfetto").get("io"));
        assertEquals("parlerò", conj.get("futuro").get("io"));
        assertNull(ItalianGrammarUtil.buildConjugation("casa", "s.f."));
    }

    @Test
    void 变位_essere助动词与性数配合() {
        Map<String, String> pp = ItalianGrammarUtil.buildConjugation("arrivare", "v.").get("passatoProssimo");
        assertEquals("sono arrivato/a", pp.get("io"));
        // 复数性数配合标注格式：分词 + /i/e（引擎设计，如 arrivato/i/e）
        assertEquals("sono arrivato/i/e", pp.get("loro"));
    }

    @Test
    void 变位_双助动词守卫_camminare永远不被加回() {
        // 2026-09 双助动词事故回归网：camminare/nuotare 是动作方式动词，只用 avere、分词不变性数，
        // DUAL_AUX_VERBS 永远不得收录（误加曾致数据错误，fixDualAuxV6 回滚）
        Map<String, String> camminare = ItalianGrammarUtil.buildConjugation("camminare", "v.").get("passatoProssimo");
        assertEquals("ho camminato", camminare.get("io"));
        assertEquals("hanno camminato", camminare.get("loro"));
        Map<String, String> nuotare = ItalianGrammarUtil.buildConjugation("nuotare", "v.").get("passatoProssimo");
        assertEquals("ho nuotato", nuotare.get("io"));
        assertEquals("hanno nuotato", nuotare.get("loro"));
        // 合法双助动词显示两种形式并配性数
        assertEquals("ho/sono vissuto/a", ItalianGrammarUtil.buildConjugation("vivere", "v.").get("passatoProssimo").get("io"));
        assertEquals("ho/sono corso/a", ItalianGrammarUtil.buildConjugation("correre", "v.").get("passatoProssimo").get("io"));
    }

    @Test
    void 变位_自反动词与未完成时例外表() {
        Map<String, Map<String, String>> svegliarsi = ItalianGrammarUtil.buildConjugation("svegliarsi", "v.rifl.");
        assertEquals("mi sveglio", svegliarsi.get("present").get("io"));
        assertEquals("mi sono svegliato/a", svegliarsi.get("passatoProssimo").get("io"));
        // 未完成时例外表仅四词（其余走规则 -avo/-evo/-ivo）
        Map<String, Map<String, String>> essere = ItalianGrammarUtil.buildConjugation("essere", "v.");
        assertEquals("ero", essere.get("imperfetto").get("io"));
        assertEquals("facevo", ItalianGrammarUtil.buildConjugation("fare", "v.").get("imperfetto").get("io"));
        assertEquals("dicevo", ItalianGrammarUtil.buildConjugation("dire", "v.").get("imperfetto").get("io"));
        assertEquals("bevevo", ItalianGrammarUtil.buildConjugation("bere", "v.").get("imperfetto").get("io"));
        assertEquals("sarò", essere.get("futuro").get("io"));
    }

    // ===== 冠词与词性 =====

    @Test
    void 定冠词推断() {
        assertEquals("il", ItalianGrammarUtil.inferArticle("libro", "s.m.", "m"));
        assertEquals("lo", ItalianGrammarUtil.inferArticle("studente", "s.m.", "m"));
        assertEquals("lo", ItalianGrammarUtil.inferArticle("zaino", "s.m.", "m"));
        assertEquals("l'", ItalianGrammarUtil.inferArticle("amico", "s.m.", "m"));
        assertEquals("la", ItalianGrammarUtil.inferArticle("casa", "s.f.", "f"));
        // 复数
        assertEquals("i", ItalianGrammarUtil.inferArticle("libri", "s.m. pl.", "m"));
        assertEquals("gli", ItalianGrammarUtil.inferArticle("amici", "s.m. pl.", "m"));
        assertEquals("le", ItalianGrammarUtil.inferArticle("case", "s.f. pl.", "f"));
        // 双性别名词
        assertEquals("l'", ItalianGrammarUtil.inferArticle("autista", "s.m./s.f.", null));
        assertEquals("il/la", ItalianGrammarUtil.inferArticle("turista", "s.m./s.f.", null));
    }

    @Test
    void 不定冠词推断() {
        assertEquals("un", ItalianGrammarUtil.indefiniteArticle("amico", "s.m.", "m"));
        assertEquals("uno", ItalianGrammarUtil.indefiniteArticle("studente", "s.m.", "m"));
        assertEquals("un'", ItalianGrammarUtil.indefiniteArticle("amica", "s.f.", "f"));
        assertEquals("una", ItalianGrammarUtil.indefiniteArticle("casa", "s.f.", "f"));
    }

    @Test
    void 复数冠词_性别漂移与例外() {
        assertEquals("i", ItalianGrammarUtil.pluralArticle("il", "m", "libro", "libri"));
        assertEquals("gli", ItalianGrammarUtil.pluralArticle("lo", "m", "studente", "studenti"));
        assertEquals("le", ItalianGrammarUtil.pluralArticle("la", "f", "casa", "case"));
        assertEquals("gli", ItalianGrammarUtil.pluralArticle("l'", "m", "amico", "amici"));
        assertEquals("le", ItalianGrammarUtil.pluralArticle("l'", "f", "amica", "amiche"));
        // -o 阳性复数漂移为 -a：冠词转 le（braccio→le braccia）
        assertEquals("le", ItalianGrammarUtil.pluralArticle("il", "m", "braccio", "braccia"));
        // 例外 paio→paia 仍阳性（i paia）
        assertEquals("i", ItalianGrammarUtil.pluralArticle("il", "m", "paio", "paia"));
        // 双性别
        assertEquals("i/le", ItalianGrammarUtil.pluralArticle("il/la", null, "turista", null));
        assertEquals("gli/le", ItalianGrammarUtil.pluralArticle("l'", null, "autista", null));
    }

    @Test
    void 词性与性别推断() {
        assertTrue(ItalianGrammarUtil.isNounPos("s.m."));
        assertTrue(ItalianGrammarUtil.isNounPos("s.m./s.f."));
        assertFalse(ItalianGrammarUtil.isNounPos("s.m. pl."));
        assertFalse(ItalianGrammarUtil.isNounPos("v."));
        assertEquals("m", ItalianGrammarUtil.inferGender("s.m."));
        assertEquals("f", ItalianGrammarUtil.inferGender("s.f."));
        assertNull(ItalianGrammarUtil.inferGender("s.m./s.f."));
        assertNull(ItalianGrammarUtil.inferGender("v."));
        // 拼写陷阱判定（纯语法保留，当前无调用方）
        assertTrue(ItalianGrammarUtil.isPluralTrapNoun("banca"));
        assertTrue(ItalianGrammarUtil.isPluralTrapNoun("faccia"));
        assertTrue(ItalianGrammarUtil.isPluralTrapNoun("camicia"));
        assertFalse(ItalianGrammarUtil.isPluralTrapNoun("libro"));
    }

    @Test
    void 人称数组与加练语境表结构() {
        assertArrayEquals(new String[]{"io", "tu", "lui/lei", "noi", "voi", "loro"}, ItalianGrammarUtil.PERSONS);
        assertEquals(7, ItalianGrammarUtil.BELLO_PRACTICE.length);
        // 语境表第四列只能为 pl 或空
        for (String[] ctx : ItalianGrammarUtil.BELLO_PRACTICE) {
            assertTrue("".equals(ctx[3]) || "pl".equals(ctx[3]));
        }
    }
}
