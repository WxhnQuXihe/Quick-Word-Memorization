import http from './http'

export function login(username: string, password: string): Promise<any> {
  return http.post('/auth/login', { username, password })
}

export function getHome(): Promise<any> {
  return http.get('/home')
}

export function listBooks(): Promise<any[]> {
  return http.get('/wordbooks')
}

export function selectBook(id: number): Promise<any> {
  return http.post(`/wordbooks/${id}/select`)
}

export function currentGroup(bookId: number): Promise<any> {
  return http.get('/study/current', { params: { wordbookId: bookId } })
}

export function nextGroup(groupId: number): Promise<any> {
  return http.post(`/study/group/${groupId}/next`)
}

export function markKnown(groupId: number, wordId: number): Promise<any> {
  return http.post(`/study/group/${groupId}/known/${wordId}`)
}

export function toggleFavorite(groupId: number, wordId: number): Promise<boolean> {
  return http.post(`/study/group/${groupId}/favorite/${wordId}`)
}

export function listFavorites(pageNo = 1, pageSize = 20): Promise<any> {
  return http.get('/favorites', { params: { pageNo, pageSize } })
}

export function removeFavorite(wordId: number): Promise<any> {
  return http.delete(`/favorites/${wordId}`)
}

export function listKnown(pageNo = 1, pageSize = 20): Promise<any> {
  return http.get('/known', { params: { pageNo, pageSize } })
}

export function removeKnown(wordId: number): Promise<any> {
  return http.delete(`/known/${wordId}`)
}
