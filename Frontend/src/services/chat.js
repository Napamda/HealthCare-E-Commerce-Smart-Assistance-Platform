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
