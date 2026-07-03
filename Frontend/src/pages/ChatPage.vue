<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useChatStore } from '../stores/chat.js'
import { useConsultationStore } from '../stores/consultation.js'
import ChatSidebar from '../components/chat/ChatSidebar.vue'
import ChatMessage from '../components/chat/ChatMessage.vue'
import ChatInput from '../components/chat/ChatInput.vue'
import EscalateDialog from '../components/chat/EscalateDialog.vue'

const route = useRoute()
const store = useChatStore()
const consultationStore = useConsultationStore()
const {
  messages,
  isSending,
  isLoadingConversation,
  error,
  activeConversation,
  activeConversationId,
} = storeToRefs(store)
const { activeConsultation, isEscalating } = storeToRefs(consultationStore)

const chatBodyRef = ref(null)
const chatInputRef = ref(null)
const showEscalateDialog = ref(false)

// On mount: load conversations + route-based conversation
onMounted(async () => {
  await store.fetchConversations()

  const routeId = route.params.id
  if (routeId) {
    await store.loadConversation(Number(routeId))
  }
})

// Watch route param changes
watch(
  () => route.params.id,
  async (newId) => {
    if (newId) {
      await store.loadConversation(Number(newId))
    } else {
      store.newConversation()
    }
  },
)

// Auto-scroll to bottom whenever messages change
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

function onSend(text) {
  store.sendUserMessage(text)
}

function onRetry() {
  store.clearError()
}

function openEscalateDialog() {
  consultationStore.clearEscalationError()
  showEscalateDialog.value = true
}

function onEscalateClose() {
  showEscalateDialog.value = false
}

function onEscalated() {
  showEscalateDialog.value = false
}

function dismissConsultationBanner() {
  consultationStore.resetActiveConsultation()
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
</script>

<template>
  <div class="chat-layout">
    <!-- Sidebar -->
    <ChatSidebar />

    <!-- Main chat area -->
    <main class="chat-main">
      <!-- Consultation banner -->
      <div v-if="activeConsultation" class="consultation-banner" :class="'status-' + activeConsultation.status.toLowerCase()">
        <div class="banner-content">
          <div class="banner-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
            </svg>
          </div>
          <div class="banner-text">
            <span class="banner-label">Consultation #{{ activeConsultation.id }}</span>
            <span class="banner-status">{{ getStatusLabel(activeConsultation.status) }}</span>
          </div>
          <span class="banner-priority" :class="'priority-' + activeConsultation.priority.toLowerCase()">
            {{ activeConsultation.priority }}
          </span>
        </div>
        <button class="banner-dismiss" @click="dismissConsultationBanner" title="Dismiss">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <!-- Loading skeleton -->
      <div v-if="isLoadingConversation" class="chat-loading">
        <div class="dot-typing">
          <span></span><span></span><span></span>
        </div>
        <p>Loading conversation...</p>
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
        <!-- Escalation toolbar (visible when conversation has messages) -->
        <div v-if="messages.length > 0" class="chat-toolbar">
          <button
            class="btn-escalate-toolbar"
            :disabled="isEscalating || !!activeConsultation"
            @click="openEscalateDialog"
            :title="activeConsultation ? 'Consultation already active' : 'Escalate to a doctor'"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <polyline points="16 11 19 14 23 10" />
            </svg>
            {{ activeConsultation ? 'Escalated' : 'Escalate to Doctor' }}
          </button>
        </div>
        <!-- Empty state -->
        <div v-if="messages.length === 0" class="chat-empty">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="4" />
              <circle cx="9" cy="10" r="1" />
              <circle cx="15" cy="10" r="1" />
              <path d="M9 15c.83.67 2 1 3 1s2.17-.33 3-1" />
            </svg>
          </div>
          <h2>Welcome to HealthCare AI</h2>
          <p class="empty-sub">
            Describe your symptoms or ask a health-related question to get started.
          </p>
          <div class="suggestion-chips">
            <button
              class="chip"
              @click="onSend('I have a headache and slight fever. What should I do?')"
            >
              Headache &amp; fever
            </button>
            <button
              class="chip"
              @click="onSend('Can you recommend products for dry skin?')"
            >
              Dry skin products
            </button>
            <button
              class="chip"
              @click="onSend('What are common cold remedies?')"
            >
              Cold remedies
            </button>
            <button
              class="chip"
              @click="onSend('I need general health tips for better sleep.')"
            >
              Sleep tips
            </button>
          </div>
        </div>

        <!-- Message list -->
        <div v-else class="message-list">
          <ChatMessage
            v-for="msg in messages"
            :key="msg.id"
            :message="msg"
          />

          <!-- Typing indicator -->
          <div v-if="isSending" class="message-row assistant">
            <div class="avatar avatar-ai">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="4" />
                <line x1="9" y1="9" x2="15" y2="9" />
                <line x1="9" y1="13" x2="13" y2="13" />
              </svg>
            </div>
            <div class="bubble bubble-ai">
              <div class="dot-typing">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Input bar -->
      <ChatInput
        v-if="!isLoadingConversation"
        ref="chatInputRef"
        @send="onSend"
      />
    </main>

    <!-- Escalate dialog -->
    <EscalateDialog
      v-if="showEscalateDialog"
      :conversation-id="activeConversationId"
      @close="onEscalateClose"
      @escalated="onEscalated"
    />
  </div>
</template>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* --- Main chat area --- */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--color-surface);
}

