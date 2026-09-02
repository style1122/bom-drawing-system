import request from './request'

export function register(data) {
  return request.post('/user/register', data)
}

export function login(data) {
  return request.post('/user/login', data)
}

export function getPendingUsers() {
  return request.get('/user/pending')
}

export function approveUser(id) {
  return request.put(`/user/approve/${id}`)
}

export function rejectUser(id, reason) {
  return request.put(`/user/reject/${id}`, { reason })
}

export function getUserList() {
  return request.get('/user/list')
}

export function disableUser(id) {
  return request.put(`/user/disable/${id}`)
}

export function resetPassword(id) {
  return request.put(`/user/reset-password/${id}`)
}

export function getCurrentUser() {
  return request.get('/user/current')
}
