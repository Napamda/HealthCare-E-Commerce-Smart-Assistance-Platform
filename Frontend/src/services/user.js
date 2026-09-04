import apiClient from './api.js'

// ---- Task 2.1 — Profile ----

export function getProfile() {
  return apiClient.get('/api/users/me').then((res) => res.data)
}

export function updateProfile(data) {
  return apiClient.put('/api/users/me', data).then((res) => res.data)
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient
    .post('/api/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data)
}

// ---- Task 2.2 — Health Profile ----

export function getHealthProfile() {
  return apiClient.get('/api/users/me/health-profile').then((res) => res.data)
}

export function updateHealthProfile(data) {
  return apiClient.put('/api/users/me/health-profile', data).then((res) => res.data)
}

// ---- Task 2.3 — Address Management ----

export function getAddresses() {
  return apiClient.get('/api/users/me/addresses').then((res) => res.data)
}

export function addAddress(data) {
  return apiClient.post('/api/users/me/addresses', data).then((res) => res.data)
}

export function updateAddress(id, data) {
  return apiClient.put(`/api/users/me/addresses/${id}`, data).then((res) => res.data)
}

export function deleteAddress(id) {
  return apiClient.delete(`/api/users/me/addresses/${id}`).then((res) => res.data)
}

export function setDefaultAddress(id) {
  return apiClient.put(`/api/users/me/addresses/${id}/default`).then((res) => res.data)
}
