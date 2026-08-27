package com.italiano.vocab.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 意大利语语法工具：名词冠词/复数推断、形容词性数变化、动词四时态变位生成
 * （导入时预填，均支持手动编辑修正）
 */
public final class ItalianGrammarUtil {

    private ItalianGrammarUtil() {
    }

    /** 变位表人称顺序 */
    private static final String[] PERSONS = {"io", "tu", "lui/lei", "noi", "voi", "loro"};

    /** 反身代词（与 PERSONS 一一对应） */
    private static final String[] REFLEXIVE_PRONOUNS = {"mi", "ti", "si", "ci", "vi", "si"};

    /** 高频不规则动词现在时变位表（词库中已确认存在的高频词） */
    private static final Map<String, String[]> IRREGULAR_PRESENT = new LinkedHashMap<>();

    static {
        IRREGULAR_PRESENT.put("essere", new String[]{"sono", "sei", "è", "siamo", "siete", "sono"});
        IRREGULAR_PRESENT.put("avere", new String[]{"ho", "hai", "ha", "abbiamo", "avete", "hanno"});
        IRREGULAR_PRESENT.put("fare", new String[]{"faccio", "fai", "fa", "facciamo", "fate", "fanno"});
        IRREGULAR_PRESENT.put("andare", new String[]{"vado", "vai", "va", "andiamo", "andate", "vanno"});
        IRREGULAR_PRESENT.put("venire", new String[]{"vengo", "vieni", "viene", "veniamo", "venite", "vengono"});
        IRREGULAR_PRESENT.put("stare", new String[]{"sto", "stai", "sta", "stiamo", "state", "stanno"});
        IRREGULAR_PRESENT.put("dare", new String[]{"do", "dai", "dà", "diamo", "date", "danno"});
        IRREGULAR_PRESENT.put("dire", new String[]{"dico", "dici", "dice", "diciamo", "dite", "dicono"});
        IRREGULAR_PRESENT.put("bere", new String[]{"bevo", "bevi", "beve", "beviamo", "bevete", "bevono"});
        IRREGULAR_PRESENT.put("uscire", new String[]{"esco", "esci", "esce", "usciamo", "uscite", "escono"});
        IRREGULAR_PRESENT.put("riuscire", new String[]{"riesco", "riesci", "riesce", "riusciamo", "riuscite", "riescono"});
        IRREGULAR_PRESENT.put("dovere", new String[]{"devo", "devi", "deve", "dobbiamo", "dovete", "devono"});
        IRREGULAR_PRESENT.put("potere", new String[]{"posso", "puoi", "può", "possiamo", "potete", "possono"});
        IRREGULAR_PRESENT.put("volere", new String[]{"voglio", "vuoi", "vuole", "vogliamo", "volete", "vogliono"});
        IRREGULAR_PRESENT.put("sapere", new String[]{"so", "sai", "sa", "sappiamo", "sapete", "sanno"});
        IRREGULAR_PRESENT.put("prendere", new String[]{"prendo", "prendi", "prende", "prendiamo", "prendete", "prendono"});
        IRREGULAR_PRESENT.put("mettere", new String[]{"metto", "metti", "mette", "mettiamo", "mettete", "mettono"});
        IRREGULAR_PRESENT.put("rimanere", new String[]{"rimango", "rimani", "rimane", "rimaniamo", "rimanete", "rimangono"});
        IRREGULAR_PRESENT.put("scegliere", new String[]{"scelgo", "scegli", "sceglie", "scegliamo", "scegliete", "scelgono"});
        IRREGULAR_PRESENT.put("piacere", new String[]{"piaccio", "piaci", "piace", "piacciamo", "piacete", "piacciono"});
        IRREGULAR_PRESENT.put("morire", new String[]{"muoio", "muori", "muore", "moriamo", "morite", "muoiono"});
        IRREGULAR_PRESENT.put("salire", new String[]{"salgo", "sali", "sale", "saliamo", "salite", "salgono"});
        IRREGULAR_PRESENT.put("sedere", new String[]{"siedo", "siedi", "siede", "sediamo", "sedete", "siedono"});
        IRREGULAR_PRESENT.put("tenere", new String[]{"tengo", "tieni", "tiene", "teniamo", "tenete", "tengono"});
        IRREGULAR_PRESENT.put("raccogliere", new String[]{"raccolgo", "raccogli", "raccoglie", "raccogliamo", "raccogliete", "raccolgono"});
        // -iare 重音在 i 上：tu 保留双 i（ringrazii/scii），通用规则会错误地去掉
        IRREGULAR_PRESENT.put("ringraziare", new String[]{"ringrazio", "ringrazii", "ringrazia", "ringraziamo", "ringraziate", "ringraziano"});
        IRREGULAR_PRESENT.put("sciare", new String[]{"scio", "scii", "scia", "sciamo", "sciate", "sciano"});
    }

