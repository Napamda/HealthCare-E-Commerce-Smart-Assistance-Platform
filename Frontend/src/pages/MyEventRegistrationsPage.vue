<template>
  <div class="page-wrap">
    <div class="page-header">
      <h1 class="page-title">My Event Registrations</h1>
      <router-link to="/events" class="btn-link">&larr; Browse Events</router-link>
    </div>

    <p v-if="error" class="alert alert-error">{{ error }}</p>
    <p v-if="success" class="alert alert-success">{{ success }}</p>

    <div v-if="loading" class="loading-state">Loading registrations...</div>

    <div v-else-if="registrations.length === 0" class="empty-state">
      You have not registered for any events yet.
      <router-link to="/events" class="btn-link empty-link">Find an event</router-link>
    </div>

    <div v-else class="registrations-list">
      <div v-for="reg in registrations" :key="reg.id" class="registration-item">
        <div class="registration-item-info">
          <div class="event-card-top">
            <span class="category-badge">{{ reg.category }}</span>
            <span class="registered-badge">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 6 9 17l-5-5" />
              </svg>
              Confirmed
            </span>
            <span
              v-if="reg.volunteerRole"
              class="volunteer-badge"
              :class="'volunteer-' + reg.volunteerRole.toLowerCase()"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z" />
              </svg>
              Volunteer · {{ reg.volunteerRole }}
            </span>
          </div>
          <router-link :to="`/events/${reg.eventId}`" class="registration-item-title">
            {{ reg.eventTitle }}
          </router-link>
          <div class="event-meta">
            <span v-if="reg.startDateTime" class="event-meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
              </svg>
              {{ formatDateTime(reg.startDateTime) }}
            </span>
            <span v-if="reg.venue || reg.city" class="event-meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" /><circle cx="12" cy="10" r="3" />
              </svg>
              {{ reg.venue }}{{ reg.venue && reg.city ? ', ' : '' }}{{ reg.city }}
            </span>
            <span v-if="reg.organizer" class="event-meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              </svg>
              {{ reg.organizer }}
            </span>
          </div>
          <p v-if="reg.registeredAt" class="registration-date">
            Registered on {{ formatDate(reg.registeredAt) }}
          </p>
        </div>
        <div class="registration-item-actions">
          <router-link :to="`/events/${reg.eventId}`" class="btn-secondary">View event</router-link>
          <button
            class="btn-danger"
            :disabled="cancellingId === reg.id"
            @click="handleCancel(reg)"
          >
            {{ cancellingId === reg.id ? 'Cancelling...' : 'Cancel' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listMyRegistrations, cancelRegistration } from '../services/event.js'

const registrations = ref([])
const loading = ref(false)
const error = ref('')
const success = ref('')
const cancellingId = ref(null)

async function loadRegistrations() {
  loading.value = true
  error.value = ''
  try {
    registrations.value = await listMyRegistrations()
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load registrations'
  } finally {
    loading.value = false
  }
}

async function handleCancel(reg) {
  if (!window.confirm(`Cancel your registration for "${reg.eventTitle}"?`)) return
  cancellingId.value = reg.id
  success.value = ''
  error.value = ''
  try {
    await cancelRegistration(reg.id)
    success.value = 'Registration cancelled'
    await loadRegistrations()
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to cancel registration'
  } finally {
    cancellingId.value = null
  }
}

function formatDateTime(dt) {
  if (!dt) return '—'
  const date = new Date(dt)
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatDate(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

onMounted(loadRegistrations)
</script>
