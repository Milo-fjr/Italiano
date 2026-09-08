package com.italiano.vocab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.italiano.vocab.entity.Word;
import com.italiano.vocab.util.ItalianGrammarUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;

/**
 * 拼写/听写两种产出型模式共用的判分支撑：附加形式判定 + 输入归一化。
 * 抽出来共用是为了两套模式永远同一口径——改一处（如新增附加形式）两边同时生效，不会漂移。
 */
@Service
@RequiredArgsConstructor
public class ExtraFormService {

    /** 附加填写类型：现在时 io 形式 */
    public static final String EXTRA_PRESENT_IO = "presentIo";
    /** 附加填写类型：名词复数 */
    public static final String EXTRA_PLURAL = "plural";

    private final ObjectMapper objectMapper;

    /**
     * 附加填写判定：不规则动词（现在时不规则）→ 现在时 io 形式（conosco/vado/faccio，
     * 覆盖 -isc、双写）；-care/-gare/-iare 拼写音变词不再出附加题（io 均为规则形式，送分）；
     * 不规则名词（不规则复数/-ca/-ga/-cia/-gia 拼写陷阱）→ 复数；复数不变词（外来/缩写/月份）
     * 复数等于原词，无附加题价值。
     * 形容词与性别类标签不考（斜杠多形式难校验/非拼写范畴）；只在其他时态不规则的动词无附加题。
     */
    public Extra resolve(Word w, String tag) {
        if (w.getPos() != null && w.getPos().startsWith("v.")) {
            if (tag != null && tag.contains("现在时不规则")) {
                String io = extractPresentIo(w.getConjugation());
                if (io != null) {
                    // 自反动词（sedersi → mi siedo）：答案自带自反代词，标签要提示连代词一起写，否则学习者不知道
                    String label = io.matches("(?i)^(mi|ti|si|ci|vi)\\s+.*")
                            ? "现在时 io 形式（含自反代词）"
                            : "现在时 io 形式";
                    return new Extra(EXTRA_PRESENT_IO, label, io);
                }
            }
            return null;
        }
        if (ItalianGrammarUtil.isNounPos(w.getPos())
                && w.getPlural() != null && !w.getPlural().isBlank()
                && ((tag != null && tag.contains("不规则复数"))
                || ItalianGrammarUtil.isPluralTrapNoun(w.getWord()))) {
            return new Extra(EXTRA_PLURAL, "复数形式", w.getPlural());
        }
        return null;
    }

    /** 从变位 JSON 取现在时 io 形式；缺失或占位符（—）返回 null */
    private String extractPresentIo(String conjugationJson) {
        if (conjugationJson == null || conjugationJson.isBlank()) {
            return null;
        }
        try {
            var node = objectMapper.readTree(conjugationJson).path("present").path("io");
            if (node.isMissingNode()) {
                return null;
            }
            String v = node.asText();
            return (v == null || v.isBlank() || "—".equals(v)) ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    /** 输入归一化：trim + 小写 + NFD 去重音（è→e、à→a）+ 折叠连续空格——中文键盘打不出重音符号 */
    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ");
    }

    /** 附加填写项（type/label 对外提示，answer 仅在判分后返回） */
    public record Extra(String type, String label, String answer) {}
}
