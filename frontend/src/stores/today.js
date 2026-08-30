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
    /** 全部完成：当前批次未完成的词一键标记完成 */
    async completeAll() {
      this.loading = true
      try {
        const data = await api.completeAllToday()
        await this.load()
        return data.completed
      } finally {
        this.loading = false
      }
    },
    async undo(id) {
      await api.undoWord(id)
      await this.load()
    }
  }
})
