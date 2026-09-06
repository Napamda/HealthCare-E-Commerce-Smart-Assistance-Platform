import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getUnreadCount,
  getNotifications,
  markAsRead,
  markAllAsRead,
} from '../services/notification.js'

// Local toast ids (server notifications come with their own id).
let toastSeq = 0

export const useNotificationStore = defineStore('notification', () => {
  // ---- Server notifications (NotificationBell / dropdown) ----
  const notifications = ref([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const error = ref(null)
  const showDropdown = ref(false)

  async function fetchUnreadCount() {
    try {
      const result = await getUnreadCount()
      unreadCount.value = result.count ?? result.unreadCount ?? 0
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

  function toggleDropdown() {
    showDropdown.value = !showDropdown.value
  }

  function closeDropdown() {
    showDropdown.value = false
  }

  // ---- Local toasts (NotificationContainer) ----
  const toasts = ref([])

  function removeNotification(id) {
    const idx = toasts.value.findIndex((t) => t.id === id)
    if (idx !== -1) {
      toasts.value.splice(idx, 1)
    }
  }

  function clearAll() {
    toasts.value = []
  }

  function addNotification({ type = 'info', title = '', message = '', duration = 5000 } = {}) {
    const id = ++toastSeq
    toasts.value.push({ id, type, title, message })
    if (duration && duration > 0) {
      setTimeout(() => removeNotification(id), duration)
    }
    return { id, type, title, message }
  }

  function success(title, message, options = {}) {
    return addNotification({ type: 'success', title, message, ...options })
  }

  function warning(title, message, options = {}) {
    return addNotification({ type: 'warning', title, message, ...options })
  }

  function info(title, message, options = {}) {
    return addNotification({ type: 'info', title, message, ...options })
  }

  return {
    notifications,
    unreadCount,
    loading,
    error,
    showDropdown,
    fetchUnreadCount,
    fetchNotifications,
    markNotificationRead,
    markAllNotificationsRead,
    toggleDropdown,
    closeDropdown,
    toasts,
    addNotification,
    removeNotification,
    clearAll,
    success,
    warning,
    info,
  }
})
