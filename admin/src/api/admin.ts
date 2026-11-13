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
