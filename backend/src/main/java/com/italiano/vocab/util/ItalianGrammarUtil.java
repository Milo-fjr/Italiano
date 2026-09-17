package com.italiano.vocab.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 意大利语语法工具：名词冠词/复数推断、形容词性数变化、动词四时态变位生成
 * （导入时预填，均支持手动编辑修正）
 */
public final class ItalianGrammarUtil {

    private ItalianGrammarUtil() {
    }

    /** 变位表人称顺序（键与变位 JSON、加练模式判分共用） */
    public static final String[] PERSONS = {"io", "tu", "lui/lei", "noi", "voi", "loro"};

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

    /**
     * 双助动词动词（avere 及物 / essere 不及物，近过去时两种形式均合法）：显示为 ho/sono vissuto。
     * 注意：camminare、nuotare 是"动作方式"动词（不表去向），只用 avere，绝不可加回此表
     * （ho camminato / ho nuotato，无 essere 形式、分词不变性数）。曾误加导致数据错误，已移除。
     */
    private static final Set<String> DUAL_AUX_VERBS = Set.of(
            "correre", "vivere", "volare", "crescere",
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

    /** 月份：复数不变且性别统一为阳性（il gennaio...），无记忆价值，不标「性别需记」 */
    private static final Set<String> MONTHS = Set.of(
            "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
            "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre");

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
     * 冠词式变化形容词（bello 型）：作定语（名词前）时形式随后续单词变化，同定冠词规则：
     * bel libro / bello studente / bell'amico / bei libri / begli studenti。
     * 表中存四格的定语形式（多形式用 " / " 分隔）；谓语位置仍用常规四式（bello/bella/belli/belle）。
     */
    private static final Map<String, Map<String, String>> BELLO_TYPE = Map.of(
            "bello", Map.of("ms", "bel / bello / bell'", "fs", "bella / bell'", "mp", "bei / begli", "fp", "belle"),
            "buono", Map.of("ms", "buon / buono", "fs", "buona / buon'", "mp", "buoni", "fp", "buone"),
            "quello", Map.of("ms", "quel / quello / quell'", "fs", "quella / quell'", "mp", "quei / quegli", "fp", "quelle"));

    /**
     * bello 型定语加练语境（名词/中文/性别/是否复数），覆盖定语形式全部考点：
     * 一般辅音（bel/buon）、特殊开头（bello/buono）、元音省音（bell'/buon）、
     * 复数一般（bei/quei）、复数特殊（begli/quegli）、复数元音（begli/quegli）、阴性省音（bell'/buon'）。
     * 语境名词取已学高频词，复数直接给复数形（libri/studenti/amici）。
     */
    public static final String[][] BELLO_PRACTICE = {
            {"libro", "书", "m", ""},
            {"studente", "学生", "m", ""},
            {"amico", "朋友", "m", ""},
            {"libri", "书", "m", "pl"},
            {"studenti", "学生", "m", "pl"},
            {"amici", "朋友", "m", "pl"},
            {"amica", "朋友", "f", ""},
    };

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
     * 名词复数拼写陷阱（-ca/-ga 加 h：banca→banche、-cia/-gia 去/留 i：arancia→arance、
     * camicia→camicie）。属规则拼写（曾打「音变」标签已删），拼写模式附加题下线后暂无调用方，
     * 保留作语法判定；此类词的复数由规则推导，不进不规则复数加练。
     */
    public static boolean isPluralTrapNoun(String word) {
        if (word == null || word.isBlank()) {
            return false;
        }
        String w = word.toLowerCase();
        return w.endsWith("ca") || w.endsWith("ga") || w.endsWith("cia") || w.endsWith("gia");
    }

    /**
     * 判定单词的语法形式是否不规则（供卡片加标记，常规词返回 null 不标记）：
     * - 动词（含反身动词，剥 -si 还原不定式）：命中任一例外表
     *   （现在时 / -isc 型 / 过去分词 / 未完成过去时 / 将来时词干）→「不规则变位」；
     *   未命中例外表但属拼写音变类（-care/-gare/-iare）不标记——io 形式规则，无记忆价值
     * - 名词：不规则复数表 →「不规则复数」；
     *   不变复数（外来词/缩写词/月份）不标记——性质即规则，无逐个记忆价值；
     *   不可数名词无复数不标记；-ca/-ga/-cia/-gia 词尾复数音变不标记（拼写有规律，
     *   附加题判分另走 isPluralTrapNoun）；
     *   词尾与性别反常（-o 却阴性 / -a 却阳性）→「阴阳性特殊」；
     *   -e 结尾名词性别无法从词尾判断 →「性别需记」
     * - 形容词：加 h / 不加 h / 不变形容词例外表 →「不规则变化」
     * 名词复数特性与性别标签均可叠加（顿号连接）：mano「不规则复数、阴阳性特殊」；
     * 月份统一阳性不标性别，作为唯一例外
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
            // 逐表判定，标注具体不规则的是哪个时态（多个坑用顿号拼接，与详情页时态名一致）
            List<String> irregulars = new ArrayList<>();
            if (IRREGULAR_PRESENT.containsKey(infinitive)) {
                irregulars.add("现在时不规则");
            }
            if (ISC_VERBS.contains(infinitive)) {
                irregulars.add("现在时不规则"); // -isc 型：capisco 而非 capo
            }
            if (IRREGULAR_PP.containsKey(infinitive)) {
                irregulars.add("近过去时不规则");
            }
            if (IRREGULAR_IMPERFETTO.containsKey(infinitive)) {
                irregulars.add("未完成时不规则");
            }
            if (IRREGULAR_FUTURO_STEM.containsKey(infinitive)) {
                irregulars.add("将来时不规则");
            }
            if (!irregulars.isEmpty()) {
                return String.join("、", irregulars.stream().distinct().toList());
            }
            // 拼写音变类（-care/-gare 加 h、-ciare/-giare 去 i、-iare 避免双 i）不再打标签：
            // 卡片上冗余信息，且 io 形式均为规则变化（cerco/pago/studio），无附加题价值
            return null;
        }
        // 名词：复数特性与性别标签可叠加（顿号连接）
        if (isNounPos(pos)) {
            List<String> tags = new ArrayList<>();
            if (IRREGULAR_PLURAL.containsKey(w)) {
                // 加 h 型 -co/-go（复数以 -chi/-ghi 结尾，重音在词干，如 lago→laghi/fuoco→fuochi）
                // 与 -ca/-ga 同规则可推导，不标红（2026-09-18 用户指出，见 AGENTS 陷阱 15）；
                // 仅软音型（amico→amici，重音位置文本不可判）与强不规则（-a 复数等）保留标签
                String plural = IRREGULAR_PLURAL.get(w);
                if (!plural.endsWith("chi") && !plural.endsWith("ghi")) {
                    tags.add("不规则复数");
                }
            }
            // 不变复数（外来词/缩写词/月份）不再打「复数不变」标签：性质即规则（外来词、缩写
            // 词复数不变），无需逐个标红提醒；仅真正不规则的复数（IRREGULAR_PLURAL）保留标签
            if (gender != null && ((w.endsWith("o") && "f".equals(gender))
                    || (w.endsWith("a") && "m".equals(gender)))) {
                tags.add("阴阳性特殊");
            } else if (w.endsWith("e") && gender != null && !MONTHS.contains(w)) {
                // -e 结尾性别无法从词尾判断（il mare ♂ / la notte ♀），需连同冠词记忆，
                // 与复数特性叠加（mouse「复数不变、性别需记」）；月份统一阳性不标
                tags.add("性别需记");
            }
            if (!tags.isEmpty()) {
                return String.join("、", tags);
            }
            // 不可数名词无复数形式，无音变陷阱可言（-e 结尾性别已在上方统一处理）
            if (UNCOUNTABLE_NOUNS.contains(w)) {
                return null;
            }
            // -ca/-ga 复数加 h、-cia/-gia 复数去/留 i 属拼写陷阱，但不再打「音变」标签
            //（卡片冗余）；附加题判分由 ExtraFormService 调 isPluralTrapNoun 保留
            return null;
        }
        // 形容词（含混合词性 agg./s.m. 等，词在形容词例外表即标记）
        if (pos.contains("agg.")) {
            // bello 型（含 pron./agg. 的 quello）：定语形式随后续单词变化，同定冠词规则
            if (BELLO_TYPE.containsKey(w)) {
                return "冠词式变化";
            }
            if (ADJ_HARD.contains(w) || ADJ_SOFT.contains(w) || ADJ_INVARIANT.contains(w)) {
                return "不规则变化";
            }
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

    /** 是否名词类词性（含混合词性如 agg./s.m.；排除复数名词）；拼写模式附加题判定复用 */
    public static boolean isNounPos(String pos) {
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
        // bello 型：定语形式随后续单词变化（bel/bello/bell'、bei/begli），非规则四式
        Map<String, String> belloForms = BELLO_TYPE.get(w);
        if (belloForms != null) {
            return new LinkedHashMap<>(belloForms);
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
     * bello 型定语形式加练判分：给定语境名词推导该形容词唯一的定语形式
     * （bel libro / bello studente / bell'amico / bei libri / begli studenti / begli amici）。
     * 形容词不在 bello 型表内返回 null。
     */
    public static String belloAttributive(String adjective, String contextNoun, String gender, boolean plural) {
        Map<String, String> forms = BELLO_TYPE.get(adjective == null ? "" : adjective.toLowerCase());
        if (forms == null) {
            return null;
        }
        String key = plural ? ("m".equals(gender) ? "mp" : "fp") : ("m".equals(gender) ? "ms" : "fs");
        String[] options = forms.get(key).split(" / ");
        if (options.length == 1) {
            return options[0];
        }
        boolean vowel = startsWithVowel(contextNoun);
        boolean special = startsWithSpecial(contextNoun);
        // 省音式（末尾 '）仅用于元音开头：bell'amico / bell'amica
        if (options[options.length - 1].endsWith("'") && vowel) {
            return options[options.length - 1];
        }
        // 两式且均非省音 [一般, 特殊]：单数元音用一般式省音（buon amico），复数元音用特殊式（begli amici）
        if (options.length == 2 && !options[1].endsWith("'")) {
            return (vowel && plural) || special ? options[1] : options[0];
        }
        return special ? options[1] : options[0];
    }

    /**
     * 不变形容词加练考点：性数不变（blu/rosa/viola/gratis/arancione），复数 = 原词。
     * ogni/qualche/nessuno 本身无复数形式，不考，返回 false。
     */
    public static boolean isInvariantAdjective(String word) {
        if (word == null) {
            return false;
        }
        String w = word.toLowerCase();
        return ADJ_INVARIANT.contains(w)
                && !"ogni".equals(w) && !"qualche".equals(w) && !"nessuno".equals(w);
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

    /** 现在时：不规则（例外表 ∪ -isc 型）→ 规则模板 */
    private static Map<String, String> buildPresent(String infinitive, boolean reflexive) {
        String[] forms = irregularPresent(infinitive);
        if (forms == null) {
            forms = regularPresent(infinitive);
        }
        if (forms == null) {
            return null;
        }
        return withPersons(forms, reflexive);
    }

    /**
     * 不规则现在时六人称（例外表 ∪ -isc 型推导；-isc 型 capisco 无法由通用规则得出，视为不规则）。
     * 规则动词返回 null。加练模式「不规则变位」抽题/筛考点用。
     */
    public static String[] irregularPresent(String infinitive) {
        String[] forms = IRREGULAR_PRESENT.get(infinitive);
        if (forms != null) {
            return forms;
        }
        if (infinitive.endsWith("ire") && ISC_VERBS.contains(infinitive)) {
            String stem = infinitive.substring(0, infinitive.length() - 3);
            return new String[]{stem + "isco", stem + "isci", stem + "isce",
                    stem + "iamo", stem + "ite", stem + "iscono"};
        }
        return null;
    }

    /**
     * 规则现在时六人称（含 -care/-gare 加 h、-iare 去 i 拼写规则；不查例外表）。
     * 与 irregularPresent 逐人称对比：形式相同的人称是规则形式，不进加练考点
     * （prendere/mettere 整表现在时与规则推导一致、andare 的 noi/voi 均被自然过滤）。
     */
    public static String[] regularPresent(String infinitive) {
        if (!infinitive.endsWith("are") && !infinitive.endsWith("ere") && !infinitive.endsWith("ire")) {
            return null;
        }
        String stem = infinitive.substring(0, infinitive.length() - 3);
        return switch (infinitive.substring(infinitive.length() - 3)) {
            // -care/-gare 保硬音加 h（cercare→cerchi/cerchiamo）；-iare 去 i（mangiare→mangi）；
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
            default -> new String[]{stem + "o", stem + "i", stem + "e", stem + "iamo", stem + "ite", stem + "ono"};
        };
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
        String[] forms = irregularFuturo(infinitive);
        if (forms == null) {
            forms = regularFuturo(infinitive);
        }
        if (forms == null) {
            return null;
        }
        return withPersons(forms, reflexive);
    }

    /** 不规则将来时六人称（例外词干表推导）；规则动词返回 null */
    public static String[] irregularFuturo(String infinitive) {
        String stem = IRREGULAR_FUTURO_STEM.get(infinitive);
        return stem == null ? null : conjugateFromStem(stem);
    }

    /**
     * 规则将来时六人称（含 -care/-gare→her、-ciare/-giare 去 i 拼写规则；不查例外表）。
     * 与 irregularFuturo 逐人称对比筛考点，同 regularPresent。
     */
    public static String[] regularFuturo(String infinitive) {
        if (!infinitive.endsWith("are") && !infinitive.endsWith("ere") && !infinitive.endsWith("ire")) {
            return null;
        }
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
        return conjugateFromStem(stem + link);
    }

    /** 将来时词干 → 六人称（sarò/sarai/sarà/saremo/sarete/saranno） */
    private static String[] conjugateFromStem(String stem) {
        return new String[]{stem + "ò", stem + "ai", stem + "à", stem + "emo", stem + "ete", stem + "anno"};
    }

    /**
     * 过去分词：不规则表 → 规则（-are→ato / -ere→uto / -ire→ito）。
     * 加练模式近过去考裸分词（避开 ho/sono 助动词歧义）。
     */
    public static String pastParticiple(String infinitive) {
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
