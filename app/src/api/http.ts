import axios from 'axios'
import { Capacitor } from '@capacitor/core'
import { showToast } from 'vant'

// API 基址由构建注入（VITE_API_BASE）。Web 端与后端同源，回落 /api 即可；
// 原生包必须注入绝对地址，否则会退化成相对路径而连不上后端。
const BASE_URL = import.meta.env.VITE_API_BASE || '/api'

if (Capacitor.isNativePlatform() && !import.meta.env.VITE_API_BASE) {
  console.error('[shorthand] VITE_API_BASE 未注入：原生包无法访问后端')
}

const http = axios.create({
  baseURL: BASE_URL,
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('app_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code !== 0) {
      showToast(body.message || '请求失败')
      if (body.code === 401) {
        localStorage.removeItem('app_token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body ? body.data : null
  },
  (error) => {
    showToast(error.response?.data?.message || '网络开小差了')
    return Promise.reject(error)
  }
)

export default http
