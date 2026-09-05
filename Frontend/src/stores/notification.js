import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getUnreadCount,
  getNotifications,
  markAsRead,
  markAllAsRead,
} from '../services/notification.js'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const error = ref(null)
  const showDropdown = ref(false)

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

  function toggleDropdown() {
    showDropdown.value = !showDropdown.value
  }

  function closeDropdown() {
    showDropdown.value = false
  }

  function clearError() {
    error.value = null
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
    clearError,
  }
})
