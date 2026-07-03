<template>
  <div class="upload-page">
    <div class="upload-container">
      <h1 class="upload-title">Upload Prescription</h1>
      <p class="upload-subtitle">
        Upload a photo or PDF of your prescription for review.
      </p>

      <div
        class="drop-zone"
        :class="{
          'drop-zone--active': isDragging,
          'drop-zone--has-file': selectedFile && !error,
          'drop-zone--error': error,
          'drop-zone--uploading': isUploading,
        }"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="onFileDrop"
        @click="triggerFileInput"
      >
        <input
          ref="fileInput"
          type="file"
          class="file-input-hidden"
          accept=".jpg,.jpeg,.png,.gif,.webp,.pdf"
          @change="onFileSelected"
        />

        <template v-if="!selectedFile">
          <div class="drop-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="17 8 12 3 7 8" />
              <line x1="12" y1="3" x2="12" y2="15" />
            </svg>
          </div>
          <p class="drop-text">Drag &amp; drop your prescription here</p>
          <p class="drop-hint">or click to browse — JPG, PNG, GIF, WebP, PDF (max 10 MB)</p>
        </template>

        <template v-else>
          <div class="file-preview">
            <div class="file-icon">
              <svg v-if="isImageFile" width="32" height="32" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
              <svg v-else width="32" height="32" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
            </div>
            <div class="file-details">
              <span class="file-name">{{ selectedFile.name }}</span>
              <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
            </div>
            <button
              v-if="!isUploading"
              class="btn-remove-file"
              @click.stop="clearFile"
              title="Remove file"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
        </template>
      </div>

      <div v-if="isUploading" class="progress-section">
        <div class="progress-bar-track">
          <div
            class="progress-bar-fill"
            :class="{ 'progress-bar-fill--done': uploadProgress === 100 }"
            :style="{ width: uploadProgress + '%' }"
          ></div>
        </div>
        <div class="progress-info">
          <span class="progress-label">{{ progressLabel }}</span>
          <span class="progress-percent">{{ uploadProgress }}%</span>
        </div>
      </div>

      <div v-if="error" class="alert alert--error">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="15" y1="9" x2="9" y2="15" />
          <line x1="9" y1="9" x2="15" y2="15" />
        </svg>
        <span>{{ error }}</span>
      </div>

      <div v-if="success" class="alert alert--success">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
          <polyline points="22 4 12 14.01 9 11.01" />
        </svg>
        <div class="success-details">
          <span class="success-title">Prescription uploaded successfully!</span>
          <span class="success-subtitle">ID: #{{ uploadedId }} &mdash; Your prescription is pending review.</span>
        </div>
      </div>

      <div class="upload-actions">
        <button
          class="btn btn--primary"
          :disabled="!selectedFile || isUploading || !!success"
          @click="uploadFile"
        >
          <svg v-if="isUploading" class="spinner" width="18" height="18" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" stroke-dasharray="31.4" stroke-dashoffset="9.4" />
          </svg>
          <span>{{ uploadButtonText }}</span>
        </button>
        <button
          v-if="success"
          class="btn btn--secondary"
          @click="resetForm"
        >
          Upload Another
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { uploadPrescription } from '../services/prescription.js'

const fileInput = ref(null)
const selectedFile = ref(null)
const isDragging = ref(false)
const isUploading = ref(false)
const uploadProgress = ref(0)
const error = ref('')
const success = ref(false)
const uploadedId = ref(null)

const isImageFile = computed(() => {
  if (!selectedFile.value) return false
  return selectedFile.value.type.startsWith('image/')
})

const progressLabel = computed(() => {
  if (uploadProgress.value === 0) return 'Preparing...'
  if (uploadProgress.value < 100) return 'Uploading...'
  return 'Processing...'
})

const uploadButtonText = computed(() => {
  if (isUploading.value) return 'Uploading...'
  if (success.value) return 'Uploaded'
  return 'Upload Prescription'
})

function triggerFileInput() {
  if (isUploading.value || success.value) return
  fileInput.value?.click()
}

function onFileSelected(event) {
  const file = event.target.files?.[0]
  if (file) setFile(file)
}

function onFileDrop(event) {
  isDragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) setFile(file)
}

function setFile(file) {
  error.value = ''
  success.value = false

  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'application/pdf']
  if (!allowedTypes.includes(file.type)) {
    error.value = 'Unsupported file type. Allowed: JPG, PNG, GIF, WebP, PDF.'
    return
  }

  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    error.value = `File size (${formatFileSize(file.size)}) exceeds the 10 MB limit.`
    return
  }

  selectedFile.value = file
}

function clearFile() {
  selectedFile.value = null
  error.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

function resetForm() {
  clearFile()
  success.value = false
  uploadProgress.value = 0
  uploadedId.value = null
}

async function uploadFile() {
  if (!selectedFile.value) return

  isUploading.value = true
  error.value = ''
  uploadProgress.value = 0

  try {
    const result = await uploadPrescription(
      selectedFile.value,
      1,
      (percent) => {
        uploadProgress.value = percent
      },
    )
    uploadProgress.value = 100
    uploadedId.value = result.prescriptionId
    success.value = true
  } catch (err) {
    const message = err.response?.data?.error || err.message || 'Upload failed. Please try again.'
    error.value = message
    uploadProgress.value = 0
  } finally {
    isUploading.value = false
  }
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<style scoped>
.upload-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f8fafc;
}

.upload-container {
  width: 100%;
  max-width: 520px;
  background: #ffffff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 8px 24px rgba(0, 0, 0, 0.04);
}

.upload-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px;
}

.upload-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 28px;
}

.drop-zone {
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  padding: 40px 24px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.drop-zone--active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.drop-zone--has-file {
  border-style: solid;
  border-color: #e2e8f0;
  padding: 20px 24px;
  cursor: default;
}

.drop-zone--error {
  border-color: #fca5a5;
  background: #fef2f2;
}

.drop-zone--uploading {
  border-color: #93c5fd;
  background: #f8fafc;
  cursor: not-allowed;
  pointer-events: none;
}

.file-input-hidden {
  display: none;
}

.drop-icon {
  color: #94a3b8;
  margin-bottom: 16px;
}

.drop-text {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 8px;
}

.drop-hint {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

.file-preview {
  display: flex;
  align-items: center;
  gap: 14px;
}

.file-icon {
  color: #64748b;
  display: flex;
  flex-shrink: 0;
}

.file-details {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 12px;
  color: #94a3b8;
}

.btn-remove-file {
  padding: 6px;
  border-radius: 6px;
  color: #94a3b8;
  transition: color 0.15s, background 0.15s;
  flex-shrink: 0;
}
.btn-remove-file:hover {
  color: #ef4444;
  background: #fef2f2;
}

.progress-section {
  margin-top: 20px;
}

.progress-bar-track {
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #2563eb);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-bar-fill--done {
  background: linear-gradient(90deg, #22c55e, #16a34a);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
}

.progress-label {
  font-size: 13px;
  color: #64748b;
}

.progress-percent {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.alert {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 20px;
  padding: 14px 16px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.5;
}

.alert--error {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.alert--success {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.success-details {
  display: flex;
  flex-direction: column;
}

.success-title {
  font-weight: 600;
}

.success-subtitle {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.upload-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
  border: none;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn--primary {
  background: #2563eb;
  color: #ffffff;
  flex: 1;
}
.btn--primary:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn--secondary {
  background: #f1f5f9;
  color: #475569;
}
.btn--secondary:hover {
  background: #e2e8f0;
}

.spinner {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
