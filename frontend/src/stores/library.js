import { defineStore } from 'pinia'
import api from '../api'

/** 单词库（列表 + 筛选） */
export const useLibraryStore = defineStore('library', {
  state: () => ({
    items: [],
    total: 0,
    page: 1,
    size: 20,
    loading: false,
    category: '',
    status: null,
    keyword: '',
    categories: []
  }),
  actions: {
    async load() {
      this.loading = true
      try {
        const params = { page: this.page, size: this.size }
        if (this.category) params.category = this.category
        if (this.status !== null && this.status !== '' && this.status !== undefined) params.status = this.status
        if (this.keyword) params.keyword = this.keyword
        const data = await api.listWords(params)
        this.items = data.items
        this.total = data.total
      } finally {
        this.loading = false
      }
    },
    // 分类列表来自统计接口的 categoryStats（保持词库原有顺序）
    async loadCategories() {
      if (this.categories.length) return
      const stats = await api.getStats()
      this.categories = stats.categoryStats.map((c) => c.category)
    },
    async search() {
      this.page = 1
      await this.load()
    }
  }
})
