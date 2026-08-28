import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000
})

// 响应拦截：统一解包 ApiResponse {code, message, data}
http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code !== undefined && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body ? body.data : body
  },
  (err) => {
    ElMessage.error(err.response?.data?.message || err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default {
  // 学习批次（手动刷新）
  getToday: () => http.get('/today'),
  extractToday: () => http.post('/today/extract'),
  // 单词库
  listWords: (params) => http.get('/words', { params }),
  getWord: (id) => http.get(`/words/${id}`),
  updateWord: (id, data) => http.put(`/words/${id}`, data),
  completeWord: (id) => http.post(`/words/${id}/complete`),
  undoWord: (id) => http.post(`/words/${id}/undo`),
  // 自测「不认识」：SRS 盒子归 0，明天再复习（词保留在本批次）
  forgetWord: (id) => http.post(`/words/${id}/forget`),
  // 统计 / 设置 / 导入
  getStats: () => http.get('/stats'),
  getSettings: () => http.get('/settings'),
  updateSettings: (data) => http.put('/settings', data),
  importWords: () => http.post('/import'),
  // 导出词库（数据库 -> vocab_data.json 备份）
  exportWords: () => http.post('/export')
}
