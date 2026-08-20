package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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
        // 存量数据回填：仅为空值的词生成形容词变化/名词复数（不覆盖用户已编辑内容）
        backfillGrammarFields();
        // 变位结构升级（旧扁平 JSON → 四时态嵌套）+ 复数规则修正（含混合词性补齐），幂等
        migrateConjugationV2();
        fixPluralV2();

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
}
