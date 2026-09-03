<template>
  <div class="page-wrap">
    <div class="event-detail-header">
      <router-link to="/events" class="btn-link event-detail-back">&larr; Back to Events</router-link>
    </div>

    <p v-if="error" class="alert alert-error">{{ error }}</p>

    <div v-if="loading" class="loading-state">Loading event...</div>

    <div v-else-if="event" class="event-detail">
      <div class="event-card-top">
        <span class="category-badge">{{ event.category }}</span>
        <span class="status-badge" :class="`status-${event.status.toLowerCase()}`">{{ event.status }}</span>
      </div>

      <h1 class="event-detail-title">{{ event.title }}</h1>

      <div class="event-detail-meta">
        <span v-if="event.startDateTime" class="event-meta-item">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
          </svg>
          {{ formatDateTime(event.startDateTime) }}
          <template v-if="event.endDateTime"> — {{ formatDateTime(event.endDateTime) }}</template>
        </span>
        <span v-if="event.venue || event.city" class="event-meta-item">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" /><circle cx="12" cy="10" r="3" />
          </svg>
          {{ event.venue }}{{ event.venue && event.city ? ', ' : '' }}{{ event.city }}
        </span>
        <span v-if="event.organizer" class="event-meta-item">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" />
          </svg>
          Organized by {{ event.organizer }}
        </span>
        <span v-if="event.capacity" class="event-meta-item">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
          {{ spotsLeftText }}
        </span>
      </div>

      <p v-if="event.description" class="event-detail-desc">{{ event.description }}</p>

      <div v-if="event.tags && event.tags.length" class="tag-list">
        <span v-for="tag in event.tags" :key="tag" class="tag-chip">{{ tag }}</span>
      </div>

      <div class="event-registration-card">
        <h3 class="event-registration-title">Registration</h3>

        <p v-if="regError" class="alert alert-error">{{ regError }}</p>
        <p v-if="regSuccess" class="alert alert-success">{{ regSuccess }}</p>

        <div class="capacity-info">
          <span>{{ regStatus ? regStatus.count : '—' }} / {{ event.capacity || '∞' }} spots filled</span>
          <span v-if="spotsLeft !== null" class="capacity-remaining">{{ spotsLeft }} left</span>
        </div>
        <div class="capacity-bar">
          <div class="capacity-fill" :class="capacityTone" :style="capacityStyle"></div>
        </div>

        <div class="registration-actions">
          <template v-if="!authStore.isAuthenticated">
            <router-link :to="`/login?redirect=/events/${event.id}`" class="btn-primary">Sign in to register</router-link>
          </template>

          <template v-else-if="regStatus && regStatus.registered">
            <span class="registered-badge">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 6 9 17l-5-5" />
              </svg>
              You are registered
            </span>
            <span v-if="regStatus.volunteerRole" class="volunteer-badge" :class="'volunteer-' + regStatus.volunteerRole.toLowerCase()">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z" />
              </svg>
              Volunteer · {{ regStatus.volunteerRole }}
            </span>
            <button class="btn-danger" :disabled="actionLoading" @click="handleCancel">
              {{ actionLoading ? 'Cancelling...' : 'Cancel registration' }}
            </button>
          </template>

          <template v-else>
            <button
              class="btn-primary"
              :disabled="actionLoading || !canRegister"
              @click="handleRegister"
            >
              {{ actionLoading ? 'Registering...' : registerCtaLabel }}
            </button>
            <p v-if="volunteerEligibleRole" class="registration-hint volunteer-hint">
              You will be registered as a <strong>{{ volunteerEligibleRole }}</strong> volunteer for this event.
            </p>
            <p v-else-if="!canRegister" class="registration-hint">{{ registerHint }}</p>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getEvent, registerForEvent, getEventRegistrationStatus, cancelRegistration } from '../services/event.js'
import { useAuthStore } from '../stores/auth.js'

const route = useRoute()
const authStore = useAuthStore()
let regPollTimer = null

const event = ref(null)
const loading = ref(false)
const error = ref('')
const regStatus = ref(null)
const regLoading = ref(false)
const actionLoading = ref(false)
const regError = ref('')
const regSuccess = ref('')

