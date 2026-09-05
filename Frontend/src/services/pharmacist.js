import apiClient from './api.js'

export function getPharmacistProfile() {
  return apiClient.get('/api/pharmacist/profile').then((res) => res.data)
}

export function createPharmacistProfile(data) {
  return apiClient.post('/api/pharmacist/profile', data).then((res) => res.data)
}

export function updatePharmacistProfile(data) {
  return apiClient.put('/api/pharmacist/profile', data).then((res) => res.data)
}