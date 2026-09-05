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
  let pollTimer = null
  const isPolling = ref(false)
  let lastMessageCount = 0

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
      // The backend returns messages as an array of ConversationMessage entities.
      // Map createdAt → timestamp so ChatMessage.vue's formatTime works.
      messages.value = (data.messages ?? []).map((m) => ({
        ...m,
        timestamp: m.createdAt || m.timestamp,
      }))
      lastMessageCount = messages.value.length
      startPolling()
    } catch (err) {
      error.value = err.response?.data?.error || err.message
    } finally {
      isLoadingConversation.value = false
    }
  }

  // ----------------------------------------------------------------
  // Real-time polling — picks up new doctor messages without a page
  // refresh. Lightweight: only replaces messages when the count changes.
  // ----------------------------------------------------------------
  function startPolling(intervalMs = 3000) {
    stopPolling()
    isPolling.value = true
    pollTimer = setInterval(async () => {
      if (!activeConversationId.value || isSending.value) return
      try {
        const data = await getConversation(activeConversationId.value)
        const newMsgs = (data.messages ?? []).map((m) => ({
          ...m,
          timestamp: m.createdAt || m.timestamp,
        }))
        if (newMsgs.length > lastMessageCount) {
          messages.value = newMsgs
          lastMessageCount = newMsgs.length
        }
      } catch (_) {
        // Silent — polling errors shouldn't disrupt the chat
      }
    }, intervalMs)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
    isPolling.value = false
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

      // When a doctor is actively handling the consultation, the backend
      // saves the patient's message but doesn't generate an AI response.
      // Don't push a fake AI bubble — the doctor will reply directly.
      if (response.mode !== 'DOCTOR_ACTIVE') {
        const aiMsg = {
          id: response.messageId,
          role: 'ASSISTANT',
          content: response.response,
          timestamp: response.timestamp,
          provider: response.provider,
          model: response.model,
        }
        messages.value.push(aiMsg)
      }

      // Keep the polling baseline in sync so we don't re-import our own
      // optimistic message on the next poll cycle.
      lastMessageCount = messages.value.length

      // If this is a new conversation, set the id and refresh the list
      if (!activeConversationId.value && response.conversationId) {
        activeConversationId.value = response.conversationId
        await fetchConversations()
        // Start polling for the new conversation so doctor replies are picked up
        lastMessageCount = messages.value.length
        startPolling()
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
    stopPolling()
    activeConversationId.value = null
    messages.value = []
    lastMessageCount = 0
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
    isPolling,
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
    startPolling,
    stopPolling,
  }
})
