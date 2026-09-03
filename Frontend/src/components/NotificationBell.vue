<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '../stores/notification.js'

const router = useRouter()
const notificationStore = useNotificationStore()
const dropdownRef = ref(null)
let pollInterval = null

const unreadNotifications = computed(() =>
  notificationStore.notifications.filter((n) => !n.isRead)
)

function formatTime(dateStr) {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / 60000)
  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return diffMins + 'm ago'
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return diffHours + 'h ago'
  return date.toLocaleDateString()
}

function typeLabel(type) {
  if (type === 'PRESCRIPTION_APPROVED') return 'Rx Approved'
  if (type === 'PRESCRIPTION_REJECTED') return 'Rx Rejected'
  if (type === 'CONSULTATION_ACCEPTED') return 'Consultation'
  if (type === 'CONSULTATION_IN_PROGRESS') return 'Consultation'
  if (type === 'CONSULTATION_CREATED') return 'Consultation'
  return type
}

function typeClass(type) {
  if (type === 'PRESCRIPTION_APPROVED') return 'type-approved'
  if (type === 'PRESCRIPTION_REJECTED') return 'type-rejected'
  return 'type-consultation'
}

async function handleBellClick() {
  if (notificationStore.showDropdown) {
    notificationStore.closeDropdown()
  } else {
    await notificationStore.fetchNotifications()
    notificationStore.toggleDropdown()
  }
}

async function handleNotificationClick(notification) {
  if (!notification.isRead) {
    await notificationStore.markNotificationRead(notification.id)
  }
  notificationStore.closeDropdown()
  // Route to the right page based on notification type.
  const type = notification.type
  if (type && type.startsWith('CONSULTATION')) {
    router.push('/consultations')
  } else {
    router.push(`/prescriptions/${notification.referenceId}`)
  }
}

async function handleMarkAllRead() {
  await notificationStore.markAllNotificationsRead()
}

function handleClickOutside(event) {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target)) {
    notificationStore.closeDropdown()
  }
}

onMounted(() => {
  notificationStore.fetchUnreadCount()
  pollInterval = setInterval(() => {
    notificationStore.fetchUnreadCount()
  }, 30000)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="notification-wrapper" ref="dropdownRef">
    <button class="bell-btn" @click.stop="handleBellClick" :title="'Notifications'">
      <span class="bell-icon">&#128276;</span>
      <span v-if="notificationStore.unreadCount > 0" class="badge">
        {{ notificationStore.unreadCount > 9 ? '9+' : notificationStore.unreadCount }}
      </span>
    </button>

    <Transition name="dropdown">
      <div v-if="notificationStore.showDropdown" class="dropdown">
        <div class="dropdown-header">
          <h3>Notifications</h3>
          <button
            v-if="unreadNotifications.length > 0"
            class="mark-all-btn"
            @click="handleMarkAllRead"
          >
            Mark all read
          </button>
        </div>

        <div v-if="notificationStore.loading" class="dropdown-loading">
          <div class="small-spinner"></div>
        </div>

        <div v-else-if="notificationStore.notifications.length === 0" class="dropdown-empty">
          <p>No notifications yet</p>
        </div>

        <div v-else class="notification-list">
          <div
            v-for="notification in notificationStore.notifications"
            :key="notification.id"
            class="notification-item"
            :class="{ unread: !notification.isRead }"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-dot" :class="typeClass(notification.type)"></div>
            <div class="notification-content">
              <div class="notification-header">
                <span class="notification-title">{{ notification.title }}</span>
                <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
              </div>
              <p class="notification-message">{{ notification.message }}</p>
              <span class="notification-type" :class="typeClass(notification.type)">
                {{ typeLabel(notification.type) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>
