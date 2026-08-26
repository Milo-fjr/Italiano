import { defineStore } from 'pinia'
import api from '../api'

/** 学习批次（手动刷新，不按日期自动轮换） */
export const useTodayStore = defineStore('today', {
  state: () => ({
    date: '',
    total: 0,
    completed: 0,
    words: [],
    loading: false
  }),
  actions: {
    async load() {
      this.loading = true
      try {
        const data = await api.getToday()
        this.date = data.date
        this.total = data.total
        this.completed = data.completed
        this.words = data.words
      } finally {
        this.loading = false
      }
    },
    /** 换一批：未完成的词保留进新批次，其余名额重新抽取 */
    async refreshBatch() {
      this.loading = true
      try {
        const data = await api.extractToday()
        this.date = data.date
        this.total = data.total
        this.completed = data.completed
        this.words = data.words
      } finally {
        this.loading = false
      }
    },
    async complete(id) {
      await api.completeWord(id)
      await this.load()
    },
    async undo(id) {
      await api.undoWord(id)
      await this.load()
    },
    /** 自测「不认识」：盒子归 0、明天复习；不刷新列表（词保留本批次，避免重置翻转状态） */
    async forgot(id) {
      await api.forgetWord(id)
    }
  }
})
