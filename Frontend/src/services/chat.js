import apiClient from './api.js'

export function sendMessage({ message, conversationId = null }) {
  return apiClient
    .post('/api/chat', { message, conversationId })
    .then((res) => res.data)
}

export function listConversations() {
  return apiClient
    .get('/api/chat/conversations')
    .then((res) => res.data)
}

export function getConversation(id) {
  return apiClient
    .get(`/api/chat/conversations/${id}`)
    .then((res) => res.data)
}

export function deleteConversation(id) {
  return apiClient.delete(`/api/chat/conversations/${id}`)
}
