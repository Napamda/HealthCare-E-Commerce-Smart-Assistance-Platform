<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePrescriptionStore } from '../stores/prescription.js'
import { useNotificationStore } from '../stores/notification.js'
import NotificationBell from '../components/NotificationBell.vue'

const router = useRouter()
const prescriptionStore = usePrescriptionStore()
const notificationStore = useNotificationStore()

const patientUserId = ref(1)

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

<style scoped>
.prescription-list {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.header-left h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
}

.header-count {
  font-size: 14px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  transition: all 0.15s ease;
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;
}

.btn-primary:hover {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.btn-secondary {
  background: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover {
  background: var(--color-border);
}

.btn-icon {
  font-size: 18px;
  line-height: 1;
}

.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 80px 24px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state h2 {
  font-size: 20px;
  color: var(--color-text);
  margin-bottom: 8px;
}

.empty-state p {
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

.error-state p {
  color: var(--color-danger);
  margin-bottom: 16px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p {
  color: var(--color-text-muted);
  font-size: 14px;
}

.prescription-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.prescription-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.prescription-card:hover {
  border-color: var(--color-primary);
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.1);
  transform: translateY(-1px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.file-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.file-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

.status-pending {
  background: #fef3c7;
  color: #92400e;
}

.status-approved {
  background: #dcfce7;
  color: #166534;
}

.status-rejected {
  background: #fee2e2;
  color: #991b1b;
}

.card-body {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 32px;
}

.card-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-comment {
  flex-basis: 100%;
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: var(--color-bg);
  padding: 10px 12px;
  border-radius: var(--radius-sm);
}

.detail-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--color-text-muted);
}

.detail-value {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.comment-text {
  font-style: italic;
  color: var(--color-text);
}

.card-footer {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

.view-detail {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary);
}
</style>
