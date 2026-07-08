<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '../stores/notification.js'

const props = defineProps({
  userId: { type: Number, required: true },
})

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
  return type === 'PRESCRIPTION_APPROVED' ? 'Approved' : 'Rejected'
}

function typeClass(type) {
  return type === 'PRESCRIPTION_APPROVED' ? 'type-approved' : 'type-rejected'
}

async function handleBellClick() {
  if (notificationStore.showDropdown) {
    notificationStore.closeDropdown()
  } else {
    await notificationStore.fetchNotifications(props.userId)
    notificationStore.toggleDropdown()
  }
}

async function handleNotificationClick(notification) {
  if (!notification.isRead) {
    await notificationStore.markNotificationRead(notification.id, props.userId)
  }
  notificationStore.closeDropdown()
  router.push(`/prescriptions/${notification.referenceId}`)
}

async function handleMarkAllRead() {
  await notificationStore.markAllNotificationsRead(props.userId)
}

function handleClickOutside(event) {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target)) {
    notificationStore.closeDropdown()
  }
}

onMounted(() => {
  notificationStore.fetchUnreadCount(props.userId)
  pollInterval = setInterval(() => {
    notificationStore.fetchUnreadCount(props.userId)
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


