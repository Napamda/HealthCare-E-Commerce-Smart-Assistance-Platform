<script setup>
import { ref, watch, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useDoctorChatStore } from '../../stores/doctorChat.js'
import ChatMessage from '../../components/chat/ChatMessage.vue'
import ChatInput from '../../components/chat/ChatInput.vue'

const route = useRoute()
const router = useRouter()
const store = useDoctorChatStore()
const {
  conversations,
  activeConversationId,
  activeConversation,
  messages,
  session,
  isLoadingList,
  isLoadingConversation,
  isSending,
  error,
} = storeToRefs(store)

const chatBodyRef = ref(null)
const chatInputRef = ref(null)

const sortedConversations = computed(() => store.sortedConversations)

onMounted(async () => {
  await store.fetchConversations()

  const routeId = route.params.conversationId
  if (routeId) {
    await store.loadConversation(Number(routeId))
  }
})

watch(
  () => route.params.conversationId,
  async (newId) => {
    if (newId) {
      await store.loadConversation(Number(newId))
    } else {
      store.resetActive()
    }
  },
)

watch(
  () => messages.value.length,
  () => {
    nextTick(() => scrollToBottom())
  },
)

function scrollToBottom() {
  const el = chatBodyRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

function onSelectConversation(conversationId) {
  router.push(`/doctor/chat/${conversationId}`)
}

function onSend(text) {
  store.sendReply(text)
}

function onRetry() {
  store.clearError()
}

function getStatusLabel(status) {
  const map = {
    PENDING: 'Pending Review',
    ACCEPTED: 'Accepted',
    REJECTED: 'Rejected',
    IN_PROGRESS: 'In Progress',
    CLOSED: 'Closed',
  }
  return map[status] || status
}

function formatDate(isoString) {
  if (!isoString) return ''
  const date = new Date(isoString)
  const now = new Date()
  const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24))
  if (diffDays === 0) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } else if (diffDays < 7) {
    return date.toLocaleDateString([], { weekday: 'short' })
  }
  return date.toLocaleDateString([], { month: 'short', day: 'numeric' })
}
</script>

<template>
  <div class="doctor-chat-layout">
    <!-- Sidebar: doctor's assigned patient conversations -->
    <aside class="doctor-chat-sidebar">
      <div class="doctor-chat-sidebar-header">
        <h2 class="doctor-chat-sidebar-title">Patient Chats</h2>
        <button class="btn-refresh-doctor" :disabled="isLoadingList" @click="store.fetchConversations()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 4 23 10 17 10" />
            <polyline points="1 20 1 14 7 14" />
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
          </svg>
        </button>
      </div>

      <div class="doctor-conv-list">
        <div v-if="isLoadingList && sortedConversations.length === 0" class="doctor-conv-empty">
          Loading consultations...
        </div>
        <div v-else-if="sortedConversations.length === 0" class="doctor-conv-empty">
          No active patient consultations yet.
          <p class="doctor-conv-empty-hint">
            Accept a consultation from your queue to start chatting with the patient.
          </p>
          <router-link to="/doctor" class="btn-link">Go to consultation queue</router-link>
        </div>

        <div
          v-for="conv in sortedConversations"
          :key="conv.id"
          class="doctor-conv-item"
          :class="{ active: conv.id === activeConversationId }"
          @click="onSelectConversation(conv.id)"
        >
          <div class="doctor-conv-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
          </div>
          <div class="doctor-conv-details">
            <span class="doctor-conv-title">{{ conv.patientName || conv.title || 'Conversation' }}</span>
            <span class="doctor-conv-meta">
              <span v-if="conv.consultationStatus" class="conv-status-pill" :class="'status-' + conv.consultationStatus.toLowerCase()">
                {{ getStatusLabel(conv.consultationStatus) }}
              </span>
              <span>{{ conv.messageCount }} msgs &middot; {{ formatDate(conv.updatedAt) }}</span>
            </span>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main chat area -->
    <main class="doctor-chat-main">
      <!-- Active session banner -->
      <div v-if="session" class="doctor-session-banner" :class="'status-' + (session.status || '').toLowerCase()">
        <div class="banner-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
          </svg>
        </div>
        <div class="banner-text">
          <span class="banner-label">
            Consultation #{{ session.consultationId }}
          </span>
          <span class="banner-status">
            {{ getStatusLabel(session.status) }}<span v-if="session.doctorName"> · {{ session.doctorName }}</span>
          </span>
        </div>
        <span v-if="session.priority" class="banner-priority" :class="'priority-' + session.priority.toLowerCase()">
          {{ session.priority }}
        </span>
      </div>

      <!-- Loading skeleton -->
      <div v-if="isLoadingConversation" class="chat-loading">
        <div class="dot-typing"><span></span><span></span><span></span></div>
        <p>Loading patient chat...</p>
      </div>

      <!-- Error banner -->
      <div v-if="error" class="error-banner">
        <span>{{ error }}</span>
        <button class="btn-dismiss" @click="onRetry">Dismiss</button>
      </div>

      <!-- Messages area -->
      <div
        v-if="!isLoadingConversation"
        ref="chatBodyRef"
        class="chat-body"
      >
        <!-- Empty state -->
        <div v-if="messages.length === 0 && activeConversationId" class="chat-empty">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
          </div>
          <h2>No messages yet</h2>
          <p class="empty-sub">
            Reply to start the consultation. Your messages will be labeled as coming from the doctor.
          </p>
        </div>

        <!-- No conversation selected -->
        <div v-else-if="!activeConversationId" class="chat-empty">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <polyline points="16 11 19 14 23 10" />
            </svg>
          </div>
          <h2>Select a patient chat</h2>
          <p class="empty-sub">
            Choose a conversation from the sidebar to review the patient's chat history and reply.
          </p>
        </div>

        <!-- Message list -->
        <div v-else class="message-list">
          <ChatMessage
            v-for="msg in messages"
            :key="msg.id"
            :message="msg"
          />
          <div v-if="isSending" class="message-row doctor">
            <div class="avatar avatar-doctor">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
            </div>
            <div class="bubble-wrapper">
              <div class="bubble bubble-doctor">
                <div class="dot-typing"><span></span><span></span><span></span></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Input bar — only shown when a conversation is loaded and the session is active -->
      <ChatInput
        v-if="!isLoadingConversation && activeConversationId && session"
        ref="chatInputRef"
        placeholder="Reply to the patient. Your message will be labeled as from the doctor."
        hint="Your replies are visible to the patient in their chat."
        @send="onSend"
      />
    </main>
  </div>
</template>
