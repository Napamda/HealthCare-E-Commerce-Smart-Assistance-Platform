import apiClient from './api.js'

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

export function getDoctorQueue() {
  return apiClient
    .get('/api/consultations/doctor/queue')
    .then((res) => res.data)
}

export function updateConsultationPriority(id, priority) {
  return apiClient
    .patch(`/api/consultations/${id}/priority`, null, {
      params: { priority },
    })
    .then((res) => res.data)
}
