<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { usePharmacistStore } from '../stores/pharmacist.js'
import { useAuthStore } from '../stores/auth.js'
import { getPrescriptionDownloadUrl } from '../services/prescription.js'

const pharmacistStore = usePharmacistStore()
const authStore = useAuthStore()

const pharmacistId = computed(() => authStore.currentUser?.id)
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