    /** -isc 型 -ire 动词（第一人称 -isco：capire → capisco；trasferire 同型，全表排查补录） */
    private static final Set<String> ISC_VERBS = Set.of(
            "capire", "finire", "preferire", "pulire", "spedire", "costruire", "trasferire");

    /** 不规则过去分词（规则：-are→ato / -ere→uto / -ire→ito） */
    private static final Map<String, String> IRREGULAR_PP = new LinkedHashMap<>();

    static {
        IRREGULAR_PP.put("essere", "stato");
        IRREGULAR_PP.put("avere", "avuto");
        IRREGULAR_PP.put("fare", "fatto");
        IRREGULAR_PP.put("dire", "detto");
        IRREGULAR_PP.put("prendere", "preso");
        IRREGULAR_PP.put("mettere", "messo");
        IRREGULAR_PP.put("aprire", "aperto");
        IRREGULAR_PP.put("chiudere", "chiuso");
        IRREGULAR_PP.put("leggere", "letto");
        IRREGULAR_PP.put("scrivere", "scritto");
        IRREGULAR_PP.put("vedere", "visto");
        IRREGULAR_PP.put("chiedere", "chiesto");
        IRREGULAR_PP.put("rispondere", "risposto");
        IRREGULAR_PP.put("bere", "bevuto");
        IRREGULAR_PP.put("venire", "venuto");
        IRREGULAR_PP.put("vivere", "vissuto");
        IRREGULAR_PP.put("rimanere", "rimasto");
        IRREGULAR_PP.put("scegliere", "scelto");
        IRREGULAR_PP.put("spegnere", "spento");
        IRREGULAR_PP.put("scendere", "sceso");
        IRREGULAR_PP.put("nascere", "nato");
        IRREGULAR_PP.put("morire", "morto");
        IRREGULAR_PP.put("correre", "corso");
        IRREGULAR_PP.put("conoscere", "conosciuto");
        IRREGULAR_PP.put("vincere", "vinto");
        IRREGULAR_PP.put("perdere", "perso");
        IRREGULAR_PP.put("spendere", "speso");
        IRREGULAR_PP.put("ridere", "riso");
        IRREGULAR_PP.put("succedere", "successo");
        IRREGULAR_PP.put("rompere", "rotto");
        IRREGULAR_PP.put("dare", "dato");
        IRREGULAR_PP.put("stare", "stato");
        // 全表排查补充（词库实存动词）
        IRREGULAR_PP.put("accendere", "acceso");
        IRREGULAR_PP.put("correggere", "corretto");
        IRREGULAR_PP.put("crescere", "cresciuto");
        IRREGULAR_PP.put("decidere", "deciso");
        IRREGULAR_PP.put("offrire", "offerto");
        IRREGULAR_PP.put("permettere", "permesso");
        IRREGULAR_PP.put("piacere", "piaciuto");
        IRREGULAR_PP.put("piangere", "pianto");
        IRREGULAR_PP.put("promettere", "promesso");
        IRREGULAR_PP.put("raccogliere", "raccolto");
        IRREGULAR_PP.put("smettere", "smesso");
        IRREGULAR_PP.put("sorridere", "sorriso");
    }

    /** 用 essere 作助动词的不及物动词（其余用 avere；反身动词一律 essere） */
    private static final Set<String> ESSERE_VERBS = Set.of(
            "essere", "stare", "andare", "venire", "partire", "uscire", "entrare",
            "arrivare", "tornare", "restare", "rimanere", "salire", "scendere",
            "nascere", "morire", "diventare", "succedere", "cadere", "piacere",
            "dispiacere", "sembrare", "apparire", "riuscire", "bastare", "costare");

    /** 双助动词动词（avere 及物 / essere 不及物，近过去时两种形式均合法）：显示为 ho/sono vissuto */
    private static final Set<String> DUAL_AUX_VERBS = Set.of(
            "correre", "vivere", "nuotare", "volare", "camminare", "crescere",
            "dimagrire", "migliorare", "peggiorare",
            "passare", "cambiare", "finire", "iniziare", "continuare", "girare", "mancare");

    /** 不规则未完成过去时（规则：-are→avo / -ere→evo / -ire→ivo） */
    private static final Map<String, String[]> IRREGULAR_IMPERFETTO = new LinkedHashMap<>();

    static {
        IRREGULAR_IMPERFETTO.put("essere", new String[]{"ero", "eri", "era", "eravamo", "eravate", "erano"});
        IRREGULAR_IMPERFETTO.put("fare", new String[]{"facevo", "facevi", "faceva", "facevamo", "facevate", "facevano"});
        IRREGULAR_IMPERFETTO.put("dire", new String[]{"dicevo", "dicevi", "diceva", "dicevamo", "dicevate", "dicevano"});
        IRREGULAR_IMPERFETTO.put("bere", new String[]{"bevevo", "bevevi", "beveva", "bevevamo", "bevevate", "bevevano"});
    }

