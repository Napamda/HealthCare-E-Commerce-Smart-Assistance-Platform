import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  listDoctorConversations,
  getDoctorConversation,
  sendDoctorMessage,
} from '../services/doctorChat.js'

export const useDoctorChatStore = defineStore('doctorChat', () => {
  const conversations = ref([])
  const activeConversationId = ref(null)
  const activeConversation = ref(null)
  const messages = ref([])
  const session = ref(null)
  const isLoadingList = ref(false)
  const isLoadingConversation = ref(false)
  const isSending = ref(false)
  const error = ref(null)

  const sortedConversations = computed(() =>
    [...conversations.value].sort(
      (a, b) => new Date(b.updatedAt) - new Date(a.updatedAt),
    ),
  )

  async function fetchConversations() {
    isLoadingList.value = true
    error.value = null
    try {
      conversations.value = await listDoctorConversations()
    } catch (err) {
      error.value = err.response?.data?.error || err.message
    } finally {
      isLoadingList.value = false
    }
  }

  async function loadConversation(conversationId) {
    isLoadingConversation.value = true
    error.value = null
    activeConversationId.value = conversationId
    activeConversation.value =
      conversations.value.find((c) => c.id === conversationId) ?? null

    try {
      const data = await getDoctorConversation(conversationId)
      messages.value = data.messages ?? []
      session.value = data.session ?? null
      if (data.session?.patientName) {
        activeConversation.value = {
          ...(activeConversation.value || {}),
          patientName: data.session.patientName,
        }
      }
    } catch (err) {
      error.value = err.response?.data?.error || err.message
      messages.value = []
    } finally {
      isLoadingConversation.value = false
    }
  }

  async function sendReply(content) {
    if (!content?.trim() || activeConversationId.value == null) return
    isSending.value = true
    error.value = null

    // Optimistically add the doctor message to the UI with a temporary id
    const tempId = `temp-${Date.now()}`
    const optimisticMsg = {
      id: tempId,
      role: 'DOCTOR',
      content,
      timestamp: new Date().toISOString(),
    }
    messages.value.push(optimisticMsg)

    try {
      const response = await sendDoctorMessage(activeConversationId.value, content)
      // Replace the optimistic message with the persisted one
      const idx = messages.value.findIndex((m) => m.id === tempId)
      if (idx !== -1) {
        messages.value[idx] = {
          id: response.messageId,
          role: 'DOCTOR',
          content: response.response,
          timestamp: response.timestamp,
          provider: response.provider,
        }
      }
      await fetchConversations()
    } catch (err) {
      error.value = err.response?.data?.error || err.message
      // Remove the optimistic message on failure
      messages.value = messages.value.filter((m) => m.id !== tempId)
    } finally {
      isSending.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function resetActive() {
    activeConversationId.value = null
    activeConversation.value = null
    messages.value = []
    session.value = null
    error.value = null
  }

  return {
    conversations,
    activeConversationId,
    activeConversation,
    messages,
    session,
    isLoadingList,
    isLoadingConversation,
    isSending,
    error,
    sortedConversations,
    fetchConversations,
    loadConversation,
    sendReply,
    clearError,
    resetActive,
  }
})