const spotsLeft = computed(() => {
  if (!regStatus.value || !regStatus.value.capacity) return null
  return Math.max(regStatus.value.capacity - regStatus.value.count, 0)
})

const spotsLeftText = computed(() => {
  if (!event.value?.capacity) return null
  const count = regStatus.value?.count ?? event.value.registeredCount ?? 0
  const left = Math.max(event.value.capacity - count, 0)
  return left === 0 ? 'Event full' : `${left} spots left`
})

const capacityTone = computed(() => {
  if (!regStatus.value || !regStatus.value.capacity) return 'capacity-fill-low'
  const ratio = regStatus.value.count / regStatus.value.capacity
  if (ratio >= 1) return 'capacity-fill-full'
  if (ratio >= 0.75) return 'capacity-fill-high'
  return 'capacity-fill-low'
})

const capacityStyle = computed(() => {
  if (!regStatus.value || !regStatus.value.capacity) return { width: '0%' }
  return { width: `${Math.min((regStatus.value.count / regStatus.value.capacity) * 100, 100)}%` }
})

const isPast = computed(() => {
  if (!event.value?.startDateTime) return false
  return new Date(event.value.startDateTime).getTime() < Date.now()
})

const isPublished = computed(() => event.value?.status === 'PUBLISHED')

const canRegister = computed(() => {
  if (!regStatus.value) return false
  return isPublished.value && !isPast.value && !regStatus.value.registered && spotsLeft.value !== 0
})

const registerHint = computed(() => {
  if (!isPublished.value) return 'This event is not open for registration'
  if (isPast.value) return 'This event has already started'
  if (spotsLeft.value === 0) return 'This event is full'
  return ''
})

// Doctors and pharmacists automatically register as volunteers (their role is
// surfaced as a badge). Patients/vendors see the normal CTA.
const volunteerEligibleRole = computed(() => {
  const role = authStore.userRole
  if (role === 'DOCTOR' || role === 'PHARMACIST') return role
  return null
})

const registerCtaLabel = computed(() => {
  if (volunteerEligibleRole.value) {
    return `Register as ${volunteerEligibleRole.value} Volunteer`
  }
  return 'Register for this event'
})

async function loadEvent() {
  loading.value = true
  error.value = ''
  try {
    event.value = await getEvent(route.params.id)
    if (authStore.isAuthenticated) {
      loadRegistrationStatus()
    }
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load event'
  } finally {
    loading.value = false
  }
}

async function loadRegistrationStatus() {
  regLoading.value = true
  regError.value = ''
  try {
    regStatus.value = await getEventRegistrationStatus(route.params.id)
    if (event.value) event.value.registeredCount = regStatus.value.count
  } catch (e) {
    regError.value = e.response?.data?.error || 'Failed to load registration status'
  } finally {
    regLoading.value = false
  }
}

async function refreshRegistrationStatus() {
  try {
    regStatus.value = await getEventRegistrationStatus(route.params.id)
    if (event.value) event.value.registeredCount = regStatus.value.count
  } catch (_) {}
}

async function handleRegister() {
  actionLoading.value = true
  regError.value = ''
  regSuccess.value = ''
  try {
    await registerForEvent(event.value.id)
    regSuccess.value = 'You have successfully registered for this event'
    await loadRegistrationStatus()
  } catch (e) {
    regError.value = e.response?.data?.error || 'Failed to register for event'
  } finally {
    actionLoading.value = false
  }
}

async function handleCancel() {
  if (!window.confirm('Cancel your registration for this event?')) return
  actionLoading.value = true
  regError.value = ''
  regSuccess.value = ''
  try {
    await cancelRegistration(regStatus.value.registrationId)
    regSuccess.value = 'Your registration has been cancelled'
    await loadRegistrationStatus()
  } catch (e) {
    regError.value = e.response?.data?.error || 'Failed to cancel registration'
  } finally {
    actionLoading.value = false
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

onMounted(() => {
  loadEvent()
  regPollTimer = setInterval(() => {
    if (authStore.isAuthenticated && event.value && !actionLoading.value) {
      refreshRegistrationStatus()
    }
  }, 10000)
})

onUnmounted(() => {
  if (regPollTimer) clearInterval(regPollTimer)
})
</script>