    /** 不规则简单将来时词干（规则：-are→erò / -ere→erò / -ire→irò） */
    private static final Map<String, String> IRREGULAR_FUTURO_STEM = new LinkedHashMap<>();

    static {
        IRREGULAR_FUTURO_STEM.put("essere", "sar");
        IRREGULAR_FUTURO_STEM.put("avere", "avr");
        IRREGULAR_FUTURO_STEM.put("andare", "andr");
        IRREGULAR_FUTURO_STEM.put("fare", "far");
        IRREGULAR_FUTURO_STEM.put("venire", "verr");
        IRREGULAR_FUTURO_STEM.put("potere", "potr");
        IRREGULAR_FUTURO_STEM.put("volere", "vorr");
        IRREGULAR_FUTURO_STEM.put("dovere", "dovr");
        IRREGULAR_FUTURO_STEM.put("sapere", "sapr");
        IRREGULAR_FUTURO_STEM.put("vedere", "vedr");
        IRREGULAR_FUTURO_STEM.put("stare", "star");
        IRREGULAR_FUTURO_STEM.put("dare", "dar");
        IRREGULAR_FUTURO_STEM.put("dire", "dir");
        IRREGULAR_FUTURO_STEM.put("bere", "berr");
        IRREGULAR_FUTURO_STEM.put("rimanere", "rimarr");
        IRREGULAR_FUTURO_STEM.put("vivere", "vivr");
        IRREGULAR_FUTURO_STEM.put("tenere", "terr");
        // -sciare 动词保留 i 维持 /ʃ/ 音（scierò），通用 -ciare 去音规则会错成 scerò
        IRREGULAR_FUTURO_STEM.put("sciare", "scier");
    }

    /** 不规则名词复数（-co/-go 重音不可知无法推导、-a 型复数等，全部显式收录） */
    private static final Map<String, String> IRREGULAR_PLURAL = new LinkedHashMap<>();

    static {
        IRREGULAR_PLURAL.put("uomo", "uomini");
        IRREGULAR_PLURAL.put("dio", "dei");
        // -co/-go 保留硬音加 h（重音在末音节组）
        IRREGULAR_PLURAL.put("cuoco", "cuochi");
        IRREGULAR_PLURAL.put("parco", "parchi");
        IRREGULAR_PLURAL.put("fungo", "funghi");
        IRREGULAR_PLURAL.put("lago", "laghi");
        IRREGULAR_PLURAL.put("albergo", "alberghi");
        IRREGULAR_PLURAL.put("bosco", "boschi");
        IRREGULAR_PLURAL.put("fuoco", "fuochi");
        IRREGULAR_PLURAL.put("impiego", "impieghi");
        IRREGULAR_PLURAL.put("gioco", "giochi");
        IRREGULAR_PLURAL.put("videogioco", "videogiochi");
        IRREGULAR_PLURAL.put("pacco", "pacchi");
        IRREGULAR_PLURAL.put("bianco", "bianchi");
        // -co/-go 不加 h（重音在前，软音 -ci/-gi）
        IRREGULAR_PLURAL.put("amico", "amici");
        IRREGULAR_PLURAL.put("medico", "medici");
        IRREGULAR_PLURAL.put("stomaco", "stomaci");
        IRREGULAR_PLURAL.put("farmaco", "farmaci");
        IRREGULAR_PLURAL.put("succo", "succi");
        IRREGULAR_PLURAL.put("traffico", "traffici");
        IRREGULAR_PLURAL.put("meccanico", "meccanici");
        IRREGULAR_PLURAL.put("idraulico", "idraulici");
        // 强不规则（复数 -a 或交叉性别）
        IRREGULAR_PLURAL.put("braccio", "braccia");
        IRREGULAR_PLURAL.put("uovo", "uova");
        IRREGULAR_PLURAL.put("paio", "paia");
        IRREGULAR_PLURAL.put("dito", "dita");
        IRREGULAR_PLURAL.put("mano", "mani");
        IRREGULAR_PLURAL.put("ginocchio", "ginocchia");
        IRREGULAR_PLURAL.put("orecchio", "orecchie");
        // 重音在 i 上的 -io：复数双 i（zio→zii）
        IRREGULAR_PLURAL.put("zio", "zii");
        // 双性别名词：阳/阴复数并列
        IRREGULAR_PLURAL.put("collega", "colleghi/colleghe");
        // 全表排查补充
        IRREGULAR_PLURAL.put("pigiama", "pigiami");
        IRREGULAR_PLURAL.put("pilota", "piloti/pilote");
        IRREGULAR_PLURAL.put("lenzuolo", "lenzuola");
    }