/* Loading */
.chat-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  gap: 12px;
  font-size: 14px;
}

/* Error banner */
.error-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #fef2f2;
  color: var(--color-danger);
  font-size: 13px;
  border-bottom: 1px solid #fecaca;
}
.btn-dismiss {
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  background: var(--color-danger);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}

/* --- Chat body (scrollable) --- */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* Empty state */
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  gap: 12px;
  padding: 40px 20px;
}
.empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 8px;
}
.chat-empty h2 {
  font-size: 22px;
  font-weight: 700;
}
.empty-sub {
  color: var(--color-text-secondary);
  font-size: 14px;
  max-width: 400px;
}

/* Suggestion chips */
.suggestion-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 12px;
  max-width: 500px;
}
.chip {
  padding: 8px 16px;
  border-radius: var(--radius-full);
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 500;
  border: 1px solid transparent;
  transition: border-color 0.15s, background 0.15s;
}
.chip:hover {
  border-color: var(--color-primary-light);
  background: #dbeafe;
}

/* Message list */
.message-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* Typing indicator (inline styles; ChatMessage scoped styles won't apply here) */
.message-row {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  max-width: 90%;
  align-self: flex-start;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.avatar-ai {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}
.bubble {
  padding: 12px 16px;
  border-radius: var(--radius-lg);
}
.bubble-ai {
  background: var(--color-ai-bubble);
  border: 1px solid var(--color-border);
  border-bottom-left-radius: var(--radius-sm);
}

/* Consultation banner */
.consultation-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  font-size: 13px;
  border-bottom: 1px solid var(--color-border);
}
.consultation-banner.status-pending {
  background: #fffbeb;
  border-bottom-color: #fde68a;
}
.consultation-banner.status-accepted,
.consultation-banner.status-in_progress {
  background: #ecfdf5;
  border-bottom-color: #a7f3d0;
}
.consultation-banner.status-rejected {
  background: #fef2f2;
  border-bottom-color: #fecaca;
}
.consultation-banner.status-closed {
  background: #f8fafc;
  border-bottom-color: #e2e8f0;
}

.banner-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.banner-icon {
  color: var(--color-warning);
  display: flex;
}
.status-accepted .banner-icon,
.status-in_progress .banner-icon {
  color: var(--color-success);
}
.status-rejected .banner-icon {
  color: var(--color-danger);
}

.banner-text {
  display: flex;
  flex-direction: column;
}

.banner-label {
  font-weight: 600;
  color: var(--color-text);
}

.banner-status {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.banner-priority {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.priority-low {
  background: #e0e7ff;
  color: #4f46e5;
}
.priority-normal {
  background: #dbeafe;
  color: #2563eb;
}
.priority-high {
  background: #fef3c7;
  color: #d97706;
}
.priority-urgent {
  background: #fecaca;
  color: #dc2626;
}

.banner-dismiss {
  padding: 4px;
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  transition: color 0.15s;
}
.banner-dismiss:hover {
  color: var(--color-text);
}

/* Chat toolbar */
.chat-toolbar {
  display: flex;
  justify-content: flex-end;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 16px;
}

.btn-escalate-toolbar {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  color: #d97706;
  background: #fffbeb;
  border: 1px solid #fde68a;
  transition: background 0.15s, border-color 0.15s;
}
.btn-escalate-toolbar:hover:not(:disabled) {
  background: #fef3c7;
  border-color: #fcd34d;
}
.btn-escalate-toolbar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
