package com.italiano.vocab.controller;

import com.italiano.vocab.dto.ApiResponse;
import com.italiano.vocab.entity.Setting;
import com.italiano.vocab.service.SettingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 设置 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    /** 获取设置（每日抽取数量、冷却天数） */
    @GetMapping
    public ApiResponse<Setting> get() {
        return ApiResponse.ok(settingService.getSetting());
    }

    /** 修改设置 */
    @PutMapping
    public ApiResponse<Setting> update(@RequestBody UpdateSettingBody body) {
        return ApiResponse.ok(settingService.updateSetting(body.getDailyCount(), body.getCooldownDays()));
    }

    @Data
    public static class UpdateSettingBody {
        private Integer dailyCount;
        private Integer cooldownDays;
    }
}
