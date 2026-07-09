<script setup>
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useConsultationStore } from '../stores/consultation.js'
import { useAuthStore } from '../stores/auth.js'

const consultationStore = useConsultationStore()
const authStore = useAuthStore()
const { consultations, escalationError } = storeToRefs(consultationStore)

const statusFilter = ref('ALL')

const statusOptions = ['ALL', 'PENDING', 'ACCEPTED', 'IN_PROGRESS', 'CLOSED', 'REJECTED']

const filteredConsultations = computed(() => {
  if (statusFilter.value === 'ALL') return consultations.value
  return consultations.value.filter((c) => c.status === statusFilter.value)
})

const sortedConsultations = computed(() =>
  [...filteredConsultations.value].sort(
    (a, b) => new Date(b.createdAt) - new Date(a.createdAt),
  ),
)

onMounted(() => {
  const userId = authStore.currentUser?.id
  if (userId) {
    consultationStore.fetchPatientConsultations(userId)
  }
})

function getStatusLabel(status) {
  const map = {
    PENDING: 'Pending Review',
    ACCEPTED: 'Accepted',
    REJECTED: 'Rejected',
    IN_PROGRESS: 'In Progress',
    CLOSED: 'Closed',
  }
  return map[status] || status
}

function getPriorityClass(priority) {
  return 'priority-' + (priority || 'normal').toLowerCase()
}

function getStatusClass(status) {
  return 'status-' + (status || 'pending').toLowerCase()
}

function formatDate(isoString) {
  if (!isoString) return ''
  return new Date(isoString).toLocaleDateString([], {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <div class="consultations-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">My Consultations</h1>
        <p class="page-subtitle">Track your escalation requests and consultation status</p>
      </div>
    </div>

    <div v-if="escalationError" class="error-banner">
      <span>{{ escalationError }}</span>
      <button class="btn-dismiss" @click="consultationStore.clearEscalationError()">Dismiss</button>
    </div>

    <div class="filters-bar">
      <button
        v-for="status in statusOptions"
        :key="status"
        class="filter-chip"
        :class="{ active: statusFilter === status }"
        @click="statusFilter = status"
      >
        {{ status === 'ALL' ? 'All' : getStatusLabel(status) }}
      </button>
    </div>

    <div v-if="sortedConsultations.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
      </div>
      <h3>No consultations found</h3>
      <p>When you escalate a chat to a doctor, it will appear here.</p>
    </div>

    <div v-else class="consultations-list">
      <div
        v-for="consultation in sortedConsultations"
        :key="consultation.id"
        class="consultation-card"
      >
        <div class="card-header">
          <div class="card-id">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="4" />
              <line x1="9" y1="9" x2="15" y2="9" />
              <line x1="9" y1="13" x2="13" y2="13" />
            </svg>
            Consultation #{{ consultation.id }}
          </div>
          <span class="card-status" :class="getStatusClass(consultation.status)">
            {{ getStatusLabel(consultation.status) }}
          </span>
        </div>

        <div class="card-body">
          <div class="card-reason">
            <span class="label">Reason:</span>
            <span class="value">{{ consultation.reason }}</span>
          </div>
        </div>

        <div class="card-footer">
          <div class="footer-meta">
            <span class="meta-item">
              <span class="priority-badge" :class="getPriorityClass(consultation.priority)">
                {{ consultation.priority }}
              </span>
            </span>
            <span class="meta-item">{{ consultation.messageCount }} messages</span>
            <span class="meta-item">{{ formatDate(consultation.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>




