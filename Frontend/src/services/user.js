import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export function getUserProfile() {
  return api.get('/api/users/profile').then((res) => res.data)
}

export function updateUserProfile(profileData) {
  return api.put('/api/users/profile', profileData).then((res) => res.data)
}

export function changePassword(currentPassword, newPassword) {
  return api.post('/api/users/change-password', {
    currentPassword,
    newPassword
  }).then((res) => res.data)
}

export function getUserAddresses() {
  return api.get('/api/users/addresses').then((res) => res.data)
}

export function addAddress(addressData) {
  return api.post('/api/users/addresses', addressData).then((res) => res.data)
}

export function updateAddress(addressId, addressData) {
  return api.put(`/api/users/addresses/${addressId}`, addressData).then((res) => res.data)
}

export function deleteAddress(addressId) {
  return api.delete(`/api/users/addresses/${addressId}`).then((res) => res.data)
}