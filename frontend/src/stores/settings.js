import { defineStore } from 'pinia'
import api from '../api'

/** 设置 */
export const useSettingsStore = defineStore('settings', {
  state: () => ({
    dailyCount: 35,
    cooldownDays: 7,
    loading: false
  }),
  actions: {
    async load() {
      this.loading = true
      try {
        const s = await api.getSettings()
        this.dailyCount = s.dailyCount
        this.cooldownDays = s.cooldownDays
      } finally {
        this.loading = false
      }
    },
    async save() {
      const s = await api.updateSettings({
        dailyCount: this.dailyCount,
        cooldownDays: this.cooldownDays
      })
      this.dailyCount = s.dailyCount
      this.cooldownDays = s.cooldownDays
    }
  }
})
