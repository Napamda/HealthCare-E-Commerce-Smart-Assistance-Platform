import apiClient from './api.js'

export function getNotifications(userId) {
  return apiClient.get(`/api/notifications/${userId}`).then((res) => res.data)
}

export function getUnreadNotifications(userId) {
  return apiClient.get(`/api/notifications/${userId}/unread`).then((res) => res.data)
}

export function getUnreadCount(userId) {
  return apiClient.get(`/api/notifications/${userId}/unread-count`).then((res) => res.data)
}

export function markAsRead(notificationId) {
  return apiClient.patch(`/api/notifications/${notificationId}/read`).then((res) => res.data)
}

export function markAllAsRead(userId) {
  return apiClient.patch(`/api/notifications/${userId}/read-all`).then((res) => res.data)
}
