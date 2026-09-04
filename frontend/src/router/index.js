import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'today', component: () => import('../views/TodayView.vue'), meta: { title: '学习模式' } },
    { path: '/quiz', name: 'quiz', component: () => import('../views/QuizView.vue'), meta: { title: '测验模式' } },
    { path: '/spell', name: 'spell', component: () => import('../views/SpellView.vue'), meta: { title: '拼写模式' } },
    { path: '/dict', name: 'dict', component: () => import('../views/DictView.vue'), meta: { title: '听写模式' } },
    { path: '/notebook', name: 'notebook', component: () => import('../views/NotebookView.vue'), meta: { title: '错题本' } },
    { path: '/library', name: 'library', component: () => import('../views/LibraryView.vue'), meta: { title: '单词库' } },
    { path: '/stats', name: 'stats', component: () => import('../views/StatsView.vue'), meta: { title: '统计' } },
    { path: '/settings', name: 'settings', component: () => import('../views/SettingsView.vue'), meta: { title: '设置' } }
  ]
})

export default router
