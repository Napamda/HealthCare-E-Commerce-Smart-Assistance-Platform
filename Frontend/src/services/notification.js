import apiClient from './api.js'

// All endpoints derive userId from the auth token — no client-supplied id.
export function getNotifications() {
  return apiClient.get('/api/notifications/mine').then((res) => res.data)
}

export function getUnreadNotifications() {
  return apiClient.get('/api/notifications/mine/unread').then((res) => res.data)
}

export function getUnreadCount() {
  return apiClient.get('/api/notifications/mine/unread-count').then((res) => res.data)
}

export function markAsRead(notificationId) {
  return apiClient.patch(`/api/notifications/${notificationId}/read`).then((res) => res.data)
}

export function markAllAsRead() {
  return apiClient.patch('/api/notifications/mine/read-all').then((res) => res.data)
}
