import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory('/'),
  routes: [
    { path: '/login', name: 'login', component: () => import('../pages/Login.vue') },
    { path: '/', name: 'home', component: () => import('../pages/Home.vue') },
    { path: '/study/:bookId', name: 'study', component: () => import('../pages/Study.vue') },
    { path: '/me', name: 'me', component: () => import('../pages/Me.vue') },
    { path: '/favorites', name: 'favorites', component: () => import('../pages/Favorites.vue') },
    { path: '/favorites/study', name: 'favorite-study', component: () => import('../pages/FavoriteStudy.vue') },
    { path: '/known', name: 'known', component: () => import('../pages/Known.vue') }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('app_token')
  if (!token && to.name !== 'login') return { name: 'login' }
  if (token && to.name === 'login') return { name: 'home' }
})

export default router
