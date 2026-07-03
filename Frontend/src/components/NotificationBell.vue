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

<style scoped>
.notification-wrapper {
  position: relative;
}

.bell-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  transition: background 0.15s ease;
}

.bell-btn:hover {
  background: var(--color-bg);
}

.bell-icon {
  font-size: 20px;
}

.badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-danger);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 380px;
  max-height: 480px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}

.dropdown-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.mark-all-btn {
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 500;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}

.mark-all-btn:hover {
  background: var(--color-primary-bg);
}

.dropdown-loading,
.dropdown-empty {
  padding: 40px 20px;
  text-align: center;
}

.small-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.dropdown-empty p {
  color: var(--color-text-muted);
  font-size: 14px;
}

.notification-list {
  overflow-y: auto;
  max-height: 400px;
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.1s ease;
  border-bottom: 1px solid var(--color-border);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: var(--color-bg);
}

.notification-item.unread {
  background: var(--color-primary-bg);
}

.notification-item.unread:hover {
  background: #e0edff;
}

.notification-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  background: var(--color-text-muted);
}

.notification-dot.type-approved {
  background: var(--color-success);
}

.notification-dot.type-rejected {
  background: var(--color-danger);
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.notification-time {
  font-size: 11px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.notification-message {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-type {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.type-approved {
  color: var(--color-success);
}

.type-rejected {
  color: var(--color-danger);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
