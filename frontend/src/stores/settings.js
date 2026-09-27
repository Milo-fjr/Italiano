import { defineStore } from 'pinia'
import api from '../api'

/** 设置 */
export const useSettingsStore = defineStore('settings', {
  state: () => ({
    dailyCount: 35,
    cooldownDays: 7,
    quizDailyLimit: 80,
    spellDailyLimit: 80,
    dictDailyLimit: 80,
    loading: false
  }),
  actions: {
    async load() {
      this.loading = true
      try {
        const s = await api.getSettings()
        this.dailyCount = s.dailyCount
        this.cooldownDays = s.cooldownDays
        this.quizDailyLimit = s.quizDailyLimit
        this.spellDailyLimit = s.spellDailyLimit
        this.dictDailyLimit = s.dictDailyLimit
      } finally {
        this.loading = false
      }
    },
    async save() {
      const s = await api.updateSettings({
        dailyCount: this.dailyCount,
        cooldownDays: this.cooldownDays,
        quizDailyLimit: this.quizDailyLimit,
        spellDailyLimit: this.spellDailyLimit,
        dictDailyLimit: this.dictDailyLimit
      })
      this.dailyCount = s.dailyCount
      this.cooldownDays = s.cooldownDays
      this.quizDailyLimit = s.quizDailyLimit
      this.spellDailyLimit = s.spellDailyLimit
      this.dictDailyLimit = s.dictDailyLimit
    }
  }
})
