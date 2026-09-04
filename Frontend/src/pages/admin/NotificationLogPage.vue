<script setup>
import { ref, onMounted } from 'vue'
import '../../styles/components/notification-logs.css'
import { useAuthStore } from '../../stores/auth.js'
import {
  getEmailLogs,
  getSmsLogs,
  testWelcomeEmail,
  testOrderEmail,
  testPrescriptionEmail,
  testEventEmail,
} from '../../services/notificationLog.js'

const authStore = useAuthStore()
const activeTab = ref('emails')

// Email logs
const emailLogs = ref([])
const emailFilter = ref('')
const emailLoading = ref(false)

// SMS logs
const smsLogs = ref([])
const smsFilter = ref('')
const smsLoading = ref(false)

// Selected log detail
const selectedLog = ref(null)
const showDetail = ref(false)

// Test form
const showTestForm = ref(false)
const testType = ref('welcome')
const testForm = ref({
  email: '',
  name: '',
  orderId: '1',
  total: '$50.00',
  prescriptionId: '1',
  status: 'APPROVED',
  comments: '',
  eventTitle: 'Health Awareness Day',
  eventDate: '2026-09-10 10:00',
  venue: 'Community Center',
})
const testMessage = ref('')

async function loadEmails() {
  emailLoading.value = true
  try {
    emailLogs.value = await getEmailLogs(emailFilter.value)
  } catch (e) {
    console.error('Failed to load emails:', e)
  } finally {
    emailLoading.value = false
  }
}

async function loadSms() {
  smsLoading.value = true
  try {
    smsLogs.value = await getSmsLogs(smsFilter.value)
  } catch (e) {
    console.error('Failed to load SMS logs:', e)
  } finally {
    smsLoading.value = false
  }
}

function viewLog(log) {
  selectedLog.value = log
  showDetail.value = true
}

function closeDetail() {
  showDetail.value = false
  selectedLog.value = null
}

async function sendTest() {
  testMessage.value = ''
  const f = testForm.value
  try {
    let result
    switch (testType.value) {
      case 'welcome':
        result = await testWelcomeEmail(f.email, f.name)
        break
      case 'order':
        result = await testOrderEmail(f.email, f.name, f.orderId, f.total)
        break
      case 'prescription':
        result = await testPrescriptionEmail(f.email, f.name, f.prescriptionId, f.status, f.comments)
        break
      case 'event':
        result = await testEventEmail(f.email, f.name, f.eventTitle, f.eventDate, f.venue)
        break
    }
    testMessage.value = result?.message || 'Test sent successfully'
    // Reload logs to show the new entry
    if (activeTab.value === 'emails') await loadEmails()
    else await loadSms()
    // Also load the other tab's data
    if (activeTab.value === 'emails') await loadSms()
    else await loadEmails()
  } catch (e) {
    testMessage.value = e.response?.data?.error || e.message || 'Failed to send test'
  }
}

function formatTime(dateStr) {
  return new Date(dateStr).toLocaleString()
}

function statusClass(status) {
  if (status === 'SENT') return 'status-sent'
  if (status === 'FAILED') return 'status-failed'
  return 'status-queued'
}

onMounted(() => {
  loadEmails()
  loadSms()
})
</script>

