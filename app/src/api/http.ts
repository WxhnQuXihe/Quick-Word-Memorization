import axios from 'axios'
import { showToast } from 'vant'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
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
