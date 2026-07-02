<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useChatStore } from '../stores/chat.js'
import ChatSidebar from '../components/chat/ChatSidebar.vue'
import ChatMessage from '../components/chat/ChatMessage.vue'
import ChatInput from '../components/chat/ChatInput.vue'

const route = useRoute()
const store = useChatStore()
const {
  messages,
  isSending,
  isLoadingConversation,
  error,
  activeConversation,
} = storeToRefs(store)

const chatBodyRef = ref(null)
const chatInputRef = ref(null)

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
</script>

<template>
  <div class="chat-layout">
    <!-- Sidebar -->
    <ChatSidebar />

    <!-- Main chat area -->
    <main class="chat-main">
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
</style>
