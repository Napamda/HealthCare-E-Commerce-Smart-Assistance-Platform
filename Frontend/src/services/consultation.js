import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function escalateToDoctor({ conversationId, reason, priority = 'NORMAL' }) {
  return apiClient
    .post('/api/consultations/escalate', {
      conversationId,
      reason,
      priority,
    })
    .then((res) => res.data)
}

export function getConsultation(id) {
  return apiClient.get(`/api/consultations/${id}`).then((res) => res.data)
}

export function getPatientConsultations(patientUserId) {
  return apiClient
    .get(`/api/consultations/patient/${patientUserId}`)
    .then((res) => res.data)
}

export function updateConsultationStatus(id, status) {
  return apiClient
    .patch(`/api/consultations/${id}/status`, null, {
      params: { status },
    })
    .then((res) => res.data)
}
