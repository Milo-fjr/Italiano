import { defineStore } from 'pinia'
import api from '../api'

/** 学习统计 */
export const useStatsStore = defineStore('stats', {
  state: () => ({
    stats: null,
    loading: false
  }),
  actions: {
    async load() {
      this.loading = true
      try {
        this.stats = await api.getStats()
      } finally {
        this.loading = false
      }
    }
  }
})
