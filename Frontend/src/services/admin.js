import apiClient from './api.js'

// ---- Admin Task 3.1 — User Management ----

export function getUsers(params = {}) {
  return apiClient.get('/api/admin/users', { params }).then((res) => res.data)
}

export function setUserStatus(userId, status, reason = null) {
  return apiClient
    .patch(`/api/admin/users/${userId}/status`, { status, reason })
    .then((res) => res.data)
}

export function changeUserRole(userId, role) {
  return apiClient
    .patch(`/api/admin/users/${userId}/role`, { role })
    .then((res) => res.data)
}

export function getUserActivity(userId, params = {}) {
  return apiClient
    .get(`/api/admin/users/${userId}/activity`, { params })
    .then((res) => res.data)
}

export function getRecentActivity(params = {}) {
  return apiClient.get('/api/admin/activity', { params }).then((res) => res.data)
}

// ---- Task 3.2 — Vendor Management ----

export function getVendors(status = null) {
  const params = status ? { status } : {}
  return apiClient.get('/api/admin/moderation/vendors', { params }).then((res) => res.data)
}

export function decideOnVendor(profileId, decision, reason = null) {
  return apiClient
    .patch(`/api/admin/moderation/vendors/${profileId}`, { decision, reason })
    .then((res) => res.data)
}

// ---- Task 3.3 — Product Moderation ----

export function getProductsForModeration(status = null) {
  const params = status ? { status } : {}
  return apiClient.get('/api/admin/moderation/products', { params }).then((res) => res.data)
}

export function decideOnProduct(productId, decision, reason = null) {
  return apiClient
    .patch(`/api/admin/moderation/products/${productId}`, { decision, reason })
    .then((res) => res.data)
}

// ---- Task 3.4 — Event Moderation ----

export function getEventsForModeration(status = null) {
  const params = status ? { status } : {}
  return apiClient.get('/api/admin/moderation/events', { params }).then((res) => res.data)
}

export function decideOnEvent(eventId, decision, reason = null) {
  return apiClient
    .patch(`/api/admin/moderation/events/${eventId}`, { decision, reason })
    .then((res) => res.data)
}

// ---- Task 3.5 — Admin Dashboard ----

export function getDashboardStats() {
  return apiClient.get('/api/admin/dashboard/stats').then((res) => res.data)
}
