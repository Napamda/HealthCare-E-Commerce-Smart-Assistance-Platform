<script setup>
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useConsultationStore } from '../stores/consultation.js'

const consultationStore = useConsultationStore()
const { doctorQueue, escalationError } = storeToRefs(consultationStore)

const statusFilter = ref('ALL')
const priorityFilter = ref('ALL')
const actionLoading = ref(null)

const statusLabels = {
  PENDING: 'Pending Review',
  ACCEPTED: 'Accepted',
  IN_PROGRESS: 'In Progress',
  REJECTED: 'Rejected',
  CLOSED: 'Closed',
}

const priorityLabels = {
  URGENT: 'Urgent',
  HIGH: 'High',
  NORMAL: 'Normal',
  LOW: 'Low',
}

const filteredQueue = computed(() => {
  let list = doctorQueue.value
  if (statusFilter.value !== 'ALL') {
    list = list.filter((c) => c.status === statusFilter.value)
  }
  if (priorityFilter.value !== 'ALL') {
    list = list.filter((c) => c.priority === priorityFilter.value)
  }
  return list.sort((a, b) => {
    const priorityOrder = { URGENT: 0, HIGH: 1, NORMAL: 2, LOW: 3 }
    const pa = priorityOrder[a.priority] ?? 2
    const pb = priorityOrder[b.priority] ?? 2
    if (pa !== pb) return pa - pb
    return new Date(a.createdAt) - new Date(b.createdAt)
  })
})

const stats = computed(() => {
  const total = doctorQueue.value.length
  const pending = doctorQueue.value.filter((c) => c.status === 'PENDING').length
  const active = doctorQueue.value.filter((c) =>
    ['ACCEPTED', 'IN_PROGRESS'].includes(c.status),
  ).length
  const urgent = doctorQueue.value.filter((c) => c.priority === 'URGENT').length
  return { total, pending, active, urgent }
})

onMounted(() => {
  consultationStore.fetchDoctorQueue()
})

async function handleAccept(id) {
  actionLoading.value = id
  try {
    await consultationStore.updateStatus(id, 'ACCEPTED')
  } finally {
    actionLoading.value = null
  }
}

async function handleReject(id) {
  actionLoading.value = id
  try {
    await consultationStore.updateStatus(id, 'REJECTED')
  } finally {
    actionLoading.value = null
  }
}

async function handleClose(id) {
  actionLoading.value = id
  try {
    await consultationStore.updateStatus(id, 'CLOSED')
  } finally {
    actionLoading.value = null
  }
}

async function handleStartProgress(id) {
  actionLoading.value = id
  try {
    await consultationStore.updateStatus(id, 'IN_PROGRESS')
  } finally {
    actionLoading.value = null
  }
}

async function handlePriorityChange(id, priority) {
  await consultationStore.updatePriority(id, priority)
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
  <div class="doctor-dashboard">
    <div class="page-header">
      <div>
        <h1 class="page-title">Doctor Dashboard</h1>
        <p class="page-subtitle">Manage patient consultation requests</p>
      </div>
      <button class="btn-refresh" @click="consultationStore.fetchDoctorQueue()" :disabled="actionLoading">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="23 4 23 10 17 10" />
          <polyline points="1 20 1 14 7 14" />
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
        </svg>
        Refresh
      </button>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-number">{{ stats.total }}</div>
        <div class="stat-label">Total Active</div>
      </div>
      <div class="stat-card stat-pending">
        <div class="stat-number">{{ stats.pending }}</div>
        <div class="stat-label">Pending</div>
      </div>
      <div class="stat-card stat-active">
        <div class="stat-number">{{ stats.active }}</div>
        <div class="stat-label">In Progress</div>
      </div>
      <div class="stat-card stat-urgent">
        <div class="stat-number">{{ stats.urgent }}</div>
        <div class="stat-label">Urgent</div>
      </div>
    </div>

    <div v-if="escalationError" class="error-banner">
      <span>{{ escalationError }}</span>
      <button class="btn-dismiss" @click="consultationStore.clearEscalationError()">Dismiss</button>
    </div>

    <div class="filters-bar">
      <span class="filter-label">Status:</span>
      <button
        v-for="status in ['ALL', 'PENDING', 'ACCEPTED', 'IN_PROGRESS']"
        :key="status"
        class="filter-chip"
        :class="{ active: statusFilter === status }"
        @click="statusFilter = status"
      >
        {{ status === 'ALL' ? 'All' : statusLabels[status] }}
      </button>
      <span class="filter-label">Priority:</span>
      <button
        v-for="priority in ['ALL', 'URGENT', 'HIGH', 'NORMAL', 'LOW']"
        :key="priority"
        class="filter-chip"
        :class="{ active: priorityFilter === priority }"
        @click="priorityFilter = priority"
      >
        {{ priority === 'ALL' ? 'All' : priorityLabels[priority] }}
      </button>
    </div>

    <div v-if="filteredQueue.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
      </div>
      <h3>No consultation requests</h3>
      <p>Patient consultations escalated from chat will appear here.</p>
    </div>

    <div v-else class="consultations-list">
      <div
        v-for="consultation in filteredQueue"
        :key="consultation.id"
        class="consultation-card"
        :class="getPriorityClass(consultation.priority)"
      >
        <div class="card-header">
          <div class="card-patient">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <span class="patient-name">{{ consultation.patientName || 'Patient #' + consultation.patientUserId }}</span>
          </div>
          <div class="card-badges">
            <select
              class="priority-select"
              :value="consultation.priority"
              @change="handlePriorityChange(consultation.id, ($event.target).value)"
            >
              <option value="LOW">Low</option>
              <option value="NORMAL">Normal</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </select>
            <span class="card-status" :class="getStatusClass(consultation.status)">
              {{ statusLabels[consultation.status] || consultation.status }}
            </span>
          </div>
        </div>

        <div class="card-body">
          <div class="card-reason">
            <span class="label">Reason:</span>
            <span class="value">{{ consultation.reason }}</span>
          </div>
          <div class="card-meta">
            <span class="meta-item">#{{ consultation.id }}</span>
            <span class="meta-item">{{ formatDate(consultation.createdAt) }}</span>
          </div>
        </div>

        <div class="card-actions">
          <template v-if="consultation.status === 'PENDING'">
            <button
              class="btn btn-accept"
              :disabled="actionLoading === consultation.id"
              @click="handleAccept(consultation.id)"
            >
              Accept
            </button>
            <button
              class="btn btn-reject"
              :disabled="actionLoading === consultation.id"
              @click="handleReject(consultation.id)"
            >
              Reject
            </button>
          </template>
          <template v-else-if="consultation.status === 'ACCEPTED'">
            <button
              class="btn btn-primary"
              :disabled="actionLoading === consultation.id"
              @click="handleStartProgress(consultation.id)"
            >
              Start Session
            </button>
          </template>
          <template v-else-if="consultation.status === 'IN_PROGRESS'">
            <button
              class="btn btn-close"
              :disabled="actionLoading === consultation.id"
              @click="handleClose(consultation.id)"
            >
              Close Consultation
            </button>
          </template>
          <span v-if="actionLoading === consultation.id" class="loading-spinner"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.doctor-dashboard {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 4px 0;
}

.page-subtitle {
  color: var(--color-text-secondary);
  margin: 0;
  font-size: 0.9375rem;
}

.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.15s;
}

