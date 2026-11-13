import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    name: localStorage.getItem('admin_name') || 'admin'
  }),
  actions: {
    setLogin(token: string, name: string) {
      this.token = token
      this.name = name
      localStorage.setItem('admin_token', token)
      localStorage.setItem('admin_name', name)
    },
    logout() {
      this.token = ''
      this.name = ''
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_name')
    }
  }
})
