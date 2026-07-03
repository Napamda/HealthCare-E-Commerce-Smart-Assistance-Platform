<script setup>
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useConsultationStore } from '../stores/consultation.js'

const consultationStore = useConsultationStore()
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
  consultationStore.fetchPatientConsultations(1)
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



<style scoped>
.consultations-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.error-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fef2f2;
  color: var(--color-danger);
  font-size: 13px;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
  border: 1px solid #fecaca;
}

.btn-dismiss {
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  background: var(--color-danger);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}

.filters-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-chip {
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  transition: all 0.15s;
}
.filter-chip:hover {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
}
.filter-chip.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-secondary);
}
.empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 16px;
}
.empty-state h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}
.empty-state p {
  font-size: 14px;
}

.consultations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.consultation-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  transition: box-shadow 0.15s;
}
.consultation-card:hover {
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.card-id {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.card-status {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
}
.status-pending {
  background: #fef3c7;
  color: #b45309;
}
.status-accepted {
  background: #d1fae5;
  color: #065f46;
}
.status-in_progress {
  background: #dbeafe;
  color: #1e40af;
}
.status-closed {
  background: #f1f5f9;
  color: #475569;
}
.status-rejected {
  background: #fee2e2;
  color: #991b1b;
}

.card-body {
  margin-bottom: 10px;
}

.card-reason {
  display: flex;
  gap: 6px;
  font-size: 14px;
}
.card-reason .label {
  font-weight: 500;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}
.card-reason .value {
  color: var(--color-text);
}

.card-footer {
  border-top: 1px solid var(--color-border);
  padding-top: 10px;
}

.footer-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 12px;
  color: var(--color-text-muted);
}

.priority-badge {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.priority-low {
  background: #e0e7ff;
  color: #4f46e5;
}
.priority-normal {
  background: #dbeafe;
  color: #2563eb;
}
.priority-high {
  background: #fef3c7;
  color: #d97706;
}
.priority-urgent {
  background: #fecaca;
  color: #dc2626;
}
</style>
