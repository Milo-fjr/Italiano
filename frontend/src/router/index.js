import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'today', component: () => import('../views/TodayView.vue'), meta: { title: '学习批次' } },
    { path: '/library', name: 'library', component: () => import('../views/LibraryView.vue'), meta: { title: '单词库' } },
    { path: '/stats', name: 'stats', component: () => import('../views/StatsView.vue'), meta: { title: '统计' } },
    { path: '/settings', name: 'settings', component: () => import('../views/SettingsView.vue'), meta: { title: '设置' } }
  ]
})

export default router
