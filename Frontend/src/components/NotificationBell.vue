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
  const labels = {
    PRESCRIPTION_APPROVED: 'Rx Approved',
    PRESCRIPTION_REJECTED: 'Rx Rejected',
    CONSULTATION_ACCEPTED: 'Consultation',
    CONSULTATION_IN_PROGRESS: 'Consultation',
    CONSULTATION_CREATED: 'Consultation',
    ORDER_CREATED: 'Order',
    ORDER_SHIPPED: 'Order Shipped',
    ORDER_DELIVERED: 'Order Delivered',
    PAYMENT_SUCCESS: 'Payment',
    PAYMENT_FAILED: 'Payment Failed',
    EVENT_REGISTRATION: 'Event',
    EVENT_REMINDER: 'Event Reminder',
    USER_REGISTERED: 'Welcome',
    WELCOME: 'Welcome',
    STOCK_LOW: 'Low Stock',
    STOCK_OUT: 'Out of Stock'
  }
  return labels[type] || type
}

function typeClass(type) {
  if (type === 'PRESCRIPTION_APPROVED') return 'type-approved'
  if (type === 'PRESCRIPTION_REJECTED') return 'type-rejected'
  if (type?.includes('ORDER')) return 'type-order'
  if (type?.includes('PAYMENT')) return 'type-payment'
  if (type?.includes('CONSULTATION')) return 'type-consultation'
  if (type?.includes('EVENT')) return 'type-event'
  return 'type-other'
}

async function handleBellClick() {
  if (notificationStore.showDropdown) {
    notificationStore.closeDropdown()
  } else {
    await notificationStore.fetchNotifications()
    notificationStore.toggleDropdown()
  }
}

// ✅ IMPROVED: Click handler with proper routing
async function handleNotificationClick(notification) {
  // Mark as read first
  if (!notification.isRead) {
    await notificationStore.markNotificationRead(notification.id)
  }
  
  notificationStore.closeDropdown()
  
  const type = notification.type
  const referenceId = notification.referenceId
  
  // Define routing logic
  const routes = {
    // Prescription related
    PRESCRIPTION_APPROVED: () => router.push(`/prescriptions/${referenceId}`),
    PRESCRIPTION_REJECTED: () => router.push(`/prescriptions/${referenceId}`),
    
    // Consultation related
    CONSULTATION_ACCEPTED: () => router.push('/consultations'),
    CONSULTATION_IN_PROGRESS: () => router.push('/consultations'),
    CONSULTATION_CREATED: () => router.push('/consultations'),
    
    // Order related
    ORDER_CREATED: () => router.push(`/orders/${referenceId}`),
    ORDER_SHIPPED: () => router.push(`/orders/${referenceId}`),
    ORDER_DELIVERED: () => router.push(`/orders/${referenceId}`),
    
    // Payment related
    PAYMENT_SUCCESS: () => router.push(`/orders/${referenceId}`),
    PAYMENT_FAILED: () => router.push('/checkout'),
    
    // Event related
    EVENT_REGISTRATION: () => router.push(`/events/${referenceId}`),
    EVENT_REMINDER: () => router.push(`/events/${referenceId}`),
    
    // User related
    USER_REGISTERED: () => router.push('/profile'),
    WELCOME: () => router.push('/products'),
    
    // Inventory related
    STOCK_LOW: () => router.push('/vendor/inventory'),
    STOCK_OUT: () => router.push('/vendor/inventory'),
    
    // Default fallback
    DEFAULT: () => {
      // Try to navigate based on type pattern
      if (referenceId) {
        if (type?.includes('PRESCRIPTION')) {
          router.push(`/prescriptions/${referenceId}`)
        } else if (type?.includes('ORDER') || type?.includes('PAYMENT')) {
          router.push(`/orders/${referenceId}`)
        } else if (type?.includes('CONSULTATION')) {
          router.push('/consultations')
        } else if (type?.includes('EVENT')) {
          router.push(`/events/${referenceId}`)
        } else {
          router.push('/notifications')
        }
      } else {
        router.push('/notifications')
      }
    }
  }
  
  // Execute the appropriate route function
  const routeFn = routes[type] || routes.DEFAULT
  routeFn()
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

<style scoped>
/* Your existing styles... */

.notification-item {
  cursor: pointer;
  transition: background 0.15s ease;
}

.notification-item:hover {
  background: rgba(0, 0, 0, 0.03);
}

.type-order { background: #4a90d9; }
.type-payment { background: #27ae60; }
.type-event { background: #8e44ad; }
.type-other { background: #7f8c8d; }
</style>