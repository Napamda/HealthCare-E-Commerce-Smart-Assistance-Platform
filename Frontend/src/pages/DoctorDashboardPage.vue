<script setup>
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useConsultationStore } from '../stores/consultation.js'

const consultationStore = useConsultationStore()
const { doctorQueue, upcomingConsultations, escalationError } = storeToRefs(consultationStore)

const activeTab = ref('queue')
const statusFilter = ref('ALL')
const priorityFilter = ref('ALL')
const actionLoading = ref(null)
const rejectingId = ref(null)
const rejectionReason = ref('')
const scheduleModalId = ref(null)
const scheduleDate = ref('')
const scheduleTime = ref('')

const calendarYear = ref(new Date().getFullYear())
const calendarMonth = ref(new Date().getMonth())

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

const tabs = [
  { id: 'queue', label: 'Queue', icon: 'inbox' },
  { id: 'upcoming', label: 'Upcoming', icon: 'calendar' },
  { id: 'calendar', label: 'Calendar', icon: 'grid' },
]

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

const sortedUpcoming = computed(() =>
  [...upcomingConsultations.value].sort((a, b) => {
    const aDate = a.scheduledAt ? new Date(a.scheduledAt) : new Date(a.createdAt)
    const bDate = b.scheduledAt ? new Date(b.scheduledAt) : new Date(b.createdAt)
    return aDate - bDate
  }),
)

const consultationsByDate = computed(() => {
  const map = new Map()
  for (const c of upcomingConsultations.value) {
    const key = c.scheduledAt
      ? new Date(c.scheduledAt).toDateString()
      : new Date(c.createdAt).toDateString()
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(c)
  }
  return new Map([...map.entries()].sort(([a], [b]) => new Date(a) - new Date(b)))
})

const stats = computed(() => {
  const total = doctorQueue.value.length
  const pending = doctorQueue.value.filter((c) => c.status === 'PENDING').length
  const active = doctorQueue.value.filter((c) =>
    ['ACCEPTED', 'IN_PROGRESS'].includes(c.status),
  ).length
  const urgent = doctorQueue.value.filter((c) => c.priority === 'URGENT').length
  const scheduled = upcomingConsultations.value.length
  return { total, pending, active, urgent, scheduled }
})

const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

const calendarDays = computed(() => {
  const firstDay = new Date(calendarYear.value, calendarMonth.value, 1)
  const lastDay = new Date(calendarYear.value, calendarMonth.value + 1, 0)
  const startPad = firstDay.getDay()
  const days = []
  for (let i = 0; i < startPad; i++) {
    days.push({ day: null })
  }
  for (let d = 1; d <= lastDay.getDate(); d++) {
    const dateStr = new Date(calendarYear.value, calendarMonth.value, d).toDateString()
    const consults = upcomingConsultations.value.filter((c) => {
      const cDate = c.scheduledAt ? new Date(c.scheduledAt) : new Date(c.createdAt)
      return cDate.toDateString() === dateStr
    })
    days.push({ day: d, consultations: consults, dateStr })
  }
  return days
})

onMounted(() => {
  consultationStore.fetchDoctorQueue()
  consultationStore.fetchUpcoming()
})

function refreshActiveTab() {
  if (activeTab.value === 'queue') consultationStore.fetchDoctorQueue()
  else if (activeTab.value === 'upcoming' || activeTab.value === 'calendar') {
    consultationStore.fetchUpcoming()
  }
}

async function handleAccept(id) {
  actionLoading.value = id
  try {
    await consultationStore.updateStatus(id, 'ACCEPTED')
  } finally {
    actionLoading.value = null
  }
}

function openRejectModal(id) {
  rejectingId.value = id
  rejectionReason.value = ''
}

function cancelReject() {
  rejectingId.value = null
  rejectionReason.value = ''
}

