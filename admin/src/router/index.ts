import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes: [
    { path: '/login', name: 'login', component: () => import('../pages/Login.vue') },
    {
      path: '/',
      component: AdminLayout,
      children: [
        { path: '', name: 'dashboard', component: () => import('../pages/Dashboard.vue'), meta: { title: '总览' } },
        { path: 'wordbooks', name: 'wordbooks', component: () => import('../pages/Wordbooks.vue'), meta: { title: '词书管理' } },
        { path: 'announcements', name: 'announcements', component: () => import('../pages/Announcements.vue'), meta: { title: '公告管理' } },
        { path: 'words', name: 'words', component: () => import('../pages/Words.vue'), meta: { title: '词条管理' } },
        { path: 'imports', name: 'imports', component: () => import('../pages/Imports.vue'), meta: { title: '导入记录' } },
        { path: 'users', name: 'users', component: () => import('../pages/Users.vue'), meta: { title: '用户管理' } },
        { path: 'feedback', name: 'feedback', component: () => import('../pages/Feedback.vue'), meta: { title: '反馈管理' } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('admin_token')
  if (!token && to.name !== 'login') return { name: 'login' }
  if (token && to.name === 'login') return { name: 'dashboard' }
})

export default router
