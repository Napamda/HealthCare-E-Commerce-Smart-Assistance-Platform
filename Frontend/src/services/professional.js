import apiClient from './api.js'

export function listProfessionals(specialty) {
  return apiClient
    .get('/api/professionals', { params: { specialty: specialty || undefined } })
    .then((res) => res.data)
}

export function listSpecialties() {
  return apiClient.get('/api/professionals/specialties').then((res) => res.data)
}

export function searchNearby({ lat, lng, radius, specialty }) {
  return apiClient
    .get('/api/professionals/nearby', {
      params: { lat, lng, radius, specialty: specialty || undefined },
    })
    .then((res) => res.data)
}

export function getProfessional(id) {
  return apiClient.get(`/api/professionals/${id}`).then((res) => res.data)
}

export function createProfessional(data) {
  return apiClient.post('/api/professionals', data).then((res) => res.data)
}

export function updateProfessional(id, data) {
  return apiClient.put(`/api/professionals/${id}`, data).then((res) => res.data)
}

export function deleteProfessional(id) {
  return apiClient.delete(`/api/professionals/${id}`)
}