    /** 不变复数名词：月份与常用外来词/缩写词（复数 = 原词） */
    private static final Set<String> INVARIANT_NOUNS = Set.of(
            "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
            "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre",
            "autobus", "bar", "computer", "email", "film", "hobby", "internet",
            "menu", "password", "sport", "tram", "weekend", "yogurt",
            "cinema", "garage", "mouse", "video", "euro",
            "foto", "bici", "auto");

    /** 不可数名词（按词库词义无复数形式）：fame/sete/sangue 等，复数留空 */
    private static final Set<String> UNCOUNTABLE_NOUNS = Set.of(
            "fame", "sete", "sangue", "nuoto", "tosse", "pasta", "gente", "neve",
            "latte", "miele", "riso", "sale", "pepe", "burro", "grandine", "denaro",
            "ginnastica", "musica", "benzina", "frutta", "salute", "calcio", "mezzogiorno");

    /** 复数加 h 的 -co/-go 形容词（硬音：antico→antichi/antiche） */
    private static final Set<String> ADJ_HARD = Set.of(
            "antico", "bianco", "fresco", "largo", "lungo", "ricco",
            "secco", "sporco", "stanco", "poco");

    /** 复数不加 h 的 -co/-go 形容词（软音：simpatico→simpatici/simpatiche） */
    private static final Set<String> ADJ_SOFT = Set.of(
            "antipatico", "economico", "simpatico");

    /** 不变形容词（性数不变，无变化形式；qualche/nessuno 无复数形式） */
    private static final Set<String> ADJ_INVARIANT = Set.of(
            "blu", "rosa", "viola", "gratis", "ogni", "qualche", "nessuno", "arancione");

    /**
     * 根据词性推断名词性别：含 s.m. → m；含 s.f. → f；
     * 同时含两者（如 s.m./s.f.，词义决定性别）返回 null，交由用户手动编辑
     */
    public static String inferGender(String pos) {
        if (pos == null || pos.isBlank()) {
            return null;
        }
        boolean hasM = pos.contains("s.m.");
        boolean hasF = pos.contains("s.f.");
        if (hasM && hasF) {
            return null;
        }
        if (hasF) {
            return "f";
        }
        if (hasM) {
            return "m";
        }
        return null;
    }

    /**
     * 判定单词的语法形式是否不规则（供卡片加标记，常规词返回 null 不标记）：
     * - 动词（含反身动词，剥 -si 还原不定式）：命中任一例外表
     *   （现在时 / -isc 型 / 过去分词 / 未完成过去时 / 将来时词干）→「不规则变位」；
     *   未命中例外表但属拼写音变类 →「音变」（-care/-gare 加 h、-ciare/-giare 去 i、
     *   -iare 避免双 i：cercare→cerchi、mangiare→mangio、studiare→tu studi）
     * - 名词：不规则复数表 →「不规则复数」；不变复数表 →「复数不变」；
     *   不可数名词无复数不标记；-ca/-ga/-cia/-gia 词尾复数音变 →「音变」
     *   （banca→banche、arancia→arance、camicia→camicie）；
     *   词尾与性别反常（-o 却阴性 / -a 却阳性）→「阴阳性特殊」；
     *   -e 结尾名词性别无法从词尾判断 →「性别需记」
     * - 形容词：加 h / 不加 h / 不变形容词例外表 →「不规则变化」
     * 名词多条命中时按优先级取一：不规则复数 > 复数不变 > 音变 > 阴阳性特殊 > 性别需记
     */
    public static String irregularTag(String word, String pos, String gender) {
        if (word == null || word.isBlank() || pos == null) {
            return null;
        }
        String w = word.toLowerCase();
        // 动词（v. / v.rifl.，反身词剥 -si 后查表，与 buildConjugation 的还原方式一致）
        if (pos.startsWith("v.")) {
            boolean reflexive = w.endsWith("si");
            String infinitive = reflexive ? w.substring(0, w.length() - 2) + "e" : w;
            if (IRREGULAR_PRESENT.containsKey(infinitive) || ISC_VERBS.contains(infinitive)
                    || IRREGULAR_PP.containsKey(infinitive) || IRREGULAR_IMPERFETTO.containsKey(infinitive)
                    || IRREGULAR_FUTURO_STEM.containsKey(infinitive)) {
                return "不规则变位";
            }
            // 音变类：拼写有规律陷阱但必须知道（加 h / 去 i / 避免双 i）
            if (infinitive.endsWith("care") || infinitive.endsWith("gare")
                    || infinitive.endsWith("iare")) {
                return "音变";
            }
            return null;
        }
        // 名词：按优先级 不规则复数 > 复数不变 > 音变 > 阴阳性特殊
        if (isNounPos(pos)) {
            if (IRREGULAR_PLURAL.containsKey(w)) {
                return "不规则复数";
            }
            if (INVARIANT_NOUNS.contains(w)) {
                return "复数不变";
            }
            // 不可数名词无复数形式，无音变陷阱可言
            if (UNCOUNTABLE_NOUNS.contains(w)) {
                // 但 -e 结尾者性别仍需记（il latte ♂ / la fame ♀），不可数只影响复数不影响性别
                return w.endsWith("e") && gender != null ? "性别需记" : null;
            }
            // 音变类：-ca/-ga 复数加 h、-cia/-gia 复数去/留 i（取决于前一字母）
            if (w.endsWith("ca") || w.endsWith("ga") || w.endsWith("cia") || w.endsWith("gia")) {
                return "音变";
            }
            if (gender != null && ((w.endsWith("o") && "f".equals(gender))
                    || (w.endsWith("a") && "m".equals(gender)))) {
                return "阴阳性特殊";
            }
            // -e 结尾名词：阴阳性别无法从词尾判断（il fiore ♂ / la mano ♀），需连同冠词记忆
            if (w.endsWith("e") && gender != null) {
                return "性别需记";
            }
            return null;
        }
        // 形容词（含混合词性 agg./s.m. 等，词在形容词例外表即标记）
        if (pos.contains("agg.")
                && (ADJ_HARD.contains(w) || ADJ_SOFT.contains(w) || ADJ_INVARIANT.contains(w))) {
            return "不规则变化";
        }
        return null;
    }

