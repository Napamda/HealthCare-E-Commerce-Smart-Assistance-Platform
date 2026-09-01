<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePrescriptionStore } from '../stores/prescription.js'
import { useNotificationStore } from '../stores/notification.js'
import { useAuthStore } from '../stores/auth.js'
import NotificationBell from '../components/NotificationBell.vue'

const router = useRouter()
const prescriptionStore = usePrescriptionStore()
const notificationStore = useNotificationStore()
const authStore = useAuthStore()

const patientUserId = computed(() => authStore.currentUser?.id)

const statusConfig = {
  PENDING_REVIEW: { label: 'Pending Review', class: 'status-pending' },
  APPROVED: { label: 'Approved', class: 'status-approved' },
  REJECTED: { label: 'Rejected', class: 'status-rejected' },
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString()
}

function goToDetail(id) {
  router.push(`/prescriptions/${id}`)
}

function goToUpload() {
  router.push('/prescriptions/upload')
}

onMounted(() => {
  prescriptionStore.fetchPatientPrescriptions(patientUserId.value)
  notificationStore.fetchUnreadCount(patientUserId.value)
})
</script>

<template>
  <div class="prescription-list">
    <header class="page-header">
      <div class="header-left">
        <h1>My Prescriptions</h1>
        <span class="header-count"
          >{{ prescriptionStore.prescriptions.length }} prescription{{
            prescriptionStore.prescriptions.length !== 1 ? 's' : ''
          }}</span
        >
      </div>
      <div class="header-right">
        <NotificationBell :userId="patientUserId" />
        <button class="btn btn-primary" @click="goToUpload">
          <span class="btn-icon">+</span> Upload New
        </button>
      </div>
    </header>

    <div v-if="prescriptionStore.loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading prescriptions...</p>
    </div>

    <div v-else-if="prescriptionStore.error" class="error-state">
      <p>{{ prescriptionStore.error }}</p>
      <button
        class="btn btn-secondary"
        @click="prescriptionStore.fetchPatientPrescriptions(patientUserId)"
      >
        Retry
      </button>
    </div>

    <div v-else-if="prescriptionStore.prescriptions.length === 0" class="empty-state">
      <div class="empty-icon">&#128138;</div>
      <h2>No prescriptions yet</h2>
      <p>Upload your first prescription to get started</p>
      <button class="btn btn-primary" @click="goToUpload">Upload Prescription</button>
    </div>

    <div v-else class="prescription-grid">
      <div
        v-for="prescription in prescriptionStore.prescriptions"
        :key="prescription.id"
        class="prescription-card"
        @click="goToDetail(prescription.id)"
      >
        <div class="card-header">
          <div class="file-info">
            <span class="file-icon">&#128196;</span>
            <span class="file-name" :title="prescription.originalFileName">
              {{ prescription.originalFileName }}
            </span>
          </div>
          <span
            class="status-badge"
            :class="statusConfig[prescription.status]?.class || ''"
          >
            {{ statusConfig[prescription.status]?.label || prescription.status }}
          </span>
        </div>
        <div class="card-body">
          <div class="card-detail">
            <span class="detail-label">Type</span>
            <span class="detail-value">{{ prescription.fileType }}</span>
          </div>
          <div class="card-detail">
            <span class="detail-label">Size</span>
            <span class="detail-value">{{ formatFileSize(prescription.fileSize) }}</span>
          </div>
          <div class="card-detail">
            <span class="detail-label">Uploaded</span>
            <span class="detail-value">{{ formatDate(prescription.createdAt) }}</span>
          </div>
          <div
            v-if="prescription.pharmacistComments"
            class="card-comment"
          >
            <span class="detail-label">Pharmacist Comment</span>
            <span class="detail-value comment-text">{{ prescription.pharmacistComments }}</span>
          </div>
        </div>
        <div class="card-footer">
          <span class="view-detail">View details &#8594;</span>
        </div>
      </div>
    </div>
  </div>
</template>


