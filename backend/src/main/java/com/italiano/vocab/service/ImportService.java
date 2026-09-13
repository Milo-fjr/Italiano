package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.italiano.vocab.entity.Word;
import com.italiano.vocab.mapper.WordMapper;
import com.italiano.vocab.util.ItalianGrammarUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 词库导入：从 classpath:data/vocab_data.json 导入（幂等，按单词文本判重）。
 * 首次启动若 word 表为空自动导入；导入时预填性别、定冠词、动词变位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService implements ApplicationRunner {

    private static final String DATA_PATH = "data/vocab_data.json";

    private final WordMapper wordMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // 老库升级：schema.sql 的 CREATE TABLE IF NOT EXISTS 不会为已有表补新列，这里显式补列（幂等）
        ensureColumn("word", "adj_forms", "TEXT NULL COMMENT '形容词性数变化 JSON {ms,fs,mp,fp}'");
        ensureColumn("word", "plural", "VARCHAR(64) NULL COMMENT '名词复数形式'");
        // 拼写模式三列（独立拼写盒子 + 防撞数据源）
        ensureColumn("word_progress", "spell_box", "INT NOT NULL DEFAULT 0 COMMENT '拼写盒子级别0-5（独立于认识盒子）'");
        ensureColumn("word_progress", "spell_next_review_at", "DATE NULL COMMENT '下次拼写复习日期（NULL=从未拼过，视为到期）'");
        ensureColumn("word_progress", "last_quiz_at", "DATE NULL COMMENT '最近一次认识测验答题日期（拼写防撞用）'");
        // 存量数据回填：仅为空值的词生成形容词变化/名词复数（不覆盖用户已编辑内容）
        backfillGrammarFields();
        // 变位结构升级（旧扁平 JSON → 四时态嵌套）+ 复数规则修正（含混合词性补齐），幂等
        migrateConjugationV2();
        fixPluralV2();
        // 双助动词动词近过去时：单形式 → ho/sono 双形式，幂等
        fixDualAuxV3();
        // 全表排查修正（V4）：系统性拼写错误一次性重建，触发后不再重复
        fixGrammarV4();
        // 全表排查修正（V5）：二轮深查——不可数名词清空复数、-ma→-mi、-io 形容词、
        // 不变形容词清空、sciare 将来时、双助动词扩充、双性别名词冠词，幂等
        fixGrammarV5();
        // 双助动词纠错（V6）：camminare/nuotare 实为"动作方式"动词只用 avere，回退 V3 误加的双形式，幂等
        fixDualAuxV6();

        Long count = wordMapper.selectCount(null);
        if (count == null || count == 0) {
            log.info("词库为空，开始自动导入 {}", DATA_PATH);
            try {
                int[] r = importFromJson();
                log.info("自动导入完成：新增 {} 个，跳过 {} 个", r[0], r[1]);
            } catch (Exception e) {
                log.error("自动导入失败，可通过 POST /api/import 手动导入：{}", e.getMessage());
            }
        }
    }

    /** 若表缺少指定列则 ALTER 补列（MySQL 8 不支持 ADD COLUMN IF NOT EXISTS） */
    private void ensureColumn(String table, String column, String ddl) {
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (exists == null || exists == 0) {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + ddl);
            log.info("已为表 {} 补充列 {}", table, column);
        }
    }

    /** 回填形容词变化与名词复数（仅处理字段为空的记录） */
    private void backfillGrammarFields() {
        int adj = 0;
        int plural = 0;
        for (Word w : wordMapper.selectList(new LambdaQueryWrapper<Word>()
                .and(q -> q.isNull(Word::getAdjForms).or().isNull(Word::getPlural)))) {
            boolean changed = false;
            if (w.getAdjForms() == null && w.getPos() != null && w.getPos().contains("agg.")) {
                Map<String, String> forms = ItalianGrammarUtil.buildAdjectiveForms(w.getWord(), w.getPos());
                if (forms != null) {
                    try {
                        w.setAdjForms(objectMapper.writeValueAsString(forms));
                        changed = true;
                        adj++;
                    } catch (Exception ignored) {
                    }
                }
            }
            if (w.getPlural() == null && w.getPos() != null && w.getPos().startsWith("s.")) {
                String p = ItalianGrammarUtil.buildPlural(w.getWord(), w.getPos());
                if (p != null) {
                    w.setPlural(p);
                    changed = true;
                    plural++;
                }
            }
            if (changed) {
                wordMapper.updateById(w);
            }
        }
        if (adj > 0 || plural > 0) {
            log.info("语法字段回填完成：形容词变化 {} 个，名词复数 {} 个", adj, plural);
        }
    }

    /**
     * 变位结构升级：旧扁平 JSON（顶层含 "io"）→ 四时态嵌套结构。
     * 新结构（顶层含 "present"）跳过，已编辑数据不受影响（幂等）。
     */
    private void migrateConjugationV2() {
        int migrated = 0;
        for (Word w : wordMapper.selectList(new LambdaQueryWrapper<Word>().isNotNull(Word::getConjugation))) {
            try {
                JsonNode node = objectMapper.readTree(w.getConjugation());
                if (node.has("present")) {
                    continue; // 已是新结构
                }
                Map<String, Map<String, String>> c = ItalianGrammarUtil.buildConjugation(w.getWord(), w.getPos());
                if (c != null) {
                    w.setConjugation(objectMapper.writeValueAsString(c));
                    wordMapper.updateById(w);
                    migrated++;
                }
            } catch (Exception ignored) {
            }
        }
        if (migrated > 0) {
            log.info("变位结构升级完成：{} 个动词已生成四时态（现在时/近过去时/未完成过去时/简单将来时）", migrated);
        }
    }

    /**
     * 复数规则修正：v1 规则（仅 -o/-a/-e 词尾）产生的错误值 → v2 规则（含 -ca/-ga/-cia/-gia、
     * 不规则表、不变复数）。仅当当前值能被 v1 规则复现（即机器生成值）或为空时才修正，
     * 用户手动编辑过的值保持不变（幂等）。
     */
    private void fixPluralV2() {
        int fixed = 0;
        for (Word w : wordMapper.selectList(null)) {
            if (w.getPos() == null || !(w.getPos().contains("s.m.") || w.getPos().contains("s.f."))) {
                continue;
            }
            String fresh = ItalianGrammarUtil.buildPlural(w.getWord(), w.getPos());
            if (java.util.Objects.equals(fresh, w.getPlural())) {
                continue;
            }
            boolean machineGenerated = w.getPlural() == null || w.getPlural().equals(oldRulePlural(w.getWord()));
            if (machineGenerated) {
                w.setPlural(fresh);
                wordMapper.updateById(w);
                fixed++;
            }
        }
        if (fixed > 0) {
            log.info("名词复数规则修正完成：{} 个（含混合词性补齐与错误复数修复）", fixed);
        }
    }

    /** v1 复数规则（仅用于判断存量值是否为机器生成）：-o→-i / -a→-e / -e→-i */
    private String oldRulePlural(String word) {
        String w = word.toLowerCase();
        if (w.endsWith("o") || w.endsWith("e")) {
            return w.substring(0, w.length() - 1) + "i";
        }
        if (w.endsWith("a")) {
            return w.substring(0, w.length() - 1) + "e";
        }
        return null;
    }

    /**
     * 全表排查修正（V4）：旧规则引擎存在系统性拼写错误，一次性用新引擎重建全部语法字段——
     * -io 名词复数（viaggii→viaggi）、-care/-gare/-iare 动词拼写（gioci→giochi、mangii→mangi、
     * manciiamo→manchiamo）、将来时（cercerò→cercherò、mangierò→mangerò）、过去分词
     * （pianguto→pianto、deciduto→deciso）、-co/-go 形容词（antici→antichi）、
     * 月份/外来词不变复数、-ista 双性别复数（autisti/autiste）、不变形容词清空（rosa/viola）。
     * <p>
     * 触发标记：ferie 的复数仍为旧引擎错误值 ferii（updateById 无法置 null 的遗留）；
     * 修复后标记消失，后续启动不再触发，用户手动编辑的值不会被覆盖。
     */
    private void fixGrammarV4() {
        Long legacy = wordMapper.selectCount(new LambdaQueryWrapper<Word>()
                .eq(Word::getWord, "ferie").eq(Word::getPlural, "ferii"));
        if (legacy == null || legacy == 0) {
            return;
        }
        int fixed = 0;
        for (Word w : wordMapper.selectList(null)) {
            try {
                // 用 UpdateWrapper 显式 set（含 null），updateById 会跳过 null 字段导致无法清空
                LambdaUpdateWrapper<Word> uw = new LambdaUpdateWrapper<Word>().eq(Word::getId, w.getId());
                boolean changed = false;
                String newPlural = ItalianGrammarUtil.buildPlural(w.getWord(), w.getPos());
                if (!java.util.Objects.equals(newPlural, w.getPlural())) {
                    uw.set(Word::getPlural, newPlural);
                    changed = true;
                }
                Map<String, String> newAdj = ItalianGrammarUtil.buildAdjectiveForms(w.getWord(), w.getPos());
                String newAdjJson = newAdj == null ? null : objectMapper.writeValueAsString(newAdj);
                if (!java.util.Objects.equals(newAdjJson, w.getAdjForms())) {
                    uw.set(Word::getAdjForms, newAdjJson);
                    changed = true;
                }
                Map<String, Map<String, String>> newConj = ItalianGrammarUtil.buildConjugation(w.getWord(), w.getPos());
                String newConjJson = newConj == null ? null : objectMapper.writeValueAsString(newConj);
                if (!java.util.Objects.equals(newConjJson, w.getConjugation())) {
                    uw.set(Word::getConjugation, newConjJson);
                    changed = true;
                }
                if (changed) {
                    wordMapper.update(null, uw);
                    fixed++;
                }
            } catch (Exception ignored) {
            }
        }
        log.info("全表语法修正（V4）完成：重建 {} 个单词的语法字段", fixed);
    }

    /**
     * 全表排查修正（V5）：二轮深查——
     * - 不可数名词（fame/sete/sangue/latte 等按词库词义）清空错误复数
     * - 希腊词源 -ma 名词：probleme→problemi、diplome→diplomi、clime→climi
     * - 不变名词补充：cinema/garage/mouse/video/euro 复数=原词
     * - 例外表补充：pigiama→pigiami、pilota→piloti/pilote、lenzuolo→lenzuola
     * - 形容词 -io 复数：doppii→doppi、grigii→grigi、vecchii→vecchi 等 6 个
     * - poco 硬音复数：poci→pochi；qualche/nessuno/arancione 清空（不变形容词）
     * - sciare 将来时：scerò→scierò（保留 i 维持 /ʃ/ 音）
     * - 双助动词扩充：passare/cambiare/finire 等 7 个近过去时 ho/sono 双形式
     * - 双性别名词冠词补齐：il/la turista、l'autista（article 为空的记录）
     * - 词性数据修正：infermiere/parrucchiere 实为阳性（阴性形式是另一个词）
     * <p>
     * 触发标记：vecchio 的形容词阳性复数仍为旧规则错误值 vecchii；修复后标记消失。
     */
    private void fixGrammarV5() {
        Word vecchio = wordMapper.selectOne(new LambdaQueryWrapper<Word>().eq(Word::getWord, "vecchio"));
        if (vecchio == null || vecchio.getAdjForms() == null
                || !vecchio.getAdjForms().contains("\"mp\":\"vecchii\"")) {
            return;
        }
        // 词性数据修正：infermiere/parrucchiere 为阳性名词（阴性形式 infermiera/parrucchiera 未收录）
        for (String wn : List.of("infermiere", "parrucchiere")) {
            Word t = wordMapper.selectOne(new LambdaQueryWrapper<Word>().eq(Word::getWord, wn));
            if (t != null && t.getPos() != null && t.getPos().contains("/s.f.")) {
                t.setPos(t.getPos().replace("/s.f.", ""));
                wordMapper.updateById(t);
            }
        }
        int fixed = 0;
        for (Word w : wordMapper.selectList(null)) {
            try {
                // UpdateWrapper 显式 set（含 null），updateById 会跳过 null 字段导致无法清空
                LambdaUpdateWrapper<Word> uw = new LambdaUpdateWrapper<Word>().eq(Word::getId, w.getId());
                boolean changed = false;
                String newPlural = ItalianGrammarUtil.buildPlural(w.getWord(), w.getPos());
                if (!java.util.Objects.equals(newPlural, w.getPlural())) {
                    uw.set(Word::getPlural, newPlural);
                    changed = true;
                }
                Map<String, String> newAdj = ItalianGrammarUtil.buildAdjectiveForms(w.getWord(), w.getPos());
                String newAdjJson = newAdj == null ? null : objectMapper.writeValueAsString(newAdj);
                if (!java.util.Objects.equals(newAdjJson, w.getAdjForms())) {
                    uw.set(Word::getAdjForms, newAdjJson);
                    changed = true;
                }
                Map<String, Map<String, String>> newConj = ItalianGrammarUtil.buildConjugation(w.getWord(), w.getPos());
                String newConjJson = newConj == null ? null : objectMapper.writeValueAsString(newConj);
                if (!java.util.Objects.equals(newConjJson, w.getConjugation())) {
                    uw.set(Word::getConjugation, newConjJson);
                    changed = true;
                }
                // 冠词仅为空时补推导（双性别名词 il/la turista 等；非空的不覆盖）
                if (w.getArticle() == null) {
                    String article = ItalianGrammarUtil.inferArticle(w.getWord(), w.getPos(),
                            ItalianGrammarUtil.inferGender(w.getPos()));
                    if (article != null) {
                        uw.set(Word::getArticle, article);
                        changed = true;
                    }
                }
                if (changed) {
                    wordMapper.update(null, uw);
                    fixed++;
                }
            } catch (Exception ignored) {
            }
        }
        log.info("全表语法修正（V5）完成：重建 {} 个单词的语法字段", fixed);
    }

    /**
     * 双助动词动词（correre/vivere/volare 等）近过去时升级：旧单助形式 → ho/sono 双形式。
     * 仅当近过去时 io 形式不含 "/"（即机器生成的旧单形式）时重建，用户手动编辑过的不动（幂等）。
     * 注意：不含 camminare、nuotare——它们是"动作方式"动词只用 avere，误加已由 fixDualAuxV6 纠错。
     */
    private void fixDualAuxV3() {
        int fixed = 0;
        for (String verb : List.of("correre", "vivere", "volare")) {
            Word w = wordMapper.selectOne(new LambdaQueryWrapper<Word>().eq(Word::getWord, verb));
            if (w == null || w.getConjugation() == null) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(w.getConjugation());
                String io = node.path("passatoProssimo").path("io").asText("");
                if (io.contains("/")) {
                    continue; // 已是双形式
                }
                Map<String, Map<String, String>> c = ItalianGrammarUtil.buildConjugation(w.getWord(), w.getPos());
                if (c != null) {
                    w.setConjugation(objectMapper.writeValueAsString(c));
                    wordMapper.updateById(w);
                    fixed++;
                }
            } catch (Exception ignored) {
            }
        }
        if (fixed > 0) {
            log.info("双助动词近过去时升级完成：{} 个（ho/sono 双形式）", fixed);
        }
    }

    /**
     * 双助动词纠错（V6）：camminare、nuotare 是"动作方式"动词（不表去向），只用 avere
     * （ho camminato / ho nuotato，无 essere 形式、分词不变性数）。V3 曾误把它们列入双助动词，
     * 此处回退为单助动词。幂等：仅当近过去时 io 形含 "/"（双形式）才重建。
     */
    private void fixDualAuxV6() {
        int fixed = 0;
        for (String verb : List.of("camminare", "nuotare")) {
            Word w = wordMapper.selectOne(new LambdaQueryWrapper<Word>().eq(Word::getWord, verb));
            if (w == null || w.getConjugation() == null) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(w.getConjugation());
                String io = node.path("passatoProssimo").path("io").asText("");
                if (!io.contains("/")) {
                    continue; // 已是单助动词（avere）
                }
                Map<String, Map<String, String>> c = ItalianGrammarUtil.buildConjugation(w.getWord(), w.getPos());
                if (c != null) {
                    w.setConjugation(objectMapper.writeValueAsString(c));
                    wordMapper.updateById(w);
                    fixed++;
                }
            } catch (Exception ignored) {
            }
        }
        if (fixed > 0) {
            log.info("双助动词纠错（V6）完成：{} 个（camminare/nuotare 回退为 avere）", fixed);
        }
    }

    /**
     * 幂等导入词库，返回 [新增数, 跳过数]。
     * 导入时按词性预填：名词性别与定冠词、动词四时态变位、形容词变化、名词复数。
     */
    @Transactional
    public int[] importFromJson() {
        JsonNode root;
        try {
            root = objectMapper.readTree(new ClassPathResource(DATA_PATH).getInputStream());
        } catch (Exception e) {
            throw new IllegalStateException("读取词库文件失败：" + DATA_PATH + "，" + e.getMessage(), e);
        }

        // 已存在的单词一次性载入判重（幂等）
        Set<String> existing = new HashSet<>();
        wordMapper.selectList(new LambdaQueryWrapper<Word>().select(Word::getWord))
                .forEach(w -> existing.add(w.getWord()));

        int inserted = 0;
        int skipped = 0;
        List<Word> buffer = new ArrayList<>();
        for (JsonNode node : root.path("words")) {
            String text = node.path("word").asText(null);
            if (text == null || text.isBlank()) {
                continue;
            }
            if (existing.contains(text)) {
                skipped++;
                continue;
            }
            Word w = new Word();
            w.setWord(text);
            w.setPos(node.path("pos").asText(null));
            w.setMeaning(node.path("meaning").asText(null));
            w.setCategory(node.path("category").asText(null));

            // 语法预填：性别、定冠词、变位、形容词变化、名词复数（均可后续手动编辑修正）
            String gender = ItalianGrammarUtil.inferGender(w.getPos());
            w.setGender(gender);
            w.setArticle(ItalianGrammarUtil.inferArticle(text, w.getPos(), gender));
            Map<String, Map<String, String>> conjugation = ItalianGrammarUtil.buildConjugation(text, w.getPos());
            if (conjugation != null) {
                try {
                    w.setConjugation(objectMapper.writeValueAsString(conjugation));
                } catch (Exception ignored) {
                }
            }
            Map<String, String> adjForms = ItalianGrammarUtil.buildAdjectiveForms(text, w.getPos());
            if (adjForms != null) {
                try {
                    w.setAdjForms(objectMapper.writeValueAsString(adjForms));
                } catch (Exception ignored) {
                }
            }
            w.setPlural(ItalianGrammarUtil.buildPlural(text, w.getPos()));

            // 备份恢复：导出文件带完整语法字段/例句时优先采用（全保真回灌），
            // 旧的精简种子无这些字段则保持上方推导结果
            if (node.hasNonNull("gender")) {
                w.setGender(node.path("gender").asText());
            }
            if (node.hasNonNull("article")) {
                w.setArticle(node.path("article").asText());
            }
            if (node.hasNonNull("plural")) {
                w.setPlural(node.path("plural").asText());
            }
            JsonNode conj = node.path("conjugation");
            if (conj.isObject() && !conj.isEmpty()) {
                w.setConjugation(conj.toString());
            }
            JsonNode adj = node.path("adjForms");
            if (adj.isObject() && !adj.isEmpty()) {
                w.setAdjForms(adj.toString());
            }
            JsonNode ex = node.path("example");
            if (ex.isObject() && !ex.isEmpty()) {
                w.setExample(ex.toString());
            }
            w.setCreatedAt(LocalDateTime.now());

            buffer.add(w);
            existing.add(text);
            inserted++;
            if (buffer.size() >= 200) {
                buffer.forEach(wordMapper::insert);
                buffer.clear();
            }
        }
        buffer.forEach(wordMapper::insert);
        return new int[]{inserted, skipped};
    }

    /**
     * 导出词库到 vocab_data.json（覆盖种子文件，git 可追踪变更）。
     * 含全部语法字段（性别/冠词/复数/变位/形容词变化）与例句，
     * 配合导入端的「JSON 值优先」实现完整备份闭环。
     */
    public Map<String, Object> exportToJson() {
        List<Word> words = wordMapper.selectList(new LambdaQueryWrapper<Word>().orderByAsc(Word::getId));

        // 分类统计（保持 DB 顺序去重）
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (Word w : words) {
            categoryCounts.merge(w.getCategory() == null ? "未分类" : w.getCategory(), 1L, Long::sum);
        }

        List<Map<String, Object>> wordList = new ArrayList<>();
        for (Word w : words) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId());
            m.put("word", w.getWord());
            m.put("pos", w.getPos());
            m.put("meaning", w.getMeaning());
            m.put("category", w.getCategory());
            if (w.getGender() != null) {
                m.put("gender", w.getGender());
            }
            if (w.getArticle() != null) {
                m.put("article", w.getArticle());
            }
            if (w.getPlural() != null) {
                m.put("plural", w.getPlural());
            }
            putJsonField(m, "conjugation", w.getConjugation());
            putJsonField(m, "adjForms", w.getAdjForms());
            putJsonField(m, "example", w.getExample());
            wordList.add(m);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("name", "意大利语A2核心词库");
        meta.put("description", "马可波罗计划生意大利语A2学习用词库（含手动编辑的语法字段与例句）");
        meta.put("total", words.size());
        meta.put("category_count", categoryCounts.size());
        meta.put("category_counts", categoryCounts);
        meta.put("exported_at", LocalDate.now().toString());
        meta.put("source", "database export");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("meta", meta);
        root.put("categories", new ArrayList<>(categoryCounts.keySet()));
        root.put("words", wordList);

        Path target = resolveSeedPath();
        try {
            Files.createDirectories(target.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), root);
        } catch (IOException e) {
            throw new IllegalStateException("导出失败：" + e.getMessage(), e);
        }
        log.info("词库导出完成：{} 个单词 -> {}", words.size(), target);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", words.size());
        result.put("path", target.toString());
        return result;
    }

    /** 语法 JSON 字符串 -> 嵌套对象写入导出结构（无效/空值跳过） */
    private void putJsonField(Map<String, Object> m, String key, String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isObject() && !node.isEmpty()) {
                m.put(key, node);
            }
        } catch (Exception ignored) {
        }
    }

    /** 种子文件路径：优先 backend/ 运行目录（git 可见的源文件），退回仓库根相对路径 */
    private Path resolveSeedPath() {
        Path p = Paths.get("src/main/resources/data/vocab_data.json");
        if (Files.exists(p.getParent())) {
            return p;
        }
        Path alt = Paths.get("backend/src/main/resources/data/vocab_data.json");
        if (Files.exists(alt.getParent())) {
            return alt;
        }
        throw new IllegalStateException("未找到词库目录 src/main/resources/data（请从 backend/ 目录启动后端）");
    }
}
