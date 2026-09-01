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
          <span v-if="ocrText" class="success-ocr-label">OCR text extracted ({{ ocrText.length }} chars)</span>
          <span v-else-if="ocrAttempted" class="success-ocr-label no-ocr">OCR could not extract text from this image</span>
        </div>
      </div>

      <div v-if="success && ocrText" class="ocr-preview">
        <div class="ocr-preview-header">
          <h3>Extracted Text</h3>
          <router-link :to="'/prescriptions/' + uploadedId" class="ocr-view-link">View &amp; correct &#8594;</router-link>
        </div>
        <pre class="ocr-preview-text">{{ ocrText }}</pre>
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
const ocrText = ref('')
const ocrAttempted = ref(false)

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
  ocrText.value = ''
  ocrAttempted.value = false
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
    ocrText.value = result.ocrText || ''
    ocrAttempted.value = result.ocrAttempted || false
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


