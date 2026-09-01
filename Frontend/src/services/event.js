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

export function listEventCities() {
  return apiClient
    .get('/api/events/cities')
    .then((res) => res.data)
}

export function searchEvents(params = {}) {
  return apiClient
    .get('/api/events/search', { params })
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

export function registerForEvent(eventId) {
  return apiClient
    .post('/api/event-registrations', { eventId })
    .then((res) => res.data)
}

export function listMyRegistrations() {
  return apiClient
    .get('/api/event-registrations/my')
    .then((res) => res.data)
}

export function getEventRegistrationStatus(eventId) {
  return apiClient
    .get(`/api/event-registrations/event/${eventId}`)
    .then((res) => res.data)
}

export function cancelRegistration(id) {
  return apiClient
    .delete(`/api/event-registrations/${id}`)
    .then((res) => res.data)
}
