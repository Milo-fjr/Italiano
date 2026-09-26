package com.italiano.vocab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.italiano.vocab.dto.DictWordDTO;
import com.italiano.vocab.dto.IrregularAnswerDTO;
import com.italiano.vocab.dto.IrregularPointDTO;
import com.italiano.vocab.dto.IrregularWordDTO;
import com.italiano.vocab.dto.PersonTenseOptionDTO;
import com.italiano.vocab.dto.SpellWordDTO;
import com.italiano.vocab.dto.TodayWordDTO;
import com.italiano.vocab.entity.Word;
import com.italiano.vocab.entity.WordProgress;
import com.italiano.vocab.mapper.WordMapper;
import com.italiano.vocab.mapper.WordProgressMapper;
import com.italiano.vocab.util.ItalianGrammarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 加练模式：纯练习，不碰任何 SRS 盒子/完成次数。
 * 四题型：quiz（认识）、spell（拼写）、dict（听写）、irregular（变化专考：听辨 + 不规则拼写）。
 * 选词范围：已学过的词（extract_count > 0），随机抽取。
 * 答错只进错题本，答对什么都不记——答到一半退出也没记录（零持久化负担）。
 * <p>
 * 变化专考（2026-09-22 听力改造，合并原「变位听写」构想）：题面藏词听形式——
 * 流程 = 听形式 → 选释义（4 选 1）→ 选人称时态（4 选 1，仅 present/futuro 且无同形歧义）→ 拼写；
 * 规则形式以「纯听辨点」入池（每词随机 1 个未被考点占用的时态人称，选对即过不拼——
 * 规则变位拼写无产出价值，练的是音→词尾解码）；同形形式（如 essere 的 sono=io/loro）不出选人称关。
 * 不规则考点由语法引擎枚举（例外表 ∪ -isc 型，逐人称与规则推导比对过滤规则形式）；
 * 判分现场推导——DB 手动编辑值（变位/复数/形容词 JSON）优先，引擎推导兜底。
 * 未完成时不考（用户尚未学习，学后补，见 AGENTS.md TODO）。
 */
@Service
@RequiredArgsConstructor
public class PracticeService {

    private final WordMapper wordMapper;
    private final WordProgressMapper progressMapper;
    private final ObjectMapper objectMapper;

    /** 选人称时态选项池（仅两种时态可听辨人称；未完成时未学不进池，学后随考点枚举一起补） */
    private static final Map<String, String> TENSE_LABELS = Map.of("present", "现在时", "futuro", "简单将来时");