async function confirmReject() {
  const id = rejectingId.value
  actionLoading.value = id
  try {
    await consultationStore.updateStatus(id, 'REJECTED', {
      rejectionReason: rejectionReason.value || undefined,
    })
    rejectingId.value = null
    rejectionReason.value = ''
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

function openScheduleModal(id) {
  scheduleModalId.value = id
  scheduleDate.value = ''
  scheduleTime.value = ''
}

function cancelSchedule() {
  scheduleModalId.value = null
  scheduleDate.value = ''
  scheduleTime.value = ''
}

async function confirmSchedule() {
  const id = scheduleModalId.value
  if (!scheduleDate.value) return
  actionLoading.value = id
  try {
    const isoString = scheduleTime.value
      ? `${scheduleDate.value}T${scheduleTime.value}:00.000Z`
      : `${scheduleDate.value}T00:00:00.000Z`
    await consultationStore.updateStatus(id, 'ACCEPTED', { scheduledAt: isoString })
    scheduleModalId.value = null
    scheduleDate.value = ''
    scheduleTime.value = ''
  } finally {
    actionLoading.value = null
  }
}

function prevMonth() {
  if (calendarMonth.value === 0) {
    calendarMonth.value = 11
    calendarYear.value--
  } else {
    calendarMonth.value--
  }
}

function nextMonth() {
  if (calendarMonth.value === 11) {
    calendarMonth.value = 0
    calendarYear.value++
  } else {
    calendarMonth.value++
  }
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

function formatDateShort(isoString) {
  if (!isoString) return '—'
  return new Date(isoString).toLocaleDateString([], {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function today() {
  return new Date().toDateString()
}
</script>

<template>
  <div class="doctor-dashboard">
    <div class="page-header">
      <div>
        <h1 class="page-title">Doctor Dashboard</h1>
        <p class="page-subtitle">Manage patient consultation requests</p>
      </div>
      <button class="btn-refresh" @click="refreshActiveTab" :disabled="actionLoading">
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
        <div class="stat-label">Active Queue</div>
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
      <div class="stat-card stat-scheduled">
        <div class="stat-number">{{ stats.scheduled }}</div>
        <div class="stat-label">Scheduled</div>
      </div>
    </div>

    <div class="tabs-bar">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="tab-btn"
        :class="{ active: activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="escalationError" class="error-banner">
      <span>{{ escalationError }}</span>
      <button class="btn-dismiss" @click="consultationStore.clearEscalationError()">Dismiss</button>
    </div>

    <!-- ========== QUEUE TAB ========== -->
    <div v-if="activeTab === 'queue'">
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
              <span v-if="consultation.scheduledAt" class="meta-item scheduled">
                Scheduled: {{ formatDateShort(consultation.scheduledAt) }}
              </span>
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
                class="btn btn-schedule"
                :disabled="actionLoading === consultation.id"
                @click="openScheduleModal(consultation.id)"
              >
                Accept & Schedule
              </button>
              <button
                class="btn btn-reject"
                :disabled="actionLoading === consultation.id"
                @click="openRejectModal(consultation.id)"
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
              <button
                class="btn btn-schedule"
                :disabled="actionLoading === consultation.id"
                @click="openScheduleModal(consultation.id)"
              >
                Reschedule
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

    <!-- ========== UPCOMING TAB ========== -->
    <div v-if="activeTab === 'upcoming'">
      <div v-if="sortedUpcoming.length === 0" class="empty-state">
        <h3>No upcoming consultations</h3>
        <p>Accepted consultations with scheduled times will appear here.</p>
      </div>

      <div v-else class="upcoming-list">
        <div
          v-for="consultation in sortedUpcoming"
          :key="consultation.id"
          class="upcoming-card"
          :class="getPriorityClass(consultation.priority)"
        >
          <div class="upcoming-time">
            <div class="time-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
            </div>
            <div>
              <div v-if="consultation.scheduledAt" class="time-value">
                {{ formatDate(consultation.scheduledAt) }}
              </div>
              <div v-else class="time-value dim">Not scheduled</div>
              <div class="time-label">Created {{ formatDateShort(consultation.createdAt) }}</div>
            </div>
          </div>
          <div class="upcoming-info">
            <div class="upcoming-patient">{{ consultation.patientName || 'Patient #' + consultation.patientUserId }}</div>
            <div class="upcoming-reason">{{ consultation.reason }}</div>
            <div class="upcoming-meta">
              <span class="card-status" :class="getStatusClass(consultation.status)">
                {{ statusLabels[consultation.status] }}
              </span>
              <span class="meta-priority" :class="getPriorityClass(consultation.priority)">
                {{ priorityLabels[consultation.priority] }}
              </span>
            </div>
          </div>
          <div class="upcoming-actions">
            <button
              v-if="consultation.status === 'ACCEPTED'"
              class="btn btn-primary btn-sm"
              :disabled="actionLoading === consultation.id"
              @click="handleStartProgress(consultation.id)"
            >
              Start
            </button>
            <button
              v-else-if="consultation.status === 'IN_PROGRESS'"
              class="btn btn-close btn-sm"
              :disabled="actionLoading === consultation.id"
              @click="handleClose(consultation.id)"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== CALENDAR TAB ========== -->
    <div v-if="activeTab === 'calendar'" class="calendar-view">
      <div class="calendar-nav">
        <button class="cal-nav-btn" @click="prevMonth">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <span class="cal-month-label">{{ monthNames[calendarMonth] }} {{ calendarYear }}</span>
        <button class="cal-nav-btn" @click="nextMonth">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>
      </div>

      <div class="cal-grid">
        <div v-for="dow in ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']" :key="dow" class="cal-dow">
          {{ dow }}
        </div>
        <div
          v-for="(cell, idx) in calendarDays"
          :key="idx"
          class="cal-day"
          :class="{
            'cal-other': !cell.day,
            'cal-today': cell.day && cell.dateStr === today(),
            'cal-has-events': cell.consultations && cell.consultations.length > 0,
          }"
        >
          <span v-if="cell.day" class="cal-day-num">{{ cell.day }}</span>
          <div v-if="cell.consultations && cell.consultations.length > 0" class="cal-events">
            <div
              v-for="c in cell.consultations"
              :key="c.id"
              class="cal-event-dot"
              :class="getPriorityClass(c.priority)"
              :title="(c.patientName || 'Patient #' + c.patientUserId) + ': ' + c.reason"
            >
              {{ (c.patientName || 'P#' + c.patientUserId).split(' ')[0] }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="consultationsByDate.size > 0" class="cal-day-list">
        <h3 class="cal-list-title">Upcoming by Date</h3>
        <div v-for="[dateStr, consults] in consultationsByDate" :key="dateStr" class="cal-date-group">
          <div class="cal-date-header">
            <span class="cal-date-label">{{ new Date(dateStr).toLocaleDateString([], { weekday: 'long', month: 'long', day: 'numeric' }) }}</span>
            <span class="cal-date-count">{{ consults.length }} consultation{{ consults.length > 1 ? 's' : '' }}</span>
          </div>
          <div
            v-for="c in consults"
            :key="c.id"
            class="cal-date-item"
            :class="getPriorityClass(c.priority)"
          >
            <span class="cal-item-time">{{ c.scheduledAt ? formatDateShort(c.scheduledAt) : 'Flexible' }}</span>
            <span class="cal-item-patient">{{ c.patientName || 'Patient #' + c.patientUserId }}</span>
            <span class="cal-item-status" :class="getStatusClass(c.status)">
              {{ statusLabels[c.status] }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== REJECTION MODAL ========== -->
    <div v-if="rejectingId" class="modal-overlay" @click.self="cancelReject">
      <div class="modal-card">
        <h3 class="modal-title">Reject Consultation</h3>
        <p class="modal-subtitle">Please provide a reason for rejecting this consultation request.</p>
        <textarea
          v-model="rejectionReason"
          class="modal-textarea"
          placeholder="e.g., No longer available, refer to specialist, outside expertise area..."
          rows="4"
          maxlength="500"
        ></textarea>
        <div class="modal-actions">
          <button class="btn btn-cancel" @click="cancelReject">Cancel</button>
          <button
            class="btn btn-reject-confirm"
            :disabled="actionLoading === rejectingId"
            @click="confirmReject"
          >
            {{ actionLoading === rejectingId ? 'Rejecting...' : 'Confirm Rejection' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========== SCHEDULE MODAL ========== -->
    <div v-if="scheduleModalId" class="modal-overlay" @click.self="cancelSchedule">
      <div class="modal-card">
        <h3 class="modal-title">Schedule Consultation</h3>
        <p class="modal-subtitle">Set a date and time for this consultation.</p>
        <div class="form-group">
          <label class="form-label">Date</label>
          <input
            type="date"
            v-model="scheduleDate"
            class="form-input"
            :min="new Date().toISOString().split('T')[0]"
          />
        </div>
        <div class="form-group">
          <label class="form-label">Time (optional)</label>
          <input
            type="time"
            v-model="scheduleTime"
            class="form-input"
          />
        </div>
        <div class="modal-actions">
          <button class="btn btn-cancel" @click="cancelSchedule">Cancel</button>
          <button
            class="btn btn-accept"
            :disabled="actionLoading === scheduleModalId || !scheduleDate"
            @click="confirmSchedule"
          >
            {{ actionLoading === scheduleModalId ? 'Scheduling...' : 'Confirm & Accept' }}
          </button>
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

/* ---- Stats ---- */
.stats-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
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

.stat-card.stat-scheduled {
  border-color: #8b5cf633;
  background: #8b5cf608;
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

/* ---- Tabs ---- */
.tabs-bar {
  display: flex;
  gap: 4px;
  background: var(--color-surface);
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 20px;
  border: 1px solid var(--color-border);
}

.tab-btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.tab-btn.active {
  background: var(--color-primary);
  color: white;
}

.tab-btn:hover:not(.active) {
  background: var(--color-surface-hover);
}

/* ---- Error ---- */
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

/* ---- Filters ---- */
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

/* ---- Empty ---- */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-secondary);
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

/* ---- Consultation Cards (Queue) ---- */
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

.meta-item.scheduled {
  color: #7c3aed;
  font-weight: 500;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

/* ---- Upcoming List ---- */
.upcoming-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upcoming-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 16px 20px;
}

.upcoming-card.priority-urgent {
  border-left: 4px solid #ef4444;
}

.upcoming-card.priority-high {
  border-left: 4px solid #f59e0b;
}

.upcoming-time {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 160px;
  border-right: 1px solid var(--color-border);
  padding-right: 16px;
}

.time-icon {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.time-value {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--color-text-primary);
}

.time-value.dim {
  color: var(--color-text-secondary);
  font-weight: 400;
}

.time-label {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.upcoming-info {
  flex: 1;
  min-width: 0;
}

.upcoming-patient {
  font-weight: 600;
  font-size: 0.9375rem;
  color: var(--color-text-primary);
}

.upcoming-reason {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.upcoming-meta {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.meta-priority {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  padding: 2px 6px;
  border-radius: 4px;
}

.meta-priority.priority-urgent {
  background: #fee2e2;
  color: #dc2626;
}

.meta-priority.priority-high {
  background: #fef3c7;
  color: #d97706;
}

.meta-priority.priority-normal {
  background: #e0e7ff;
  color: #4f46e5;
}

.meta-priority.priority-low {
  background: #f3f4f6;
  color: #6b7280;
}

.upcoming-actions {
  flex-shrink: 0;
}

/* ---- Calendar View ---- */
.calendar-view {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 20px;
}

.calendar-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.cal-nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}

.cal-nav-btn:hover {
  background: var(--color-surface-hover);
  color: var(--color-text-primary);
}

.cal-month-label {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.cal-dow {
  text-align: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  padding: 8px 0;
  text-transform: uppercase;
}

.cal-day {
  min-height: 72px;
  padding: 6px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: var(--color-bg);
}

.cal-day.cal-today {
  border-color: var(--color-primary);
  background: var(--color-primary);
  background: rgba(var(--color-primary-rgb, 59 130 246), 0.06);
}

.cal-day.cal-has-events {
  border-color: var(--color-border);
}

.cal-day-num {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.cal-events {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 4px;
}

.cal-event-dot {
  font-size: 0.65rem;
  padding: 1px 5px;
  border-radius: 4px;
  color: white;
  cursor: default;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  background: #6b7280;
}

.cal-event-dot.priority-urgent {
  background: #ef4444;
}

.cal-event-dot.priority-high {
  background: #f59e0b;
  color: #1f2937;
}

.cal-event-dot.priority-normal {
  background: #3b82f6;
}

.cal-event-dot.priority-low {
  background: #9ca3af;
}

.cal-day-list {
  margin-top: 24px;
  border-top: 1px solid var(--color-border);
  padding-top: 20px;
}

.cal-list-title {
  margin: 0 0 16px 0;
  font-size: 1rem;
  color: var(--color-text-primary);
}

.cal-date-group {
  margin-bottom: 16px;
}

.cal-date-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.cal-date-label {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--color-text-primary);
}

.cal-date-count {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  background: var(--color-surface-hover);
  padding: 2px 8px;
  border-radius: 10px;
}

.cal-date-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  margin-bottom: 6px;
  background: var(--color-bg);
}

.cal-date-item.priority-urgent {
  border-left: 3px solid #ef4444;
}

.cal-date-item.priority-high {
  border-left: 3px solid #f59e0b;
}

.cal-item-time {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #7c3aed;
  min-width: 90px;
}

.cal-item-patient {
  flex: 1;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-primary);
}

.cal-item-status {
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

/* ---- Modal ---- */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(2px);
}

.modal-card {
  background: var(--color-surface);
  border-radius: 16px;
  padding: 28px;
  width: 90%;
  max-width: 440px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.modal-title {
  margin: 0 0 6px 0;
  font-size: 1.25rem;
  color: var(--color-text-primary);
}

.modal-subtitle {
  margin: 0 0 16px 0;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.modal-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.875rem;
  font-family: inherit;
  resize: vertical;
  background: var(--color-bg);
  color: var(--color-text-primary);
  box-sizing: border-box;
}

.modal-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-rgb, 59 130 246), 0.1);
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.875rem;
  font-family: inherit;
  background: var(--color-bg);
  color: var(--color-text-primary);
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-rgb, 59 130 246), 0.1);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

/* ---- Buttons ---- */
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

.btn-sm {
  padding: 5px 12px;
  font-size: 0.75rem;
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

.btn-reject-confirm {
  background: #ef4444;
  color: white;
}

.btn-reject-confirm:hover:not(:disabled) {
  background: #dc2626;
}

.btn-schedule {
  background: transparent;
  color: #7c3aed;
  border: 1px solid #7c3aed;
}

.btn-schedule:hover:not(:disabled) {
  background: #f5f3ff;
}

.btn-cancel {
  background: transparent;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.btn-cancel:hover:not(:disabled) {
  background: var(--color-surface-hover);
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

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(3, 1fr);
  }

  .upcoming-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .upcoming-time {
    border-right: none;
    border-bottom: 1px solid var(--color-border);
    padding-right: 0;
    padding-bottom: 10px;
    width: 100%;
  }

  .cal-grid {
    gap: 2px;
  }

  .cal-day {
    min-height: 48px;
    padding: 4px;
  }

  .cal-events {
    display: none;
  }
}
</style>