    /** 是否以元音开头 */
    private static boolean startsWithVowel(String w) {
        return "aeiou".indexOf(w.charAt(0)) >= 0;
    }

    /** 是否为需要 lo/gli 的特殊开头：s+辅音、z、gn、pn、ps、x、y（双字符前缀优先匹配） */
    private static boolean startsWithSpecial(String w) {
        if (w.startsWith("z") || w.startsWith("gn") || w.startsWith("pn")
                || w.startsWith("ps") || w.startsWith("x") || w.startsWith("y")) {
            return true;
        }
        // s+辅音（s impura）
        return w.startsWith("s") && w.length() > 1 && "aeiou".indexOf(w.charAt(1)) < 0;
    }

    /** 是否名词类词性（含混合词性如 agg./s.m.；排除复数名词） */
    private static boolean isNounPos(String pos) {
        return pos != null && (pos.contains("s.m.") || pos.contains("s.f.")) && !pos.contains("pl.");
    }

    /**
     * 推断名词定冠词（导入时预填，可手动编辑）：
     * - 复数名词（词性含 pl.）：阳性按开头给 i/gli，阴性给 le
     * - 元音开头 → l'；阳性特殊开头 → lo，其余 → il；阴性 → la
     * - 双性别名词（s.m./s.f.，gender 为 null）：元音 → l'（l'autista），其余 il/la、lo/la 并列
     */
    public static String inferArticle(String word, String pos, String gender) {
        if (word == null || word.isBlank()) {
            return null;
        }
        String w = word.toLowerCase();
        boolean male = "m".equals(gender);
        boolean mixed = gender == null && isNounPos(pos);
        boolean plural = pos != null && pos.contains("pl.");
        if (plural) {
            if (!male) {
                return "le";
            }
            return (startsWithVowel(w) || startsWithSpecial(w)) ? "gli" : "i";
        }
        if (startsWithVowel(w)) {
            return "l'";
        }
        if (mixed) {
            return startsWithSpecial(w) ? "lo/la" : "il/la";
        }
        if (male) {
            return startsWithSpecial(w) ? "lo" : "il";
        }
        if (gender == null) {
            return null;
        }
        return "la";
    }

    /**
     * 推断名词不定冠词（展示用，不落库）：
     * 阳性：元音/一般辅音 → un（un amico），特殊开头（s+辅音/z/ps/gn/x/y）→ uno（uno studente）
     * 阴性：元音 → un'（un'amica），辅音 → una（una casa）
     */
    public static String indefiniteArticle(String word, String pos, String gender) {
        if (word == null || word.isBlank() || gender == null || !isNounPos(pos)) {
            return null;
        }
        String w = word.toLowerCase();
        boolean male = "m".equals(gender);
        if (startsWithVowel(w)) {
            return male ? "un" : "un'";
        }
        if (male) {
            return startsWithSpecial(w) ? "uno" : "un";
        }
        return "una";
    }