.btn-refresh:hover {
  background: var(--color-surface-hover);
  color: var(--color-text-primary);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}

.stat-card.stat-pending {
  border-color: #f59e0b33;
  background: #f59e0b08;
}

.stat-card.stat-active {
  border-color: #3b82f633;
  background: #3b82f608;
}

.stat-card.stat-urgent {
  border-color: #ef444433;
  background: #ef444408;
}

.stat-number {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.stat-label {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.error-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 0.875rem;
}

.btn-dismiss {
  background: none;
  border: none;
  color: #991b1b;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.8125rem;
}

.filters-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-right: 4px;
}

.filter-chip {
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 0.8125rem;
  cursor: pointer;
  transition: all 0.15s;
}

.filter-chip.active {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.filter-chip:hover:not(.active) {
  background: var(--color-surface-hover);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-secondary);
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.4;
}

.empty-state h3 {
  margin: 0 0 8px 0;
  font-size: 1.125rem;
  color: var(--color-text-primary);
}

.empty-state p {
  margin: 0;
  font-size: 0.9375rem;
}

.consultations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.consultation-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 16px 20px;
  transition: box-shadow 0.15s;
}

.consultation-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.consultation-card.priority-urgent {
  border-left: 4px solid #ef4444;
}

.consultation-card.priority-high {
  border-left: 4px solid #f59e0b;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.card-patient {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-primary);
}

.patient-name {
  font-weight: 600;
  font-size: 1rem;
}

.card-badges {
  display: flex;
  align-items: center;
  gap: 8px;
}

.priority-select {
  padding: 4px 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  background: var(--color-surface);
  color: var(--color-text-primary);
  cursor: pointer;
}

.card-status {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-pending {
  background: #fef3c7;
  color: #92400e;
}

.status-accepted {
  background: #dbeafe;
  color: #1e40af;
}

.status-in_progress {
  background: #d1fae5;
  color: #065f46;
}

.status-rejected {
  background: #fee2e2;
  color: #991b1b;
}

.status-closed {
  background: #f3f4f6;
  color: #6b7280;
}

.card-body {
  margin-bottom: 12px;
}

.card-reason {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.card-reason .label {
  color: var(--color-text-secondary);
  font-size: 0.8125rem;
  font-weight: 500;
  white-space: nowrap;
}

.card-reason .value {
  color: var(--color-text-primary);
  font-size: 0.875rem;
}

.card-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

.btn {
  padding: 6px 16px;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.15s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-accept {
  background: #10b981;
  color: white;
}

.btn-accept:hover:not(:disabled) {
  background: #059669;
}

.btn-reject {
  background: transparent;
  color: #ef4444;
  border: 1px solid #ef4444;
}

.btn-reject:hover:not(:disabled) {
  background: #fef2f2;
}

.btn-primary {
  background: var(--color-primary);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-close {
  background: #6b7280;
  color: white;
}

.btn-close:hover:not(:disabled) {
  background: #4b5563;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
