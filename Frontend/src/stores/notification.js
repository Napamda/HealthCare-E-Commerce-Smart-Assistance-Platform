import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])

  async function fetchUnreadCount() {
    try {
      const result = await getUnreadCount()
      unreadCount.value = result.count
    } catch (e) {
      console.error('Failed to fetch unread count:', e)
    }
  }

  async function fetchNotifications() {
    loading.value = true
    error.value = null
    try {
      notifications.value = await getNotifications()
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to load notifications'
    } finally {
      loading.value = false
    }
  }

  async function markNotificationRead(notificationId) {
    try {
      await markAsRead(notificationId)
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      const idx = notifications.value.findIndex((n) => n.id === notificationId)
      if (idx !== -1) {
        notifications.value[idx].isRead = true
      }
    } catch (e) {
      console.error('Failed to mark notification as read:', e)
    }
  }

  async function markAllNotificationsRead() {
    try {
      await markAllAsRead()
      notifications.value.forEach((n) => (n.isRead = true))
      unreadCount.value = 0
    } catch (e) {
      console.error('Failed to mark all notifications as read:', e)
    }
  }

  function warning(title, message, options = {}) {
    return addNotification({ type: 'warning', title, message, ...options })
  }

  function info(title, message, options = {}) {
    return addNotification({ type: 'info', title, message, ...options })
  }

  return {
    notifications,
    addNotification,
    removeNotification,
    clearAll,
    success,
    error,
    warning,
    info,
  }
})