<template>
  <div class="notification-log-page">
    <div class="page-header">
      <h1>Notification Logs</h1>
      <p class="subtitle">Email and SMS notification history</p>
    </div>

    <div class="tabs">
      <button :class="{ active: activeTab === 'emails' }" @click="activeTab = 'emails'">
        Emails ({{ emailLogs.length }})
      </button>
      <button :class="{ active: activeTab === 'sms' }" @click="activeTab = 'sms'">
        SMS ({{ smsLogs.length }})
      </button>
      <button class="test-btn" @click="showTestForm = !showTestForm">
        {{ showTestForm ? 'Close' : 'Send Test' }}
      </button>
    </div>

    <!-- Test Form -->
    <Transition name="slide">
      <div v-if="showTestForm" class="test-form">
        <h3>Send Test Notification</h3>
        <div class="form-row">
          <label>Type</label>
          <select v-model="testType">
            <option value="welcome">Welcome</option>
            <option value="order">Order Confirmation</option>
            <option value="prescription">Prescription Status</option>
            <option value="event">Event Reminder</option>
          </select>
        </div>
        <div class="form-row">
          <label>Email</label>
          <input v-model="testForm.email" type="email" placeholder="user@example.com" />
        </div>
        <div class="form-row">
          <label>Name</label>
          <input v-model="testForm.name" type="text" placeholder="John Doe" />
        </div>
        <div v-if="testType === 'order'" class="form-row">
          <label>Order ID</label>
          <input v-model="testForm.orderId" type="text" />
          <label>Total</label>
          <input v-model="testForm.total" type="text" />
        </div>
        <div v-if="testType === 'prescription'" class="form-row">
          <label>Prescription ID</label>
          <input v-model="testForm.prescriptionId" type="text" />
          <label>Status</label>
          <select v-model="testForm.status">
            <option value="APPROVED">APPROVED</option>
            <option value="REJECTED">REJECTED</option>
          </select>
          <label>Comments</label>
          <input v-model="testForm.comments" type="text" />
        </div>
        <div v-if="testType === 'event'" class="form-row">
          <label>Event Title</label>
          <input v-model="testForm.eventTitle" type="text" />
          <label>Date</label>
          <input v-model="testForm.eventDate" type="text" />
          <label>Venue</label>
          <input v-model="testForm.venue" type="text" />
        </div>
        <button class="send-btn" @click="sendTest">Send</button>
        <p v-if="testMessage" class="test-message">{{ testMessage }}</p>
      </div>
    </Transition>

    <!-- Emails Tab -->
    <div v-if="activeTab === 'emails'" class="tab-content">
      <div class="filter-bar">
        <select v-model="emailFilter" @change="loadEmails">
          <option value="">All Types</option>
          <option value="WELCOME">Welcome</option>
          <option value="ORDER_CONFIRMATION">Order Confirmation</option>
          <option value="PRESCRIPTION_STATUS">Prescription Status</option>
          <option value="EVENT_REMINDER">Event Reminder</option>
        </select>
        <button @click="loadEmails">Refresh</button>
      </div>
      <div v-if="emailLoading" class="loading">Loading...</div>
      <table v-else-if="emailLogs.length" class="log-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>To</th>
            <th>Subject</th>
            <th>Type</th>
            <th>Status</th>
            <th>Date</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in emailLogs" :key="log.id">
            <td>{{ log.id }}</td>
            <td>{{ log.toEmail }}</td>
            <td>{{ log.subject }}</td>
            <td><span class="type-badge">{{ log.type }}</span></td>
            <td><span class="status-badge" :class="statusClass(log.status)">{{ log.status }}</span></td>
            <td>{{ formatTime(log.createdAt) }}</td>
            <td><button class="view-btn" @click="viewLog(log)">View</button></td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">No emails sent yet</div>
    </div>

    <!-- SMS Tab -->
    <div v-if="activeTab === 'sms'" class="tab-content">
      <div class="filter-bar">
        <select v-model="smsFilter" @change="loadSms">
          <option value="">All Types</option>
          <option value="ORDER_CONFIRMATION">Order Confirmation</option>
          <option value="PRESCRIPTION_STATUS">Prescription Status</option>
          <option value="EVENT_REMINDER">Event Reminder</option>
        </select>
        <button @click="loadSms">Refresh</button>
      </div>
      <div v-if="smsLoading" class="loading">Loading...</div>
      <table v-else-if="smsLogs.length" class="log-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Phone</th>
            <th>Message</th>
            <th>Type</th>
            <th>Status</th>
            <th>Date</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in smsLogs" :key="log.id">
            <td>{{ log.id }}</td>
            <td>{{ log.phoneNumber }}</td>
            <td class="msg-cell">{{ log.message }}</td>
            <td><span class="type-badge">{{ log.type }}</span></td>
            <td><span class="status-badge" :class="statusClass(log.status)">{{ log.status }}</span></td>
            <td>{{ formatTime(log.createdAt) }}</td>
            <td><button class="view-btn" @click="viewLog(log)">View</button></td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">No SMS sent yet</div>
    </div>

    <!-- Detail Modal -->
    <Transition name="fade">
      <div v-if="showDetail && selectedLog" class="modal-overlay" @click.self="closeDetail">
        <div class="modal">
          <div class="modal-header">
            <h3>Notification Detail</h3>
            <button class="close-btn" @click="closeDetail">&times;</button>
          </div>
          <div class="modal-body">
            <div v-if="selectedLog.toEmail" class="detail-row">
              <strong>To:</strong> {{ selectedLog.toEmail }}
            </div>
            <div v-if="selectedLog.phoneNumber" class="detail-row">
              <strong>Phone:</strong> {{ selectedLog.phoneNumber }}
            </div>
            <div class="detail-row">
              <strong>Subject/Type:</strong> {{ selectedLog.subject || selectedLog.type }}
            </div>
            <div class="detail-row">
              <strong>Type:</strong> {{ selectedLog.type }}
            </div>
            <div class="detail-row">
              <strong>Status:</strong>
              <span class="status-badge" :class="statusClass(selectedLog.status)">{{ selectedLog.status }}</span>
            </div>
            <div v-if="selectedLog.error" class="detail-row error">
              <strong>Error:</strong> {{ selectedLog.error }}
            </div>
            <div class="detail-row">
              <strong>Date:</strong> {{ formatTime(selectedLog.createdAt) }}
            </div>
            <div class="detail-body">
              <strong>Content:</strong>
              <div class="content-preview" v-html="selectedLog.body || selectedLog.message"></div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>
