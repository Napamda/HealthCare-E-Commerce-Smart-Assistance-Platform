import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])

  function addNotification(notification) {
    const id = Date.now()
    const newNotification = {
      id,
      type: notification.type || 'info', // success, error, warning, info
      title: notification.title || '',
      message: notification.message || '',
      duration: notification.duration || 5000,
      persistent: notification.persistent || false,
    }
    notifications.value.push(newNotification)

    if (!newNotification.persistent && newNotification.duration > 0) {
      setTimeout(() => {
        removeNotification(id)
      }, newNotification.duration)
    }

    return id
  }

  function removeNotification(id) {
    const index = notifications.value.findIndex(n => n.id === id)
    if (index !== -1) {
      notifications.value.splice(index, 1)
    }
  }

  function clearAll() {
    notifications.value = []
  }

  // Convenience methods
  function success(title, message, options = {}) {
    return addNotification({ type: 'success', title, message, ...options })
  }

  function error(title, message, options = {}) {
    return addNotification({ type: 'error', title, message, ...options })
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