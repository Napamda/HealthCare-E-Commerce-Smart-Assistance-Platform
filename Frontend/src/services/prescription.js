import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
})

export function uploadPrescription(file, patientUserId, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('patientUserId', patientUserId)

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

export function getPatientPrescriptions(patientUserId) {
  return apiClient
    .get(`/api/prescriptions/patient/${patientUserId}`)
    .then((res) => res.data)
}

export function getPrescriptionDownloadUrl(id, userId) {
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return `${base}/api/prescriptions/${id}/download?userId=${userId}`
}

export function updateOcrText(id, ocrText) {
  return apiClient
    .patch(`/api/prescriptions/${id}/ocr`, { ocrText })
    .then((res) => res.data)
}
