<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { usePharmacistStore } from '../stores/pharmacist.js'
import { getPrescriptionDownloadUrl } from '../services/prescription.js'

const pharmacistStore = usePharmacistStore()

const pharmacistId = ref(1)
const searchQuery = ref('')
const selectedStatus = ref('')
const startDate = ref('')
const endDate = ref('')
const selectedPrescription = ref(null)
const showDetail = ref(false)
const reviewComment = ref('')

const statusTabs = [
  { value: '', label: 'All' },
  { value: 'PENDING_REVIEW', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
]

const statusConfig = {
  PENDING_REVIEW: { label: 'Pending Review', class: 'status-pending', icon: '&#9203;' },
  APPROVED: { label: 'Approved', class: 'status-approved', icon: '&#9989;' },
  REJECTED: { label: 'Rejected', class: 'status-rejected', icon: '&#10060;' },
}

const hasActiveFilters = computed(() => {
  return searchQuery.value || selectedStatus.value || startDate.value || endDate.value
})

const statCounts = computed(() => {
  const counts = { total: pharmacistStore.prescriptions.length, pending: 0, approved: 0, rejected: 0 }
  pharmacistStore.prescriptions.forEach((p) => {
    if (p.status === 'PENDING_REVIEW') counts.pending++
    if (p.status === 'APPROVED') counts.approved++
    if (p.status === 'REJECTED') counts.rejected++
  })
  return counts
})

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

function formatOcrPreview(text) {
  if (!text) return ''
  return text.length > 120 ? text.substring(0, 120) + '...' : text
}

function selectStatus(status) {
  selectedStatus.value = status
  performSearch()
}

function performSearch() {
  const params = {}
  if (selectedStatus.value) params.status = selectedStatus.value
  if (searchQuery.value.trim()) params.search = searchQuery.value.trim()
  if (startDate.value) params.startDate = startDate.value
  if (endDate.value) params.endDate = endDate.value

  if (Object.keys(params).length > 0) {
    pharmacistStore.fetchSearchPrescriptions(params)
  } else {
    pharmacistStore.fetchPendingPrescriptions()
  }
}

function clearFilters() {
  searchQuery.value = ''
  selectedStatus.value = ''
  startDate.value = ''
  endDate.value = ''
  pharmacistStore.fetchPendingPrescriptions()
}

function viewPrescriptionDetail(prescription) {
  pharmacistStore.fetchPrescription(prescription.id).then(() => {
    selectedPrescription.value = pharmacistStore.currentPrescription
    showDetail.value = true
  })
}

function closeDetail() {
  showDetail.value = false
  selectedPrescription.value = null
  pharmacistStore.clearCurrentPrescription()
}

function downloadFile(prescription) {
  const url = getPrescriptionDownloadUrl(prescription.id, prescription.patientUserId)
  window.open(url, '_blank')
}

function getReviewActionsDisabled() {
  if (pharmacistStore.submitting) return true
  if (!pharmacistStore.currentPrescription) return true
  if (pharmacistStore.currentPrescription.status !== 'PENDING_REVIEW') return true
  return false
}

async function approvePrescription() {
  if (getReviewActionsDisabled()) return
  pharmacistStore.resetReviewState()
  try {
    await pharmacistStore.submitReview(
      pharmacistStore.currentPrescription.id,
      'APPROVED',
      pharmacistId.value,
      reviewComment.value,
    )
    selectedPrescription.value = pharmacistStore.currentPrescription
  } catch (_) {
  }
}

async function rejectPrescription() {
  if (getReviewActionsDisabled()) return
  pharmacistStore.resetReviewState()
  try {
    await pharmacistStore.submitReview(
      pharmacistStore.currentPrescription.id,
      'REJECTED',
      pharmacistId.value,
      reviewComment.value,
    )
    selectedPrescription.value = pharmacistStore.currentPrescription
  } catch (_) {
  }
}

onMounted(() => {
  pharmacistStore.fetchPendingPrescriptions()
})
</script>

<template>
  <div class="pharmacist-dashboard">
    <header class="dashboard-header">
      <div class="header-top">
        <div>
          <h1>Pharmacist Dashboard</h1>
          <p class="header-subtitle">Review and manage prescriptions</p>
        </div>
      </div>

      <div class="stat-cards">
        <div class="stat-card stat-pending">
          <span class="stat-number">{{ statCounts.pending }}</span>
          <span class="stat-label">Pending</span>
        </div>
        <div class="stat-card stat-approved">
          <span class="stat-number">{{ statCounts.approved }}</span>
          <span class="stat-label">Approved</span>
        </div>
        <div class="stat-card stat-rejected">
          <span class="stat-number">{{ statCounts.rejected }}</span>
          <span class="stat-label">Rejected</span>
        </div>
        <div class="stat-card stat-total">
          <span class="stat-number">{{ statCounts.total }}</span>
          <span class="stat-label">Total</span>
        </div>
      </div>

      <div class="search-section">
        <div class="search-bar">
          <span class="search-icon">&#128269;</span>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Search by file name, OCR text, patient ID, or comments..."
            class="search-input"
            @keyup.enter="performSearch"
          />
        </div>

        <div class="filter-controls">
          <div class="status-tabs">
            <button
              v-for="tab in statusTabs"
              :key="tab.value"
              class="status-tab"
              :class="{ active: selectedStatus === tab.value }"
              @click="selectStatus(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>

          <div class="date-filters">
            <input v-model="startDate" type="date" class="date-input" title="Start date" />
            <span class="date-separator">to</span>
            <input v-model="endDate" type="date" class="date-input" title="End date" />
            <button class="btn btn-secondary" @click="performSearch">Filter</button>
          </div>

          <button
            v-if="hasActiveFilters"
            class="btn btn-clear"
            @click="clearFilters"
          >
            Clear Filters
          </button>
        </div>
      </div>
    </header>

    <div v-if="pharmacistStore.loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading prescriptions...</p>
    </div>

    <div v-else-if="pharmacistStore.error" class="error-state">
      <p>{{ pharmacistStore.error }}</p>
      <button class="btn btn-secondary" @click="pharmacistStore.fetchPendingPrescriptions()">
        Retry
      </button>
    </div>

    <div v-else-if="!showDetail && pharmacistStore.prescriptions.length === 0" class="empty-state">
      <div class="empty-icon">&#128138;</div>
      <h2>No prescriptions found</h2>
      <p v-if="hasActiveFilters">No prescriptions match your search criteria. Try adjusting your filters.</p>
      <p v-else>There are no pending prescriptions to review at this time.</p>
    </div>

    <div v-else-if="!showDetail" class="prescription-table-container">
      <div class="table-header">
        <span class="col-file">File</span>
        <span class="col-patient">Patient ID</span>
        <span class="col-ocr">OCR Preview</span>
        <span class="col-date">Uploaded</span>
        <span class="col-status">Status</span>
        <span class="col-action"></span>
      </div>

      <div
        v-for="prescription in pharmacistStore.prescriptions"
        :key="prescription.id"
        class="table-row"
        @click="viewPrescriptionDetail(prescription)"
      >
        <div class="col-file">
          <span class="file-icon">&#128196;</span>
          <div class="file-info">
            <span class="file-name">{{ prescription.originalFileName }}</span>
            <span class="file-meta">{{ prescription.fileType }} &middot; {{ formatFileSize(prescription.fileSize) }}</span>
          </div>
        </div>
        <div class="col-patient">
          <span class="patient-badge">#{{ prescription.patientUserId }}</span>
        </div>
        <div class="col-ocr">
          <span class="ocr-preview" v-if="prescription.ocrText">{{ formatOcrPreview(prescription.ocrText) }}</span>
          <span class="ocr-preview-empty" v-else>—</span>
        </div>
        <div class="col-date">
          <span class="date-text">{{ formatDate(prescription.createdAt) }}</span>
        </div>
        <div class="col-status">
          <span
            class="status-badge"
            :class="statusConfig[prescription.status]?.class || ''"
            v-html="(statusConfig[prescription.status]?.icon || '') + ' ' + (statusConfig[prescription.status]?.label || prescription.status)"
          ></span>
        </div>
        <div class="col-action">
          <button class="btn btn-view" @click.stop="viewPrescriptionDetail(prescription)">
            View &#8594;
          </button>
        </div>
      </div>
    </div>

    <div v-if="showDetail && pharmacistStore.currentPrescription" class="detail-panel">
      <div class="detail-header">
        <button class="btn btn-back" @click="closeDetail">&larr; Back to List</button>
        <div class="detail-header-right">
          <span
            class="status-badge-large"
            :class="statusConfig[pharmacistStore.currentPrescription.status]?.class || ''"
            v-html="(statusConfig[pharmacistStore.currentPrescription.status]?.icon || '') + ' ' + (statusConfig[pharmacistStore.currentPrescription.status]?.label || pharmacistStore.currentPrescription.status)"
          ></span>
        </div>
      </div>

      <div class="detail-body">
        <div class="detail-card">
          <h3 class="section-title">Prescription Information</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">Prescription ID</span>
              <span class="detail-value">#{{ pharmacistStore.currentPrescription.id }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Patient ID</span>
              <span class="detail-value">#{{ pharmacistStore.currentPrescription.patientUserId }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">File Name</span>
              <span class="detail-value">{{ pharmacistStore.currentPrescription.originalFileName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">File Type</span>
              <span class="detail-value">{{ pharmacistStore.currentPrescription.fileType }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">File Size</span>
              <span class="detail-value">{{ formatFileSize(pharmacistStore.currentPrescription.fileSize) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Uploaded</span>
              <span class="detail-value">{{ formatDate(pharmacistStore.currentPrescription.createdAt) }}</span>
            </div>
            <div class="detail-item" v-if="pharmacistStore.currentPrescription.pharmacistId">
              <span class="detail-label">Reviewed by</span>
              <span class="detail-value">Pharmacist #{{ pharmacistStore.currentPrescription.pharmacistId }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Last Updated</span>
              <span class="detail-value">{{ formatDate(pharmacistStore.currentPrescription.updatedAt) }}</span>
            </div>
          </div>

          <button
            class="btn btn-secondary download-btn"
            @click="downloadFile(pharmacistStore.currentPrescription)"
          >
            &#128229; Download File
          </button>
        </div>

        <div class="detail-card ocr-card">
          <h3 class="section-title">OCR Extracted Text</h3>
          <pre class="ocr-display" v-if="pharmacistStore.currentPrescription.ocrText">{{
            pharmacistStore.currentPrescription.ocrText
          }}</pre>
          <p class="ocr-empty" v-else>No text has been extracted from this prescription.</p>
        </div>

        <div
          v-if="pharmacistStore.currentPrescription.pharmacistComments"
          class="detail-card comment-card"
        >
          <h3 class="section-title">Previous Pharmacist Comment</h3>
          <p class="comment-text">{{ pharmacistStore.currentPrescription.pharmacistComments }}</p>
        </div>

        <div
          v-if="pharmacistStore.currentPrescription.status === 'PENDING_REVIEW'"
          class="detail-card review-card"
        >
          <h3 class="section-title">Review This Prescription</h3>

          <div v-if="pharmacistStore.reviewSuccess" class="review-success">
            <span class="success-icon">&#10003;</span>
            <p>{{ pharmacistStore.reviewMessage }}</p>
          </div>

          <div v-if="pharmacistStore.error" class="review-error">
            <p>{{ pharmacistStore.error }}</p>
          </div>

          <div v-if="!pharmacistStore.reviewSuccess">
            <label class="review-label" for="review-comment">Review Comment</label>
            <textarea
              id="review-comment"
              v-model="reviewComment"
              class="review-textarea"
              placeholder="Add your comments, observations, or reason for rejection..."
              rows="4"
              :disabled="pharmacistStore.submitting"
            ></textarea>

            <div class="review-actions">
              <button
                class="btn btn-approve"
                :disabled="getReviewActionsDisabled()"
                @click="approvePrescription"
              >
                <span v-if="pharmacistStore.submitting" class="btn-spinner"></span>
                <span v-else>&#10003;</span>
                Approve Prescription
              </button>
              <button
                class="btn btn-reject"
                :disabled="getReviewActionsDisabled()"
                @click="rejectPrescription"
              >
                <span v-if="pharmacistStore.submitting" class="btn-spinner"></span>
                <span v-else>&#10007;</span>
                Reject Prescription
              </button>
            </div>
          </div>

          <div v-if="pharmacistStore.reviewSuccess" class="review-after-actions">
            <button class="btn btn-secondary" @click="closeDetail">
              &larr; Back to List
            </button>
          </div>
        </div>

        <div
          v-if="pharmacistStore.currentPrescription.status !== 'PENDING_REVIEW'
            && pharmacistStore.currentPrescription.pharmacistComments"
          class="detail-card reviewed-card"
        >
          <h3 class="section-title">Review Outcome</h3>
          <div class="reviewed-status">
            <span
              class="status-badge"
              :class="pharmacistStore.currentPrescription.status === 'APPROVED' ? 'status-approved' : 'status-rejected'"
            >
              {{ pharmacistStore.currentPrescription.status === 'APPROVED' ? '&#9989; Approved' : '&#10060; Rejected' }}
            </span>
            <span class="reviewed-by" v-if="pharmacistStore.currentPrescription.pharmacistId">
              by Pharmacist #{{ pharmacistStore.currentPrescription.pharmacistId }}
            </span>
          </div>
          <p class="comment-text">{{ pharmacistStore.currentPrescription.pharmacistComments }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pharmacist-dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}

.dashboard-header {
  margin-bottom: 28px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-top h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
}

.header-subtitle {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-pending .stat-number { color: #d97706; }
.stat-pending .stat-label { color: #92400e; }

.stat-approved .stat-number { color: #16a34a; }
.stat-approved .stat-label { color: #166534; }

.stat-rejected .stat-number { color: #dc2626; }
.stat-rejected .stat-label { color: #991b1b; }

.stat-total .stat-number { color: var(--color-primary); }
.stat-total .stat-label { color: var(--color-text-muted); }

.search-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 14px;
  margin-bottom: 16px;
}

.search-bar:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.search-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  padding: 10px 0;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--color-text);
  outline: none;
}

.search-input::placeholder {
  color: var(--color-text-muted);
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.status-tabs {
  display: flex;
  gap: 4px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  padding: 3px;
}

.status-tab {
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border-radius: var(--radius-sm);
  transition: all 0.15s ease;
}

.status-tab:hover {
  color: var(--color-text);
}

.status-tab.active {
  background: var(--color-surface);
  color: var(--color-text);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.date-filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-input {
  padding: 6px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-text);
  background: var(--color-bg);
  outline: none;
}

.date-input:focus {
  border-color: var(--color-primary);
}

.date-separator {
  font-size: 12px;
  color: var(--color-text-muted);
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  transition: all 0.15s ease;
}

.btn-secondary {
  background: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover {
  background: var(--color-border);
}

.btn-clear {
  color: var(--color-danger);
  font-weight: 500;
}

.btn-clear:hover {
  background: var(--color-bg);
}

.btn-back {
  color: var(--color-text-secondary);
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  transition: all 0.15s ease;
}

.btn-back:hover {
  background: var(--color-bg);
  border-color: var(--color-text-muted);
}

.btn-view {
  color: var(--color-primary);
  font-weight: 600;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}

.btn-view:hover {
  background: var(--color-primary-bg);
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
  font-size: 14px;
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

.prescription-table-container {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-header {
  display: grid;
  grid-template-columns: 2fr 1fr 2fr 1.5fr 1fr 0.5fr;
  padding: 12px 20px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--color-text-muted);
}

.table-row {
  display: grid;
  grid-template-columns: 2fr 1fr 2fr 1.5fr 1fr 0.5fr;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: background 0.1s ease;
  align-items: center;
}

.table-row:last-child {
  border-bottom: none;
}

.table-row:hover {
  background: var(--color-bg);
}

.col-file {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.file-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.file-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 12px;
  color: var(--color-text-muted);
}

.col-patient {
  display: flex;
  align-items: center;
}

.patient-badge {
  padding: 3px 10px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.col-ocr {
  display: flex;
  align-items: center;
  min-width: 0;
}

.ocr-preview {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-family: 'Courier New', Courier, monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.ocr-preview-empty {
  font-size: 13px;
  color: var(--color-text-muted);
}

.col-date {
  display: flex;
  align-items: center;
}

.date-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.col-status {
  display: flex;
  align-items: center;
}

.status-badge {
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
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

.col-action {
  display: flex;
  justify-content: flex-end;
}

.detail-panel {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.detail-header-right {
  display: flex;
  align-items: center;
}

.status-badge-large {
  padding: 6px 16px;
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 600;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
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
  color: var(--color-text);
  word-break: break-all;
}

.download-btn {
  font-size: 14px;
}

.ocr-card {
  border-left: 4px solid var(--color-warning);
}

.ocr-display {
  font-size: 14px;
  font-family: 'Courier New', Courier, monospace;
  line-height: 1.7;
  color: var(--color-text);
  white-space: pre-wrap;
  word-break: break-word;
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  margin: 0;
}

.ocr-empty {
  font-size: 14px;
  color: var(--color-text-muted);
  font-style: italic;
}

.comment-card {
  border-left: 4px solid var(--color-primary);
}

.comment-text {
  font-size: 15px;
  color: var(--color-text);
  line-height: 1.7;
  white-space: pre-wrap;
}

.review-card {
  border-left: 4px solid var(--color-warning);
}

.review-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.review-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text);
  background: var(--color-bg);
  resize: vertical;
  line-height: 1.6;
  outline: none;
  transition: border-color 0.15s ease;
  box-sizing: border-box;
}

.review-textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.review-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.review-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.btn-approve {
  flex: 1;
  justify-content: center;
  padding: 12px 20px;
  background: #16a34a;
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  transition: background 0.15s ease;
}

.btn-approve:hover:not(:disabled) {
  background: #15803d;
}

.btn-approve:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-reject {
  flex: 1;
  justify-content: center;
  padding: 12px 20px;
  background: #dc2626;
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  transition: background 0.15s ease;
}

.btn-reject:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-reject:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.review-success {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: #dcfce7;
  border: 1px solid #bbf7d0;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.success-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #16a34a;
  color: #fff;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.review-success p {
  font-size: 14px;
  color: #166534;
  margin: 0;
  line-height: 1.6;
}

.review-error {
  padding: 12px 16px;
  background: #fee2e2;
  border: 1px solid #fecaca;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.review-error p {
  font-size: 13px;
  color: #991b1b;
  margin: 0;
}

.review-after-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.reviewed-card {
  border-left: 4px solid var(--color-text-muted);
}

.reviewed-status {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.reviewed-by {
  font-size: 13px;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .table-header,
  .table-row {
    grid-template-columns: 1fr 0.8fr 0.6fr;
    gap: 8px;
  }

  .col-ocr, .col-date {
    display: none;
  }

  .filter-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .date-filters {
    flex-wrap: wrap;
  }
}
</style>
