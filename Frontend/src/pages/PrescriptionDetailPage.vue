<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePrescriptionStore } from '../stores/prescription.js'
import { useNotificationStore } from '../stores/notification.js'
import { getPrescriptionDownloadUrl } from '../services/prescription.js'
import NotificationBell from '../components/NotificationBell.vue'

const route = useRoute()
const router = useRouter()
const prescriptionStore = usePrescriptionStore()
const notificationStore = useNotificationStore()

const patientUserId = ref(1)
const editingOcr = ref(false)
const editedOcrText = ref('')
const saveMessage = ref('')

const prescriptionId = computed(() => Number(route.params.id))

const statusConfig = {
  PENDING_REVIEW: { label: 'Pending Review', class: 'status-pending', icon: '&#9203;' },
  APPROVED: { label: 'Approved', class: 'status-approved', icon: '&#9989;' },
  REJECTED: { label: 'Rejected', class: 'status-rejected', icon: '&#10060;' },
}

const ocrHasContent = computed(() => {
  const text = prescriptionStore.currentPrescription?.ocrText
  return text && text.trim().length > 0 && !text.startsWith('[')
})

const ocrIsPlaceholder = computed(() => {
  const text = prescriptionStore.currentPrescription?.ocrText
  return text && text.startsWith('[')
})

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

function goBack() {
  router.push('/prescriptions')
}

function downloadFile() {
  if (!prescriptionStore.currentPrescription) return
  const url = getPrescriptionDownloadUrl(
    prescriptionStore.currentPrescription.id,
    patientUserId.value
  )
  window.open(url, '_blank')
}

function startEditing() {
  editedOcrText.value = prescriptionStore.currentPrescription?.ocrText || ''
  saveMessage.value = ''
  editingOcr.value = true
}

function cancelEditing() {
  editingOcr.value = false
  editedOcrText.value = ''
  saveMessage.value = ''
}

async function saveOcrText() {
  saveMessage.value = ''
  await prescriptionStore.updateOcrText(prescriptionId.value, editedOcrText.value)
  if (!prescriptionStore.error) {
    editingOcr.value = false
    saveMessage.value = 'OCR text saved successfully.'
    setTimeout(() => { saveMessage.value = '' }, 3000)
  }
}

watch(() => prescriptionStore.saveSuccess, (val) => {
  if (val) {
    prescriptionStore.clearSaveSuccess()
  }
})

onMounted(() => {
  if (prescriptionId.value) {
    prescriptionStore.fetchPrescription(prescriptionId.value)
  }
  notificationStore.fetchUnreadCount(patientUserId.value)
})
</script>

