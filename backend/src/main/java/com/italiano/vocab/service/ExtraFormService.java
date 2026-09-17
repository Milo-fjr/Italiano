package com.italiano.vocab.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;

/**
 * 拼写/听写/加练各产出型模式共用的输入归一化。
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
}
