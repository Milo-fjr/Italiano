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
    }

    /** -isc 型 -ire 动词（第一人称 -isco：capire → capisco） */
    private static final Set<String> ISC_VERBS = Set.of(
            "capire", "finire", "preferire", "pulire", "spedire", "costruire");

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
    }

    /** 用 essere 作助动词的不及物动词（其余用 avere；反身动词一律 essere） */
    private static final Set<String> ESSERE_VERBS = Set.of(
            "essere", "stare", "andare", "venire", "partire", "uscire", "entrare",
            "arrivare", "tornare", "restare", "rimanere", "salire", "scendere",
            "nascere", "morire", "diventare", "succedere", "cadere", "piacere",
            "dispiacere", "sembrare", "apparire");

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
    }

    /** 不规则名词复数（重音在词尾音节的 -co/-go 等，规则推导会出错） */
    private static final Map<String, String> IRREGULAR_PLURAL = new LinkedHashMap<>();

    static {
        IRREGULAR_PLURAL.put("uomo", "uomini");
        IRREGULAR_PLURAL.put("dio", "dei");
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
    }

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

    /** 是否以元音开头 */
    private static boolean startsWithVowel(String w) {
        return "aeiou".indexOf(w.charAt(0)) >= 0;
    }

    /** 是否为需要 lo/gli 的特殊开头：s+辅音、z、gn、ps、x、y */
    private static boolean startsWithSpecial(String w) {
        if (w.startsWith("z") || w.startsWith("gn") || w.startsWith("ps")
                || w.startsWith("x") || w.startsWith("y")) {
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
     */
    public static String inferArticle(String word, String pos, String gender) {
        if (word == null || word.isBlank() || gender == null) {
            return null;
        }
        String w = word.toLowerCase();
        boolean male = "m".equals(gender);
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
        if (male) {
            return startsWithSpecial(w) ? "lo" : "il";
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
        Map<String, String> forms = new LinkedHashMap<>();
        if (w.endsWith("o")) {
            String stem = w.substring(0, w.length() - 1);
            forms.put("ms", stem + "o");
            forms.put("fs", stem + "a");
            forms.put("mp", stem + "i");
            forms.put("fp", stem + "e");
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
     * - 不规则表优先（uomo→uomini、parco→parchi 等）
     * - 重音结尾（città、caffè）→ 不变复数，返回原词
     * - -ca/-ga → -che/-ghe（amica→amiche）；-cia/-gia 按前一字母元音保留 i（camicia→camicie）或辅音去 i（arancia→arance）
     * - -o→-i、-a→-e、-e→-i
     * - 复数名词（pl.）与其他词尾返回 null
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
        // 重音结尾 → 不变复数
        if ("àèéìòù".indexOf(w.charAt(w.length() - 1)) >= 0) {
            return w;
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

    /** 由单数定冠词推导复数定冠词（详情展示用）；本身已是复数（i/gli/le）则原样返回 */
    public static String pluralArticle(String article, String gender) {
        if (article == null) {
            return null;
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
                case "are" -> new String[]{stem + "o", stem + "i", stem + "a", stem + "iamo", stem + "ate", stem + "ano"};
                case "ere" -> new String[]{stem + "o", stem + "i", stem + "e", stem + "iamo", stem + "ete", stem + "ono"};
                // -ire 分 -isc 型（capire→capisco）与普通型（dormire→dormo）
                default -> isc
                        ? new String[]{stem + "isco", stem + "isci", stem + "isce", stem + "iamo", stem + "ite", stem + "iscono"}
                        : new String[]{stem + "o", stem + "i", stem + "e", stem + "iamo", stem + "ite", stem + "ono"};
            };
        }
        return withPersons(forms, reflexive);
    }

    /** 近过去时：avere/essere 助动词 + 过去分词（essere 类分词带性数配合标注） */
    private static Map<String, String> buildPassatoProssimo(String infinitive, boolean reflexive) {
        String pp = pastParticiple(infinitive);
        if (pp == null) {
            return null;
        }
        boolean withEssere = reflexive || ESSERE_VERBS.contains(infinitive);
        String[] aux = withEssere
                ? new String[]{"sono", "sei", "è", "siamo", "siete", "sono"}
                : new String[]{"ho", "hai", "ha", "abbiamo", "avete", "hanno"};
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < PERSONS.length; i++) {
            String participle = pp;
            if (withEssere && pp.endsWith("o")) {
                // 与主语性数配合：单数 -o/-a，复数 -i/-e（如 arrivato/a、arrivati/e）
                participle = i < 3 ? pp + "/a" : pp + "/i/e";
            }
            String form = aux[i] + " " + participle;
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
            String link = infinitive.endsWith("ire") ? "ir" : "er";
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
