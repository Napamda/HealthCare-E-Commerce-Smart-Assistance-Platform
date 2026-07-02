import axios from 'axios'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

/**
 * Send a message to the AI and get a response.
 *
 * @param {object}  payload
 * @param {string}  payload.message        – user's message text (required)
 * @param {number}  [payload.userId]        – user id (defaults to 1)
 * @param {number}  [payload.conversationId] – existing conversation id (null = new)
 * @returns {Promise<object>} ChatResponse
 */
export function sendMessage({ message, userId = 1, conversationId = null }) {
  return apiClient
    .post('/api/chat', { message, userId, conversationId })
    .then((res) => res.data)
}

/**
 * Health-check / connectivity test.
 */
export function testConnection(message) {
  return apiClient
    .post('/api/chat/test', { message })
    .then((res) => res.data)
}

/**
 * List all conversations for a user (paginated).
 *
 * @param {number} [userId=1]
 * @returns {Promise<Array>} ConversationResponse[]
 */
export function listConversations(userId = 1) {
  return apiClient
    .get('/api/chat/conversations', { params: { userId } })
    .then((res) => res.data)
}

/**
 * Get a single conversation with its full message history.
 *
 * @param {number} id – conversation id
 * @returns {Promise<object>} { id, title, status, createdAt, updatedAt, messages[] }
 */
export function getConversation(id) {
  return apiClient
    .get(`/api/chat/conversations/${id}`)
    .then((res) => res.data)
}

/**
 * Soft-delete a conversation.
 *
 * @param {number} id – conversation id
 */
export function deleteConversation(id) {
  return apiClient.delete(`/api/chat/conversations/${id}`)
}
