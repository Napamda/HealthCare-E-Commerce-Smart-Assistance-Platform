import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  sendMessage,
  listConversations,
  getConversation,
  deleteConversation,
} from '../services/chat.js'

export const useChatStore = defineStore('chat', () => {
  // ----------------------------------------------------------------
  // State
  // ----------------------------------------------------------------
  const conversations = ref([])
  const activeConversationId = ref(null)
  const messages = ref([])
  const isSending = ref(false)
  const isLoadingConversation = ref(false)
  const error = ref(null)

  // ----------------------------------------------------------------
  // Getters
  // ----------------------------------------------------------------
  const activeConversation = computed(() =>
    conversations.value.find((c) => c.id === activeConversationId.value) ?? null,
  )

  const sortedConversations = computed(() =>
    [...conversations.value].sort(
      (a, b) => new Date(b.updatedAt) - new Date(a.updatedAt),
    ),
  )

  // ----------------------------------------------------------------
  // Actions
  // ----------------------------------------------------------------
  async function fetchConversations() {
    try {
      error.value = null
      const data = await listConversations()
      conversations.value = data
    } catch (err) {
      error.value = err.response?.data?.error || err.message
    }
  }

  async function loadConversation(id) {
    try {
      error.value = null
      isLoadingConversation.value = true
      activeConversationId.value = id

      const data = await getConversation(id)
      // The backend returns messages as an array of objects with role/content/timestamp
      messages.value = data.messages ?? []
    } catch (err) {
      error.value = err.response?.data?.error || err.message
    } finally {
      isLoadingConversation.value = false
    }
  }

  async function sendUserMessage(messageText) {
    if (!messageText?.trim()) return

    // Optimistically add user message to UI
    const userMsg = {
      id: `temp-${Date.now()}`,
      role: 'USER',
      content: messageText,
      timestamp: new Date().toISOString(),
    }
    messages.value.push(userMsg)

    isSending.value = true
    error.value = null

    try {
      const response = await sendMessage({
        message: messageText,
        conversationId: activeConversationId.value,
      })

      // Add AI response to UI
      const aiMsg = {
        id: response.messageId,
        role: 'ASSISTANT',
        content: response.response,
        timestamp: response.timestamp,
        provider: response.provider,
        model: response.model,
      }
      messages.value.push(aiMsg)

      // If this is a new conversation, set the id and refresh the list
      if (!activeConversationId.value && response.conversationId) {
        activeConversationId.value = response.conversationId
        await fetchConversations()
      } else {
        // Refresh to update last message / timestamp
        await fetchConversations()
      }
    } catch (err) {
      error.value = err.response?.data?.error || err.message
      // Remove the optimistic user message on failure
      messages.value.pop()
    } finally {
      isSending.value = false
    }
  }

  async function removeConversation(id) {
    try {
      error.value = null
      await deleteConversation(id)
      conversations.value = conversations.value.filter((c) => c.id !== id)

      if (activeConversationId.value === id) {
        newConversation()
      }
    } catch (err) {
      error.value = err.response?.data?.error || err.message
    }
  }

  function newConversation() {
    activeConversationId.value = null
    messages.value = []
    error.value = null
  }

  function clearError() {
    error.value = null
  }

  return {
    // state
    conversations,
    activeConversationId,
    messages,
    isSending,
    isLoadingConversation,
    error,
    // getters
    activeConversation,
    sortedConversations,
    // actions
    fetchConversations,
    loadConversation,
    sendUserMessage,
    removeConversation,
    newConversation,
    clearError,
  }
})
