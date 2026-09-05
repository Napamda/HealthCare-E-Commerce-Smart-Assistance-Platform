import apiClient from './api.js'

export function getDoctorProfile() {
  return apiClient.get('/api/doctor/profile').then((res) => res.data)
}

export function createDoctorProfile(data) {
  return apiClient.post('/api/doctor/profile', data).then((res) => res.data)
}

export function updateDoctorProfile(data) {
  return apiClient.put('/api/doctor/profile', data).then((res) => res.data)
}