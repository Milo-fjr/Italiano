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
  completeAllToday: () => http.post('/today/complete-all'),
  // 单词库
  listWords: (params) => http.get('/words', { params }),
  getWord: (id) => http.get(`/words/${id}`),
  updateWord: (id, data) => http.put(`/words/${id}`, data),
  completeWord: (id) => http.post(`/words/${id}/complete`),
  undoWord: (id) => http.post(`/words/${id}/undo`),
  // 自测「不认识」：SRS 盒子归 0，明天再复习（词保留在本批次）
  forgetWord: (id) => http.post(`/words/${id}/forget`),
  // 测验模式（SRS 到期复习，独立于学习批次）
  getQuizDue: () => http.get('/quiz'),
  quizKnow: (id) => http.post(`/quiz/${id}/know`),
  // 拼写模式（中→意产出复习，独立拼写盒子 + 防撞规则）
  getSpellDue: () => http.get('/spell'),
  spellAnswer: (id, data) => http.post(`/spell/${id}/answer`, data),
  // 听写模式（听音→写词，独立听写盒子 + 防撞规则；两段式：先选释义再拼写）
  getDictDue: () => http.get('/dict'),
  dictCheckMeaning: (id, data) => http.post(`/dict/${id}/check-meaning`, data),
  dictAnswer: (id, data) => http.post(`/dict/${id}/answer`, data),
  // 错题本（测验/拼写答错的词，背熟后手动移出）
  getNotebook: () => http.get('/notebook'),
  notebookLearn: (id) => http.post(`/notebook/${id}/learn`),
  notebookUndo: (id) => http.post(`/notebook/${id}/undo`),
  notebookLearnAll: () => http.post('/notebook/learn-all'),
  // 统计 / 设置 / 导入
  getStats: () => http.get('/stats'),
  getSettings: () => http.get('/settings'),
  updateSettings: (data) => http.put('/settings', data),
  importWords: () => http.post('/import'),
  // 导出词库（数据库 -> vocab_data.json 备份）
  exportWords: () => http.post('/export'),
  // 加练模式（纯练习，不碰SRS盒子，答错进错题本）
  getPractice: (type, count) => http.get('/practice', { params: { type, count } }),
  practiceKnow: (id, know) => http.post(`/practice/${id}/know`, null, { params: { know } }),
  practiceSpellAnswer: (id, data) => http.post(`/practice/${id}/spell-answer`, data),
  practiceDictCheckMeaning: (id, data) => http.post(`/practice/${id}/dict-check-meaning`, data),
  practiceDictAnswer: (id, data) => http.post(`/practice/${id}/dict-answer`, data)
}
