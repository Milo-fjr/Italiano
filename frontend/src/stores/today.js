import { defineStore } from 'pinia'
import api from '../api'

/** 今日单词 */
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
    async complete(id) {
      await api.completeWord(id)
      await this.load()
    },
    async undo(id) {
      await api.undoWord(id)
      await this.load()
    }
  }
})
