import apiClient from './api.js'

export function uploadPrescription(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)

  return apiClient.post('/api/prescriptions/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    },
  }).then((res) => res.data)
}

export function getPrescription(id) {
  return apiClient.get(`/api/prescriptions/${id}`).then((res) => res.data)
}

export function getMyPrescriptions() {
  return apiClient
    .get('/api/prescriptions/mine')
    .then((res) => res.data)
}

export function getPatientPrescriptions(patientUserId) {
  // Used by doctor/pharmacist UI when looking up a specific patient's list.
  // For a patient listing their own list, prefer getMyPrescriptions().
  return apiClient
    .get(`/api/prescriptions/patient/${patientUserId}`)
    .then((res) => res.data)
}

export function getPrescriptionDownloadUrl(id) {
  const base = import.meta.env.VITE_API_BASE_URL || ''
  // Backend derives userId from the authenticated session.
  return `${base}/api/prescriptions/${id}/download`
}

export function updateOcrText(id, ocrText) {
  return apiClient
    .patch(`/api/prescriptions/${id}/ocr`, { ocrText })
    .then((res) => res.data)
}

export function getPendingPrescriptions() {
  return apiClient
    .get('/api/prescriptions/pharmacist/pending')
    .then((res) => res.data)
}

export function searchPrescriptions(params = {}) {
  return apiClient
    .get('/api/prescriptions/pharmacist/search', { params })
    .then((res) => res.data)
}

export function reviewPrescription(id, reviewData) {
  return apiClient
    .patch(`/api/prescriptions/${id}/review`, reviewData)
    .then((res) => res.data)
}

// Patient adds the pharmacist-selected medications to their own cart.
export function orderPrescription(id) {
  return apiClient
    .post(`/api/prescriptions/${id}/order`)
    .then((res) => res.data)
}
