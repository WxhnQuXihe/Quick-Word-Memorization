import http from './http'

export interface LoginResult {
  token: string
  userId: number
  username: string
  nickname: string
}

export function adminLogin(username: string, password: string): Promise<LoginResult> {
  return http.post('/admin/auth/login', { username, password })
}

export function listUsers(pageNo = 1, pageSize = 20, keyword = ''): Promise<any> {
  return http.get('/admin/users', { params: { pageNo, pageSize, keyword } })
}

export function createUser(data: { username: string; password: string; nickname?: string }): Promise<void> {
  return http.post('/admin/users', data)
}

export function resetPassword(id: number, password: string): Promise<void> {
  return http.post(`/admin/users/${id}/reset-password`, { password })
}

export function updateUserStatus(id: number, status: number): Promise<void> {
  return http.patch(`/admin/users/${id}/status`, null, { params: { status } })
}

export function listWordbooks(): Promise<any[]> {
  return http.get('/admin/wordbooks')
}

export function createWordbook(data: any): Promise<any> {
  return http.post('/admin/wordbooks', data)
}

export function updateWordbook(id: number, data: any): Promise<any> {
  return http.put(`/admin/wordbooks/${id}`, data)
}

export function updateWordbookStatus(id: number, status: number): Promise<void> {
  return http.post(`/admin/wordbooks/${id}/status`, null, { params: { status } })
}

export function listWords(pageNo = 1, pageSize = 20, keyword = ''): Promise<any> {
  return http.get('/admin/words', { params: { pageNo, pageSize, keyword } })
}

export function createWord(data: any): Promise<number> {
  return http.post('/admin/words', data)
}

export function updateWord(id: number, data: any): Promise<void> {
  return http.put(`/admin/words/${id}`, data)
}

export function deleteWord(id: number): Promise<void> {
  return http.delete(`/admin/words/${id}`)
}

export function listImports(pageNo = 1, pageSize = 20): Promise<any> {
  return http.get('/admin/import/logs', { params: { pageNo, pageSize } })
}

export function importWordbook(wordbookId: number, file: File): Promise<any> {
  const form = new FormData()
  form.append('file', file)
  return http.post('/admin/import/wordbook', form, {
    params: { wordbookId },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function listFeedback(pageNo = 1, pageSize = 20, status?: number): Promise<any> {
  return http.get('/admin/feedback', { params: { pageNo, pageSize, status } })
}

export function replyFeedback(id: number, reply: string): Promise<void> {
  return http.post(`/admin/feedback/${id}/reply`, { reply })
}

export function getDashboard(): Promise<any> {
  return http.get('/admin/dashboard')
}
