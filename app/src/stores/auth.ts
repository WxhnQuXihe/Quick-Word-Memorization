import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('app_token') || '',
    name: localStorage.getItem('app_name') || ''
  }),
  actions: {
    setLogin(token: string, name: string) {
      this.token = token
      this.name = name
      localStorage.setItem('app_token', token)
      localStorage.setItem('app_name', name)
    },
    logout() {
      this.token = ''
      this.name = ''
      localStorage.removeItem('app_token')
      localStorage.removeItem('app_name')
    }
  }
})