<template>
  <div class="prescription-detail">
    <header class="page-header">
      <div class="header-left">
        <button class="btn btn-back" @click="goBack">&larr; Back</button>
        <h1>Prescription Details</h1>
      </div>
      <NotificationBell :userId="patientUserId" />
    </header>

    <div v-if="prescriptionStore.loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading prescription...</p>
    </div>

    <div v-else-if="prescriptionStore.error" class="error-state">
      <p>{{ prescriptionStore.error }}</p>
      <button class="btn btn-secondary" @click="prescriptionStore.fetchPrescription(prescriptionId)">
        Retry
      </button>
    </div>

    <div v-else-if="prescriptionStore.currentPrescription" class="detail-content">
      <div
        class="status-banner"
        :class="statusConfig[prescriptionStore.currentPrescription.status]?.class || ''"
      >
        <span
          class="status-icon"
          v-html="statusConfig[prescriptionStore.currentPrescription.status]?.icon"
        ></span>
        <div class="status-text">
          <h2>
            {{ statusConfig[prescriptionStore.currentPrescription.status]?.label }}
          </h2>
          <p>
            <template v-if="prescriptionStore.currentPrescription.status === 'PENDING_REVIEW'">
              Your prescription is awaiting review by a pharmacist.
            </template>
            <template v-else-if="prescriptionStore.currentPrescription.status === 'APPROVED'">
              Your prescription has been approved and is ready for use.
            </template>
            <template v-else-if="prescriptionStore.currentPrescription.status === 'REJECTED'">
              Your prescription has been rejected. Please see the pharmacist's comments below.
            </template>
          </p>
        </div>
      </div>

      <div class="detail-card">
        <h3 class="section-title">File Information</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">File Name</span>
            <span class="detail-value">{{ prescriptionStore.currentPrescription.originalFileName }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">File Type</span>
            <span class="detail-value">{{ prescriptionStore.currentPrescription.fileType }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">File Size</span>
            <span class="detail-value">{{ formatFileSize(prescriptionStore.currentPrescription.fileSize) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Uploaded</span>
            <span class="detail-value">{{ formatDate(prescriptionStore.currentPrescription.createdAt) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">Last Updated</span>
            <span class="detail-value">{{ formatDate(prescriptionStore.currentPrescription.updatedAt) }}</span>
          </div>
          <div class="detail-item" v-if="prescriptionStore.currentPrescription.pharmacistId">
            <span class="detail-label">Reviewed by Pharmacist</span>
            <span class="detail-value">ID: {{ prescriptionStore.currentPrescription.pharmacistId }}</span>
          </div>
        </div>
        <button class="btn btn-primary download-btn" @click="downloadFile">
          &#128229; Download File
        </button>
      </div>

      <div class="detail-card ocr-card">
        <div class="ocr-header">
          <h3 class="section-title">Extracted Text (OCR)</h3>
          <button
            v-if="!editingOcr"
            class="btn btn-edit"
            @click="startEditing"
          >
            &#9998; {{ ocrHasContent ? 'Correct' : 'Enter Text' }}
          </button>
        </div>

        <div v-if="prescriptionStore.saving" class="ocr-saving">
          <div class="small-spinner"></div>
          <span>Saving...</span>
        </div>

        <div v-else-if="editingOcr" class="ocr-editor">
          <textarea
            v-model="editedOcrText"
            class="ocr-textarea"
            placeholder="Type or paste the prescription text here..."
            rows="8"
          ></textarea>
          <div class="ocr-editor-actions">
            <div class="ocr-editor-hint">
              {{ editedOcrText.length }} / 2000 characters
            </div>
            <div class="ocr-editor-buttons">
              <button class="btn btn-secondary" @click="cancelEditing">Cancel</button>
              <button class="btn btn-primary" @click="saveOcrText">Save</button>
            </div>
          </div>
          <p v-if="prescriptionStore.error" class="ocr-error">{{ prescriptionStore.error }}</p>
        </div>

        <div v-else-if="ocrHasContent" class="ocr-display">
          <pre class="ocr-text">{{ prescriptionStore.currentPrescription.ocrText }}</pre>
        </div>

        <div v-else-if="ocrIsPlaceholder" class="ocr-placeholder">
          <p class="ocr-placeholder-text">{{ prescriptionStore.currentPrescription.ocrText }}</p>
          <button class="btn btn-primary" @click="startEditing">
            Enter Prescription Text
          </button>
        </div>

        <div v-else class="ocr-empty">
          <p class="ocr-empty-text">No text has been extracted from this prescription yet.</p>
          <button class="btn btn-primary" @click="startEditing">
            Enter Text Manually
          </button>
        </div>

        <p v-if="saveMessage" class="ocr-success">{{ saveMessage }}</p>
      </div>

      <div
        v-if="prescriptionStore.currentPrescription.pharmacistComments"
        class="detail-card comment-card"
      >
        <h3 class="section-title">Pharmacist Comments</h3>
        <div class="comment-body">
          {{ prescriptionStore.currentPrescription.pharmacistComments }}
        </div>
      </div>

      <div v-else-if="prescriptionStore.currentPrescription.status === 'REJECTED'" class="detail-card comment-card no-comment">
        <h3 class="section-title">Pharmacist Comments</h3>
        <p class="no-comment-text">No comments were provided by the pharmacist.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.prescription-detail {
  max-width: 800px;
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
  align-items: center;
  gap: 16px;
}

.header-left h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
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

.btn-primary {
  background: var(--color-primary);
  color: #fff;
}

.btn-primary:hover {
  background: var(--color-primary-hover);
}

.btn-secondary {
  background: var(--color-bg);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover {
  background: var(--color-border);
}

.loading-state,
.error-state {
  text-align: center;
  padding: 80px 24px;
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

.error-state p {
  color: var(--color-danger);
  margin-bottom: 16px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-banner {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  border-radius: var(--radius-lg);
  border: 1px solid transparent;
}

.status-banner.status-pending {
  background: #fffbeb;
  border-color: #fde68a;
}

.status-banner.status-approved {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.status-banner.status-rejected {
  background: #fef2f2;
  border-color: #fecaca;
}

.status-icon {
  font-size: 36px;
  flex-shrink: 0;
  line-height: 1;
}

.status-text h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.status-text p {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.5;
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
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
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

.comment-card {
  border-left: 4px solid var(--color-primary);
}

.comment-body {
  font-size: 15px;
  color: var(--color-text);
  line-height: 1.7;
  white-space: pre-wrap;
}

.no-comment {
  border-left-color: var(--color-border);
}

.no-comment-text {
  font-size: 14px;
  color: var(--color-text-muted);
  font-style: italic;
}

.ocr-card {
  border-left: 4px solid var(--color-warning);
}

.ocr-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.ocr-header .section-title {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.btn-edit {
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 500;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}

.btn-edit:hover {
  background: var(--color-primary-bg);
}

.ocr-saving {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 0;
  color: var(--color-text-muted);
  font-size: 14px;
}

.small-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.ocr-editor {
  margin-top: 16px;
}

.ocr-textarea {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: 'Courier New', Courier, monospace;
  line-height: 1.6;
  color: var(--color-text);
  background: var(--color-bg);
  resize: vertical;
  min-height: 140px;
}

.ocr-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.ocr-editor-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  gap: 12px;
  flex-wrap: wrap;
}

.ocr-editor-hint {
  font-size: 12px;
  color: var(--color-text-muted);
}

.ocr-editor-buttons {
  display: flex;
  gap: 8px;
}

.ocr-error {
  margin-top: 8px;
  font-size: 13px;
  color: var(--color-danger);
}

.ocr-success {
  margin-top: 12px;
  font-size: 13px;
  color: var(--color-success);
  font-weight: 500;
}

.ocr-display {
  margin-top: 16px;
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.ocr-text {
  font-size: 14px;
  font-family: 'Courier New', Courier, monospace;
  line-height: 1.7;
  color: var(--color-text);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.ocr-placeholder {
  margin-top: 16px;
  text-align: center;
  padding: 24px 16px;
}

.ocr-placeholder-text {
  font-size: 14px;
  color: var(--color-text-muted);
  font-style: italic;
  line-height: 1.6;
  margin-bottom: 16px;
}

.ocr-empty {
  margin-top: 16px;
  text-align: center;
  padding: 24px 16px;
}

.ocr-empty-text {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-bottom: 16px;
}
</style>
