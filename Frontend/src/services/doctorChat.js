import apiClient from './api.js'

export function listDoctorConversations() {
  return apiClient
    .get('/api/doctor/conversations')
    .then((res) => res.data)
}

export function getDoctorConversation(conversationId) {
  return apiClient
    .get(`/api/doctor/conversations/${conversationId}`)
    .then((res) => res.data)
}

export function sendDoctorMessage(conversationId, content) {
  return apiClient
    .post(`/api/doctor/conversations/${conversationId}/messages`, { content })
    .then((res) => res.data)
}

export function getDoctorConversationSession(conversationId) {
  return apiClient
    .get(`/api/doctor/conversations/${conversationId}/session`)
    .then((res) => res.data)
}