    /** 从已学词里随机抽 count 个，按题型组装 DTO */
    public Map<String, Object> draw(String type, int count) {
        if (count <= 0) count = 20;
        if (count > 100) count = 100;

        // 已学过的词（有学习记录且 extract_count > 0）
        List<WordProgress> learned = progressMapper.selectList(new LambdaQueryWrapper<WordProgress>()
                .gt(WordProgress::getExtractCount, 0));
        if (learned.isEmpty()) {
            return emptyResult(type, "还没有学过的词，先去学习模式背一些吧。");
        }

        // 随机打乱
        Collections.shuffle(learned);

        // 变化专考：动词必有听辨点（规则形式采样），从全量已学词里筛够 count 个（整词入队，考点全出）
        if ("irregular".equals(type)) {
            Map<Long, Word> wordById = wordMapper.selectBatchIds(
                            learned.stream().map(WordProgress::getWordId).toList()).stream()
                    .collect(Collectors.toMap(Word::getId, Function.identity()));
            List<IrregularWordDTO> words = new ArrayList<>();
            for (WordProgress p : learned) {
                Word w = wordById.get(p.getWordId());
                if (w == null) {
                    continue;
                }
                List<IrregularPointDTO> points = buildIrregularPoints(w);
                if (points.isEmpty()) {
                    continue;
                }
                words.add(buildIrregularDTO(w, points));
                if (words.size() >= count) {
                    break;
                }
            }
            if (words.isEmpty()) {
                return emptyResult(type, "已学词里还没有可考的变化形式。");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", words.size());
            result.put("words", words);
            return result;
        }

        List<WordProgress> picked = learned.subList(0, Math.min(count, learned.size()));

        List<Long> ids = picked.stream().map(WordProgress::getWordId).toList();
        Map<Long, Word> wordById = wordMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Word::getId, Function.identity()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", picked.size());

        switch (type) {
            case "quiz" -> {
                List<TodayWordDTO> words = new ArrayList<>();
                for (WordProgress p : picked) {
                    Word w = wordById.get(p.getWordId());
                    if (w == null) continue;
                    TodayWordDTO dto = new TodayWordDTO();
                    dto.setWordId(w.getId());
                    dto.setWord(w.getWord());
                    dto.setPos(w.getPos());
                    dto.setMeaning(w.getMeaning());
                    dto.setCategory(w.getCategory());
                    dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
                    words.add(dto);
                }
                result.put("words", words);
            }
            case "spell" -> {
                List<SpellWordDTO> words = new ArrayList<>();
                for (WordProgress p : picked) {
                    Word w = wordById.get(p.getWordId());
                    if (w == null) continue;
                    words.add(buildSpellDTO(w));
                }
                result.put("words", words);
            }
            case "dict" -> {
                List<DictWordDTO> words = new ArrayList<>();
                for (WordProgress p : picked) {
                    Word w = wordById.get(p.getWordId());
                    if (w == null) continue;
                    words.add(buildDictDTO(w));
                }
                result.put("words", words);
            }
            default -> throw new IllegalArgumentException("未知题型：" + type);
        }
        return result;
    }

    /**
     * 判分（quiz/spell/dict）：答错 → 进错题本；答对 → 什么都不记。不碰 SRS 盒子、不记答题时间。
     * 返回正确答案结构（与 spell/dict/quiz 判分返回字段一致，便于前端复用结果展示）。
     */
    @Transactional
    public Map<String, Object> answer(String type, Long wordId, String wordInput,
                                      String meaningInput, boolean know) {
        Word w = wordMapper.selectById(wordId);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        String tag = ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender());

        boolean passed;
        boolean wordCorrect = true;
        boolean meaningCorrect = true;

        switch (type) {
            case "quiz" -> passed = know;
            case "spell" -> {
                wordCorrect = ExtraFormService.normalize(wordInput).equals(ExtraFormService.normalize(w.getWord()));
                passed = wordCorrect;
            }
            case "dict" -> {
                wordCorrect = ExtraFormService.normalize(wordInput).equals(ExtraFormService.normalize(w.getWord()));
                meaningCorrect = ExtraFormService.matchMeaning(meaningInput, w.getMeaning());
                passed = wordCorrect && meaningCorrect;
            }
            default -> throw new IllegalArgumentException("未知题型：" + type);
        }

        // 唯一副作用：答错进错题本（幂等）
        if (!passed) {
            markNotebook(wordId);
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("passed", passed);
        r.put("wordCorrect", wordCorrect);
        r.put("meaningCorrect", meaningCorrect);
        r.put("word", w.getWord());
        r.put("meaning", w.getMeaning());
        r.put("pos", w.getPos());
        r.put("category", w.getCategory());
        r.put("irregular", tag);
        return r;
    }

    /**
     * 变化专考判分：题面藏词，三段判定——释义 + 人称时态（personChoice/listenOnly 点）+ 拼写（listenOnly 点无）。
     * 考点描述与所选组合由请求回传，正确答案现场推导——DB 手动编辑值优先，引擎兜底。
     * 答错只进错题本，不碰任何 SRS 盒子/次数；返回各关对错 + 正确答案供结果对照。
     */
    @Transactional
    public Map<String, Object> answerIrregular(Long wordId, IrregularAnswerDTO body) {
        Word w = wordMapper.selectById(wordId);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        String type = body.getType();
        String person = body.getPerson();

        boolean meaningCorrect = ExtraFormService.matchMeaning(body.getMeaning(), w.getMeaning());
        boolean personChoice = Boolean.TRUE.equals(body.getPersonChoice());
        boolean listenOnly = Boolean.TRUE.equals(body.getListenOnly());
        boolean personCorrect = true;
        if (personChoice || listenOnly) {
            personCorrect = type != null && type.equals(body.getChosenTense())
                    && person != null && person.equals(body.getChosenPerson());
        }
        String answer = resolveForm(w, type, person,
                body.getContextNoun(), body.getContextGender(), body.getContextPlural());
        boolean wordCorrect = listenOnly || (answer != null && matchesAny(body.getInput(), answer));
        boolean passed = meaningCorrect && personCorrect && wordCorrect;
        if (!passed) {
            markConjNotebook(wordId); // 变位答错进变位本（2026-09-26 拆本：与词本分离）
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("passed", passed);
        r.put("meaningCorrect", meaningCorrect);
        r.put("personCorrect", personCorrect);
        r.put("wordCorrect", wordCorrect);
        r.put("answer", answer);
        r.put("word", w.getWord());
        r.put("meaning", w.getMeaning());
        r.put("pos", w.getPos());
        r.put("category", w.getCategory());
        r.put("irregular", ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
        return r;
    }

    /** 听力第一关释义预检（听写/变化专考共用口径）：只判断不落库、不返回正确答案，选错由前端走正式判分 */
    public boolean checkWordMeaning(Long wordId, String meaningInput) {
        Word w = wordMapper.selectById(wordId);
        if (w == null) {
            throw new IllegalArgumentException("单词不存在");
        }
        return ExtraFormService.matchMeaning(meaningInput, w.getMeaning());
    }

    /** 「选人称时态」预检：所选组合与考点标识比对，只判断不落库 */
    public boolean checkIrregularPerson(String type, String person, String chosenTense, String chosenPerson) {
        return type != null && type.equals(chosenTense) && person != null && person.equals(chosenPerson);
    }

    // ===== 不规则考点枚举与答案推导 =====

    /**
     * 枚举一个词的全部考点（不含判分答案，含听力题面字段）：
     * - 动词：现在时逐人称、过去分词、简单将来时逐人称（与规则推导比对，规则形式不考；
     *   present/futuro 无同形歧义者先出「选人称时态」关）+ 1 个纯听辨点（规则形式随机采样）
     * - 名词：不规则复数（-ca/-ga/-cia/-gia 拼写陷阱词不在此列，规则可推导）
     * - 形容词：bello 型定语形式（BELLO_PRACTICE 语境名词）、-co/-go 硬软音阳性复数、不变形容词复数
     * 不考：未完成时（未学）、阴阳性特殊/性别需记（非变形考点）
     */
    private List<IrregularPointDTO> buildIrregularPoints(Word w) {
        List<IrregularPointDTO> points = new ArrayList<>();
        String tag = ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender());
        String lw = w.getWord().toLowerCase();
        if (w.getPos() != null && w.getPos().startsWith("v.")) {
            boolean reflexive = lw.endsWith("si");
            String infinitive = reflexive ? lw.substring(0, lw.length() - 2) + "e" : lw;
            addPersonPoints(points, "present", "现在时",
                    ItalianGrammarUtil.irregularPresent(infinitive),
                    ItalianGrammarUtil.regularPresent(infinitive), reflexive, w);
            if (tag != null && tag.contains("近过去时不规则")) {
                IrregularPointDTO pp = point("pp", "过去分词");
                fillListeningFields(pp, w);
                points.add(pp);
            }
            addPersonPoints(points, "futuro", "简单将来时",
                    ItalianGrammarUtil.irregularFuturo(infinitive),
                    ItalianGrammarUtil.regularFuturo(infinitive), reflexive, w);
            addListenOnlyPoint(points, w);
        } else if (ItalianGrammarUtil.isNounPos(w.getPos())) {
            if (tag != null && tag.contains("不规则复数")) {
                IrregularPointDTO p = point("plural", "复数形式");
                fillListeningFields(p, w);
                points.add(p);
            }
        } else if (w.getPos() != null && w.getPos().contains("agg.")) {
            if (tag != null && tag.contains("冠词式变化")) {
                for (String[] ctx : ItalianGrammarUtil.BELLO_PRACTICE) {
                    boolean plural = "pl".equals(ctx[3]);
                    String answer = ItalianGrammarUtil.belloAttributive(lw, ctx[0], ctx[2], plural);
                    // 规则式复数（buono→buoni）无加练价值，跳过
                    if (answer == null || isRegularPlural(lw, answer)) {
                        continue;
                    }
                    IrregularPointDTO p = point("bello", "定语形式");
                    p.setContextNoun(ctx[0]);
                    p.setContextMeaning(ctx[1]);
                    p.setContextGender(ctx[2]);
                    p.setContextPlural(plural);
                    fillListeningFields(p, w);
                    points.add(p);
                }
            } else if (ItalianGrammarUtil.isInvariantAdjective(lw)) {
                IrregularPointDTO p = point("adjInv", "复数形式（性数不变）");
                fillListeningFields(p, w);
                points.add(p);
            } else if (tag != null && tag.contains("不规则变化")) {
                IrregularPointDTO p = point("adjMp", "阳性复数");
                fillListeningFields(p, w);
                points.add(p);
            }
        }
        return points;
    }

    /** 动词逐人称筛考点：不规则形式与规则推导相同的人称不考（prendere 整表、andare 的 noi/voi 被过滤）；同形歧义降级见 fillListeningFields */
    private void addPersonPoints(List<IrregularPointDTO> points, String type, String tenseLabel,
                                 String[] irregular, String[] regular, boolean reflexive, Word w) {
        if (irregular == null || regular == null) {
            return;
        }
        for (int i = 0; i < ItalianGrammarUtil.PERSONS.length; i++) {
            if (!irregular[i].equals(regular[i])) {
                String person = ItalianGrammarUtil.PERSONS[i];
                IrregularPointDTO p = point(type, tenseLabel + " · " + person);
                p.setPerson(person);
                if (reflexive) {
                    p.setLabel(p.getLabel() + "（含自反代词）");
                }
                fillListeningFields(p, w);
                points.add(p);
            }
        }
    }

    /**
     * 填充听力题面字段：播报形式 + present/futuro 的「选人称时态」关判定。
     * 同形歧义（该形式在同一时态内与其他人称相同，如 essere 的 sono=io/loro）→ 不出选人称关，
     * 降级为听形式直接拼写（形式照考，只是人称不可辨）。
     */
    private void fillListeningFields(IrregularPointDTO p, Word w) {
        p.setForm(resolveForm(w, p.getType(), p.getPerson(),
                p.getContextNoun(), p.getContextGender(), p.getContextPlural()));
        if ("present".equals(p.getType()) || "futuro".equals(p.getType())) {
            boolean homonym = isHomonymForm(w, p.getType(), p.getPerson(), p.getForm());
            p.setPersonChoice(!homonym);
            if (!homonym) {
                p.setPersonTenseOptions(buildPersonTenseOptions(p.getType(), p.getPerson()));
            }
        }
    }

    /** 形式在该时态内是否与其他人称同形（form 已解析传入，避免重复推导） */
    private boolean isHomonymForm(Word w, String tense, String person, String form) {
        if (form == null || form.isBlank()) {
            return false;
        }
        for (String other : ItalianGrammarUtil.PERSONS) {
            if (other.equals(person)) {
                continue;
            }
            if (form.equals(resolveForm(w, tense, other, null, null, null))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 纯听辨点：从未被本词考点占用的 (现在时/将来时, 人称) 组合里随机抽 1 个（排除同形歧义），
     * 听形式选对人称时态即过、不拼写——规则变位拼写无产出价值，练的是音→词尾解码。
     * 整表不规则的词（potere/essere 的 futuro 等）候选耗尽则自然不出。
     */
    private void addListenOnlyPoint(List<IrregularPointDTO> points, Word w) {
        Set<String> occupied = points.stream()
                .filter(p -> "present".equals(p.getType()) || "futuro".equals(p.getType()))
                .map(p -> p.getType() + "|" + p.getPerson())
                .collect(Collectors.toSet());
        List<String[]> candidates = new ArrayList<>();
        for (String tense : TENSE_LABELS.keySet()) {
            for (String person : ItalianGrammarUtil.PERSONS) {
                if (occupied.contains(tense + "|" + person)) {
                    continue;
                }
                String form = resolveForm(w, tense, person, null, null, null);
                if (form == null || form.isBlank() || "—".equals(form) || isHomonymForm(w, tense, person, form)) {
                    continue;
                }
                candidates.add(new String[]{tense, person});
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        String[] pick = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        IrregularPointDTO p = point(pick[0], TENSE_LABELS.get(pick[0]) + " · " + pick[1]);
        p.setPerson(pick[1]);
        p.setListenOnly(true);
        fillListeningFields(p, w);
        points.add(p);
    }

    /** 「选人称时态」4 选 1：正确项 + 3 个随机干扰组合，整体打乱（不含正确项标识） */
    private List<PersonTenseOptionDTO> buildPersonTenseOptions(String correctTense, String correctPerson) {
        List<PersonTenseOptionDTO> options = new ArrayList<>();
        options.add(tenseOption(correctTense, correctPerson));
        List<String[]> pool = new ArrayList<>();
        for (Map.Entry<String, String> e : TENSE_LABELS.entrySet()) {
            for (String person : ItalianGrammarUtil.PERSONS) {
                if (e.getKey().equals(correctTense) && person.equals(correctPerson)) {
                    continue;
                }
                pool.add(new String[]{e.getKey(), person});
            }
        }
        Collections.shuffle(pool);
        for (int i = 0; i < Math.min(3, pool.size()); i++) {
            options.add(tenseOption(pool.get(i)[0], pool.get(i)[1]));
        }
        Collections.shuffle(options);
        return options;
    }

    private static PersonTenseOptionDTO tenseOption(String tense, String person) {
        PersonTenseOptionDTO o = new PersonTenseOptionDTO();
        o.setTense(tense);
        o.setPerson(person);
        o.setLabel(TENSE_LABELS.get(tense) + " · " + person);
        return o;
    }

    /** 现场推导考点正确答案（播报文本同源）：DB 手动编辑值（变位/复数/形容词 JSON）优先，缺失回退引擎推导 */
    private String resolveForm(Word w, String type, String person,
                                          String contextNoun, String contextGender, Boolean contextPlural) {
        boolean plural = Boolean.TRUE.equals(contextPlural);
        return switch (type) {
            case "present" -> conjugationForm(w, "present", person);
            case "pp" -> participleFromDb(w);
            case "futuro" -> conjugationForm(w, "futuro", person);
            case "plural" -> w.getPlural() != null && !w.getPlural().isBlank()
                    ? w.getPlural()
                    : ItalianGrammarUtil.buildPlural(w.getWord(), w.getPos());
            case "bello" -> ItalianGrammarUtil.belloAttributive(w.getWord(), contextNoun, contextGender, plural);
            case "adjMp" -> adjectiveFormFromDb(w, "mp");
            case "adjInv" -> w.getWord();
            default -> throw new IllegalArgumentException("未知考点类型：" + type);
        };
    }

    /** 变位真值：DB 变位 JSON 优先（含手动修正），缺失回退引擎推导（反身代词由引擎统一处理） */
    private String conjugationForm(Word w, String tense, String person) {
        if (w.getConjugation() != null && !w.getConjugation().isBlank()) {
            try {
                var node = objectMapper.readTree(w.getConjugation()).path(tense).path(person);
                if (!node.isMissingNode()) {
                    String v = node.asText();
                    if (!v.isBlank() && !"—".equals(v)) {
                        return v;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        Map<String, Map<String, String>> conj = ItalianGrammarUtil.buildConjugation(w.getWord(), w.getPos());
        if (conj != null) {
            Map<String, String> tenseMap = conj.get(tense);
            if (tenseMap != null) {
                String f = tenseMap.get(person);
                if (f != null && !f.isBlank()) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * 过去分词真值：DB 近过去 io 形式（ho preso / sono andato/a / mi sono alzato/a / ho/sono vissuto）
     * 剥助动词与性数配合标注取裸分词；缺失回退引擎分词表。
     */
    private String participleFromDb(Word w) {
        String pp = conjugationForm(w, "passatoProssimo", "io");
        if (pp != null) {
            String[] parts = pp.trim().split("\\s+");
            String participle = parts[parts.length - 1].split("/")[0];
            if (!participle.isBlank() && !"—".equals(participle)) {
                return participle;
            }
        }
        String lw = w.getWord().toLowerCase();
        boolean reflexive = lw.endsWith("si");
        return ItalianGrammarUtil.pastParticiple(reflexive ? lw.substring(0, lw.length() - 2) + "e" : lw);
    }

    /** 形容词形式真值：DB adjForms JSON 优先（含手动修正），缺失回退引擎推导 */
    private String adjectiveFormFromDb(Word w, String key) {
        if (w.getAdjForms() != null && !w.getAdjForms().isBlank()) {
            try {
                var node = objectMapper.readTree(w.getAdjForms()).path(key);
                if (!node.isMissingNode()) {
                    String v = node.asText();
                    if (!v.isBlank() && !"—".equals(v)) {
                        return v;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        Map<String, String> forms = ItalianGrammarUtil.buildAdjectiveForms(w.getWord(), w.getPos());
        return forms == null ? null : forms.get(key);
    }

    /** 判分：输入归一化后与答案比对；"/" 分隔的多形式（colleghi/colleghe、piloti/pilote）任一命中即对（包可见供单测） */
    static boolean matchesAny(String input, String answer) {
        String normalized = ExtraFormService.normalize(input);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String alt : answer.split("/")) {
            if (normalized.equals(ExtraFormService.normalize(alt))) {
                return true;
            }
        }
        return false;
    }

    /** bello 型语境答案若等于规则式复数（buono→buoni）则无加练价值 */
    private static boolean isRegularPlural(String adjective, String answer) {
        return answer.equals(adjective.substring(0, adjective.length() - 1) + "i");
    }

    private static IrregularPointDTO point(String type, String label) {
        IrregularPointDTO p = new IrregularPointDTO();
        p.setType(type);
        p.setLabel(label);
        return p;
    }

    private IrregularWordDTO buildIrregularDTO(Word w, List<IrregularPointDTO> points) {
        IrregularWordDTO dto = new IrregularWordDTO();
        dto.setWordId(w.getId());
        dto.setWord(w.getWord());
        dto.setPos(w.getPos());
        dto.setMeaning(w.getMeaning());
        dto.setCategory(w.getCategory());
        dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
        dto.setMeaningOptions(buildIrregularMeaningOptions(w));
        dto.setPoints(points);
        return dto;
    }

    // ===== 通用工具（quiz/spell/dict 三题型）=====

    /** 答错进错题本（幂等：已在本的词保持不动）——quiz/spell/dict 三题型用词本 */
    private void markNotebook(Long wordId) {
        WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getWordId, wordId));
        if (p != null && !Boolean.TRUE.equals(p.getInNotebook())) {
            p.setInNotebook(true);
            progressMapper.updateById(p);
        }
    }

    /** 变位答错进变位本（幂等；与词本独立标记，quiz/spell/dict 的 markNotebook 互不影响） */
    private void markConjNotebook(Long wordId) {
        WordProgress p = progressMapper.selectOne(new LambdaQueryWrapper<WordProgress>()
                .eq(WordProgress::getWordId, wordId));
        if (p != null && !Boolean.TRUE.equals(p.getInConjNotebook())) {
            p.setInConjNotebook(true);
            progressMapper.updateById(p);
        }
    }

    private SpellWordDTO buildSpellDTO(Word w) {
        SpellWordDTO dto = new SpellWordDTO();
        dto.setWordId(w.getId());
        dto.setMeaning(w.getMeaning());
        dto.setPos(w.getPos());
        dto.setCategory(w.getCategory());
        dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
        return dto;
    }

    private DictWordDTO buildDictDTO(Word w) {
        DictWordDTO dto = new DictWordDTO();
        dto.setWordId(w.getId());
        dto.setWord(w.getWord());
        dto.setPos(w.getPos());
        dto.setCategory(w.getCategory());
        dto.setIrregular(ItalianGrammarUtil.irregularTag(w.getWord(), w.getPos(), w.getGender()));
        dto.setMeaningOptions(buildMeaningOptions(w));
        return dto;
    }

    /** 释义 4 选 1：正确释义 + 3 个随机干扰项（与 DictService 一致） */
    private List<String> buildMeaningOptions(Word w) {
        List<String> options = new ArrayList<>();
        options.add(w.getMeaning());
        appendMeaningDistractors(options, wordMapper.selectList(new LambdaQueryWrapper<Word>()
                .select(Word::getMeaning)
                .ne(Word::getId, w.getId())
                .last("ORDER BY RAND() LIMIT 20")));
        Collections.shuffle(options);
        return options;
    }

    /**
     * 变化专考听力第一关释义 4 选 1：同词性优先取干扰项（动词题从动词抽，听辨才有辨析价值），
     * 同词性不足 4 个回退全库随机；归一化去重同上。
     */
    private List<String> buildIrregularMeaningOptions(Word w) {
        List<String> options = new ArrayList<>();
        options.add(w.getMeaning());
        String prefix = meaningPoolPrefix(w);
        if (prefix != null) {
            appendMeaningDistractors(options, wordMapper.selectList(new LambdaQueryWrapper<Word>()
                    .select(Word::getMeaning)
                    .ne(Word::getId, w.getId())
                    .likeRight(Word::getPos, prefix)
                    .last("ORDER BY RAND() LIMIT 20")));
        }
        if (options.size() < 4) {
            appendMeaningDistractors(options, wordMapper.selectList(new LambdaQueryWrapper<Word>()
                    .select(Word::getMeaning)
                    .ne(Word::getId, w.getId())
                    .last("ORDER BY RAND() LIMIT 20")));
        }
        Collections.shuffle(options);
        return options;
    }

    /** 同词性干扰项查询前缀：动词 v* / 形容词 agg* / 名词 s* */
    private static String meaningPoolPrefix(Word w) {
        String pos = w.getPos() == null ? "" : w.getPos();
        if (pos.startsWith("v")) {
            return "v";
        }
        if (pos.contains("agg")) {
            return "agg";
        }
        if (ItalianGrammarUtil.isNounPos(w.getPos())) {
            return "s";
        }
        return null;
    }

    /** 追加释义干扰项：归一化后与已有选项及彼此不重复，补到 4 个为止 */
    private void appendMeaningDistractors(List<String> options, List<Word> candidates) {
        for (Word c : candidates) {
            if (options.size() >= 4) return;
            String m = c.getMeaning();
            boolean dup = options.stream().anyMatch(o -> ExtraFormService.normalizeMeaning(o).equals(ExtraFormService.normalizeMeaning(m)));
            if (!dup) options.add(m);
        }
    }

    private Map<String, Object> emptyResult(String type, String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", 0);
        r.put("words", List.of());
        r.put("message", msg);
        return r;
    }
}