    /**
     * 生成形容词性数变化（四格：阳性单数/阴性单数/阳性复数/阴性复数）：
     * - 四式（-o 结尾）：bello → bella / belli / belle
     * - 二式（-e 结尾）：grande → grande / grandi（单数阴阳同形，复数 -i）
     * - -a 结尾（如 egoista）：单数同形，复数 -i / -e
     * - 不变形容词（blu、gratis 等其他词尾）返回 null，留空手动编辑
     */
    public static Map<String, String> buildAdjectiveForms(String word, String pos) {
        if (word == null || word.isBlank() || pos == null || !pos.contains("agg.")) {
            return null;
        }
        String w = word.toLowerCase();
        if (ADJ_INVARIANT.contains(w)) {
            return null; // 不变形容词（blu、rosa、viola 等性数不变）
        }
        Map<String, String> forms = new LinkedHashMap<>();
        if (w.endsWith("io")) {
            // -io 结尾：去 io 加词尾（doppio→doppi/doppie、vecchio→vecchi/vecchie，不加 h）
            String stem = w.substring(0, w.length() - 2);
            forms.put("ms", w);
            forms.put("fs", stem + "ia");
            forms.put("mp", stem + "i");
            forms.put("fp", stem + "ie");
        } else if (w.endsWith("o")) {
            String stem = w.substring(0, w.length() - 1);
            // -co/-go 形容词：硬音复数加 h（antichi/antiche），软音不加（simpatici/simpatiche）
            boolean hard = ADJ_HARD.contains(w);
            boolean soft = ADJ_SOFT.contains(w);
            forms.put("ms", w);
            forms.put("fs", stem + "a");
            forms.put("mp", hard ? stem + "hi" : stem + "i");
            forms.put("fp", (hard || soft) ? stem + "he" : stem + "e");
        } else if (w.endsWith("e")) {
            String stem = w.substring(0, w.length() - 1);
            forms.put("ms", w);
            forms.put("fs", w);
            forms.put("mp", stem + "i");
            forms.put("fp", stem + "i");
        } else if (w.endsWith("a")) {
            String stem = w.substring(0, w.length() - 1);
            forms.put("ms", w);
            forms.put("fs", w);
            forms.put("mp", stem + "i");
            forms.put("fp", stem + "e");
        } else {
            return null; // 不变形容词（blu、gratis 等）
        }
        return forms;
    }

    /**
     * 生成名词复数形式：
     * - 不规则表优先（-co/-go 全部显式收录：加 h 与不加 h 取决于重音位置，纯文本无法判断）
     * - 重音结尾（città、caffè）→ 不变复数，返回原词
     * - -ca/-ga → -che/-ghe（amica→amiche）；-cia/-gia 按前一字母元音保留 i（camicia→camicie）或辅音去 i（arancia→arance）
     * - -o→-i、-a→-e、-e→-i
     * - 表外 -co/-go（重音不可知）与复数名词（pl.）及其他词尾返回 null，留空手动编辑
     */
    public static String buildPlural(String word, String pos) {
        if (word == null || word.isBlank() || !isNounPos(pos)) {
            return null;
        }
        String w = word.toLowerCase();
        String exception = IRREGULAR_PLURAL.get(w);
        if (exception != null) {
            return exception;
        }
        // 不可数名词（按词库词义）：无复数形式，留空
        if (UNCOUNTABLE_NOUNS.contains(w)) {
            return null;
        }
        // 不变复数（月份、外来词）与重音结尾 → 复数 = 原词
        if (INVARIANT_NOUNS.contains(w) || "àèéìòù".indexOf(w.charAt(w.length() - 1)) >= 0) {
            return w;
        }
        // -ista 双性别职业名词：i turisti / le turiste（阴阳复数并列）
        if (w.endsWith("ista") && pos.contains("s.m.") && pos.contains("s.f.")) {
            String stem = w.substring(0, w.length() - 1);
            return stem + "i/" + stem + "e";
        }
        // -ie 结尾去 e：moglie→mogli、serie→seri
        if (w.endsWith("ie")) {
            return w.substring(0, w.length() - 1);
        }
        // -io 结尾去 o：figlio→figli、ufficio→uffici（zio 等例外已在表中）
        if (w.endsWith("io")) {
            return w.substring(0, w.length() - 1);
        }
        // -cia/-gia：前一字母为元音保留 i，辅音去 i
        if (w.endsWith("cia") || w.endsWith("gia")) {
            if (w.length() >= 4 && "aeiou".indexOf(w.charAt(w.length() - 4)) >= 0) {
                return w.substring(0, w.length() - 1) + "e"; // farmacia→farmacie
            }
            return w.substring(0, w.length() - 2) + "e";     // arancia→arance
        }
        if (w.endsWith("ca") || w.endsWith("ga")) {
            return w.substring(0, w.length() - 1) + "he";    // amica→amiche / riga→righe
        }
        // 希腊词源 -ma 阳性名词：-ma → -mi（problema→problemi、clima→climi、diploma→diplomi）
        if (w.endsWith("ma")) {
            return w.substring(0, w.length() - 1) + "i";
        }
        // -co/-go：复数是否加 h 依赖重音位置（书写无重音符号），表外无法推导
        if (w.endsWith("co") || w.endsWith("go")) {
            return null;
        }
        if (w.endsWith("o")) {
            return w.substring(0, w.length() - 1) + "i";
        }
        if (w.endsWith("a")) {
            return w.substring(0, w.length() - 1) + "e";
        }
        if (w.endsWith("e")) {
            return w.substring(0, w.length() - 1) + "i";
        }
        return null; // 其他词尾（外来词等）留空手动编辑
    }

