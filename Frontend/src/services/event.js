import apiClient from './api.js'

export function listEvents(params = {}) {
  return apiClient
    .get('/api/events', { params })
    .then((res) => res.data)
}

export function listEventCategories() {
  return apiClient
    .get('/api/events/categories')
    .then((res) => res.data)
}

export function getEvent(id) {
  return apiClient
    .get(`/api/events/${id}`)
    .then((res) => res.data)
}

export function createEvent(payload) {
  return apiClient
    .post('/api/events', payload)
    .then((res) => res.data)
}

export function updateEvent(id, payload) {
  return apiClient
    .put(`/api/events/${id}`, payload)
    .then((res) => res.data)
}

export function deleteEvent(id) {
  return apiClient
    .delete(`/api/events/${id}`)
    .then((res) => res.data)
}
