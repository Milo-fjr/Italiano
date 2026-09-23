package com.italiano.vocab.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;

/**
 * 各产出型模式（拼写/听写/加练）共用的输入归一化与中文释义判分。
 * 抽出来共用是为了各模式永远同一口径——改一处全部同时生效，不会漂移。
 * （原「附加形式判定」已下线：不规则变化改由加练模式第 4 题型专考，见 PracticeService。）
 */
@Service
public class ExtraFormService {

    /** 输入归一化：trim + 小写 + NFD 去重音（è→e、à→a）+ 折叠连续空格——中文键盘打不出重音符号 */
    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ");
    }

    /**
     * 中文释义判分（点选口径）：输入与答案都按「；」拆成子段，任一子段归一化命中任一答案子段即对。
     * 点选传入整个选项文本（如「一对；情侣」）也能正确命中；输入为 null（释义关没选就放弃）判错。
     * 听写/加练判分共用（原 DictService 与 PracticeService 两份私有拷贝上收于此，2026-09-23）。
     */
    public static boolean matchMeaning(String input, String answer) {
        if (input == null) {
            return false;
        }
        for (String in : input.split("[；;]")) {
            String ni = normalizeMeaning(in);
            if (ni.isEmpty()) {
                continue;
            }
            for (String alt : answer.split("[；;]")) {
                if (normalizeMeaning(alt).equals(ni)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 释义归一化：trim + 剔除中英文括号注解 + 剔除全部空格 + 剥掉尾部虚词（的了呀啊吧呢吗地）。
     * 判分容错之外，还承担释义 4 选 1 选项构建的去重（防「好」「好的」同现两个选项）。
     */
    public static String normalizeMeaning(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim()
                .replaceAll("（[^）]*）|\\([^)]*\\)", "")
                .replaceAll("\\s+", "");
        while (!t.isEmpty() && "的了呀啊吧呢吗地".indexOf(t.charAt(t.length() - 1)) >= 0) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