    /**
     * 由单数定冠词推导复数定冠词（详情展示用）；本身已是复数（i/gli/le）则原样返回。
     * 复数性别漂移：-o 阳性名词复数为 -a（braccio→braccia、uovo→uova）时冠词转阴性复数 le；
     * 例外 paio→paia 仍为阳性（i paia）。
     * 双性别名词：il/la → i/le、lo/la → gli/le、l' → gli/le。
     */
    public static String pluralArticle(String article, String gender, String word, String plural) {
        if (plural != null && word != null && word.endsWith("o") && plural.endsWith("a")
                && !"paio".equals(word.toLowerCase())) {
            return "le";
        }
        if (article == null) {
            return null;
        }
        if (gender == null && article.contains("/")) {
            return switch (article) {
                case "il/la" -> "i/le";
                default -> "gli/le"; // lo/la
            };
        }
        if (gender == null && "l'".equals(article)) {
            return "gli/le"; // 双性别元音开头：l'autista → gli/le autisti/autiste
        }
        return switch (article) {
            case "il" -> "i";
            case "lo" -> "gli";
            case "l'" -> "m".equals(gender) ? "gli" : "le";
            case "la" -> "le";
            default -> article;
        };
    }

    /**
     * 生成动词四时态变位（现在时/近过去时/未完成过去时/简单将来时，各六人称）：
     * - 不规则动词查内置表；反身动词剥离 si 后变位并加反身代词
     * - 近过去时按动词类型选 avere/essere 助动词；essere 类分词标注性数配合（arrivato/a、arrivati/e）
     * 非动词或词尾无法识别时返回 null
     */
    public static Map<String, Map<String, String>> buildConjugation(String word, String pos) {
        if (word == null || word.isBlank() || pos == null || !pos.startsWith("v.")) {
            return null;
        }
        String w = word.toLowerCase();
        boolean reflexive = w.endsWith("si");
        // 反身动词还原为不定式原形：alzarsi → alzare
        String infinitive = reflexive ? w.substring(0, w.length() - 2) + "e" : w;

        Map<String, String> present = buildPresent(infinitive, reflexive);
        if (present == null) {
            return null;
        }
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        result.put("present", present);
        putIfNotNull(result, "passatoProssimo", buildPassatoProssimo(infinitive, reflexive));
        putIfNotNull(result, "imperfetto", buildImperfetto(infinitive, reflexive));
        putIfNotNull(result, "futuro", buildFuturo(infinitive, reflexive));
        return result;
    }

    private static void putIfNotNull(Map<String, Map<String, String>> target, String key, Map<String, String> value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    /** 现在时：不规则表 → -isc 型 → 规则模板 */
    private static Map<String, String> buildPresent(String infinitive, boolean reflexive) {
        String[] forms = IRREGULAR_PRESENT.get(infinitive);
        if (forms == null) {
            if (!infinitive.endsWith("are") && !infinitive.endsWith("ere") && !infinitive.endsWith("ire")) {
                return null;
            }
            String stem = infinitive.substring(0, infinitive.length() - 3);
            String ending = infinitive.substring(infinitive.length() - 3);
            boolean isc = ending.equals("ire") && ISC_VERBS.contains(infinitive);
            forms = switch (ending) {
                // -are 按词干分三类：
                // -care/-gare 保硬音加 h（cercare→cerchi/cerchiamo）；
                // -iare 去 i（mangiare→mangi/mangiate、cambiare→cambi/cambiate）；
                // 其余规则（parlare→parli/parlate）
                case "are" -> {
                    if (infinitive.endsWith("care") || infinitive.endsWith("gare")) {
                        yield new String[]{stem + "o", stem + "hi", stem + "a", stem + "hiamo", stem + "ate", stem + "ano"};
                    }
                    if (infinitive.endsWith("iare")) {
                        String s = stem.substring(0, stem.length() - 1);
                        yield new String[]{s + "io", s + "i", s + "ia", s + "iamo", s + "iate", s + "iano"};
                    }
                    yield new String[]{stem + "o", stem + "i", stem + "a", stem + "iamo", stem + "ate", stem + "ano"};
                }
                case "ere" -> new String[]{stem + "o", stem + "i", stem + "e", stem + "iamo", stem + "ete", stem + "ono"};
                // -ire 分 -isc 型（capire→capisco）与普通型（dormire→dormo）
                default -> isc
                        ? new String[]{stem + "isco", stem + "isci", stem + "isce", stem + "iamo", stem + "ite", stem + "iscono"}
                        : new String[]{stem + "o", stem + "i", stem + "e", stem + "iamo", stem + "ite", stem + "ono"};
            };
        }
        return withPersons(forms, reflexive);
    }

    /**
     * 近过去时：avere/essere 助动词 + 过去分词
     * - 反身动词强制 essere（分词性数配合）
     * - essere 类分词带性数配合标注（arrivato/a、arrivati/e）
     * - 双助动词动词显示两种形式（ho/sono vissuto、abbiamo/siamo vissuti/e）
     */
    private static Map<String, String> buildPassatoProssimo(String infinitive, boolean reflexive) {
        String pp = pastParticiple(infinitive);
        if (pp == null) {
            return null;
        }
        boolean withEssere = reflexive || ESSERE_VERBS.contains(infinitive);
        boolean dual = !reflexive && DUAL_AUX_VERBS.contains(infinitive);
        String[] aux = withEssere
                ? new String[]{"sono", "sei", "è", "siamo", "siete", "sono"}
                : new String[]{"ho", "hai", "ha", "abbiamo", "avete", "hanno"};
        String[] dualAux = {"ho/sono", "hai/sei", "ha/è", "abbiamo/siamo", "avete/siete", "hanno/sono"};
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < PERSONS.length; i++) {
            String participle = pp;
            if ((withEssere || dual) && pp.endsWith("o")) {
                // 与主语性数配合：单数 -o/-a，复数 -i/-e（如 arrivato/a、arrivati/e）
                participle = i < 3 ? pp + "/a" : pp + "/i/e";
            }
            String form = (dual ? dualAux[i] : aux[i]) + " " + participle;
            if (reflexive) {
                form = REFLEXIVE_PRONOUNS[i] + " " + form;
            }
            result.put(PERSONS[i], form);
        }
        return result;
    }

