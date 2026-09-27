package com.italiano.vocab.service;

import com.italiano.vocab.entity.Setting;
import com.italiano.vocab.mapper.SettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设置服务（单行设置，id=1） */
@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingMapper settingMapper;

    public Setting getSetting() {
        Setting s = settingMapper.selectById(1L);
        if (s == null) {
            // schema.sql 已写入默认行，此处兜底
            s = new Setting();
            s.setId(1L);
            s.setDailyCount(35);
            s.setCooldownDays(7);
            s.setQuizDailyLimit(80);
            s.setSpellDailyLimit(80);
            s.setDictDailyLimit(80);
            settingMapper.insert(s);
        }
        return s;
    }

    @Transactional
    public Setting updateSetting(Integer dailyCount, Integer cooldownDays,
                                 Integer quizDailyLimit, Integer spellDailyLimit, Integer dictDailyLimit) {
        Setting s = getSetting();
        if (dailyCount != null) {
            if (dailyCount < 1 || dailyCount > 200) {
                throw new IllegalArgumentException("每日抽取数量需在 1-200 之间");
            }
            s.setDailyCount(dailyCount);
        }
        if (cooldownDays != null) {
            if (cooldownDays < 0 || cooldownDays > 90) {
                throw new IllegalArgumentException("冷却天数需在 0-90 之间");
            }
            s.setCooldownDays(cooldownDays);
        }
        if (quizDailyLimit != null) {
            validateDailyLimit(quizDailyLimit, "测验每日上限");
            s.setQuizDailyLimit(quizDailyLimit);
        }
        if (spellDailyLimit != null) {
            validateDailyLimit(spellDailyLimit, "拼写每日上限");
            s.setSpellDailyLimit(spellDailyLimit);
        }
        if (dictDailyLimit != null) {
            validateDailyLimit(dictDailyLimit, "听写每日上限");
            s.setDictDailyLimit(dictDailyLimit);
        }
        settingMapper.updateById(s);
        return s;
    }

    /** 每日上限校验：0=不限制，上限 500 防误输 */
    private static void validateDailyLimit(Integer value, String label) {
        if (value < 0 || value > 500) {
            throw new IllegalArgumentException(label + "需在 0-500 之间（0=不限制）");
        }
    }
}