    /** 未完成过去时：不规则表 → 规则后缀（-avo/-evo/-ivo） */
    private static Map<String, String> buildImperfetto(String infinitive, boolean reflexive) {
        String[] forms = IRREGULAR_IMPERFETTO.get(infinitive);
        if (forms == null) {
            if (!infinitive.endsWith("are") && !infinitive.endsWith("ere") && !infinitive.endsWith("ire")) {
                return null;
            }
            String stem = infinitive.substring(0, infinitive.length() - 3);
            String v = switch (infinitive.substring(infinitive.length() - 3)) {
                case "are" -> "av";
                case "ere" -> "ev";
                default -> "iv";
            };
            forms = new String[]{stem + v + "o", stem + v + "i", stem + v + "a",
                    stem + v + "amo", stem + v + "ate", stem + v + "ano"};
        }
        return withPersons(forms, reflexive);
    }

    /** 简单将来时：不规则词干表 → 规则（-are/-ere→erò、-ire→irò） */
    private static Map<String, String> buildFuturo(String infinitive, boolean reflexive) {
        String[] forms;
        String irregularStem = IRREGULAR_FUTURO_STEM.get(infinitive);
        if (irregularStem != null) {
            forms = conjugateFromStem(irregularStem);
        } else if (infinitive.endsWith("are") || infinitive.endsWith("ere") || infinitive.endsWith("ire")) {
            String stem = infinitive.substring(0, infinitive.length() - 3);
            String link;
            if (infinitive.endsWith("care") || infinitive.endsWith("gare")) {
                link = "her"; // 保硬音：giocare→giocherò、pagare→pagherò
            } else if (infinitive.endsWith("ciare") || infinitive.endsWith("giare")) {
                stem = stem.substring(0, stem.length() - 1); // 去 i：mangiare→mangerò、lasciare→lascerò
                link = "er";
            } else {
                link = infinitive.endsWith("ire") ? "ir" : "er"; // 普通 -iare 保留 i：cambiare→cambierò
            }
            forms = conjugateFromStem(stem + link);
        } else {
            return null;
        }
        return withPersons(forms, reflexive);
    }

    /** 将来时词干 → 六人称（sarò/sarai/sarà/saremo/sarete/saranno） */
    private static String[] conjugateFromStem(String stem) {
        return new String[]{stem + "ò", stem + "ai", stem + "à", stem + "emo", stem + "ete", stem + "anno"};
    }

    /** 过去分词：不规则表 → 规则（-are→ato / -ere→uto / -ire→ito） */
    private static String pastParticiple(String infinitive) {
        String pp = IRREGULAR_PP.get(infinitive);
        if (pp != null) {
            return pp;
        }
        if (!infinitive.endsWith("are") && !infinitive.endsWith("ere") && !infinitive.endsWith("ire")) {
            return null;
        }
        String stem = infinitive.substring(0, infinitive.length() - 3);
        return switch (infinitive.substring(infinitive.length() - 3)) {
            case "are" -> stem + "ato";
            case "ere" -> stem + "uto";
            default -> stem + "ito";
        };
    }

    /** 六人称数组 → 带人称键的 Map（反身动词加反身代词） */
    private static Map<String, String> withPersons(String[] forms, boolean reflexive) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < PERSONS.length; i++) {
            result.put(PERSONS[i], reflexive ? REFLEXIVE_PRONOUNS[i] + " " + forms[i] : forms[i]);
        }
        return result;
    }
}
