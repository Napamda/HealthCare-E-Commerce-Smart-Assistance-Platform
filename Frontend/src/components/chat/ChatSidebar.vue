<script setup>
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useChatStore } from '../../stores/chat.js'

const router = useRouter()
const store = useChatStore()
const { sortedConversations, activeConversationId } = storeToRefs(store)

function selectConversation(id) {
  store.loadConversation(id)
}

function removeConversation(id, event) {
  event.stopPropagation()
  if (confirm('Delete this conversation?')) {
    store.removeConversation(id)
  }
}

function startNewChat() {
  store.newConversation()
}

function goToConsultations() {
  router.push('/consultations')
}

function goToProducts() {
  router.push('/products')
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
  } else {
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' })
  }
}
</script>

<template>
  <aside class="sidebar">
    <!-- Header -->
    <div class="sidebar-header">
      <h2 class="sidebar-title">HealthCare AI</h2>
      <button class="btn-new-chat" @click="startNewChat" title="New conversation">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        <span>New Chat</span>
      </button>
    </div>

    <!-- Conversation list -->
    <div class="conversation-list">
      <div v-if="sortedConversations.length === 0" class="empty-state">
        No conversations yet. Start a new chat!
      </div>

      <div
        v-for="conv in sortedConversations"
        :key="conv.id"
        class="conversation-item"
        :class="{ active: conv.id === activeConversationId }"
        @click="selectConversation(conv.id)"
      >
        <div class="conv-icon">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </div>
        <div class="conv-details">
          <span class="conv-title">{{ conv.title || 'New conversation' }}</span>
          <span class="conv-meta">
            {{ conv.messageCount }} messages &middot; {{ formatDate(conv.updatedAt) }}
          </span>
        </div>
        <button class="btn-delete" @click="removeConversation(conv.id, $event)" title="Delete">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Bottom nav -->
    <div class="sidebar-footer">
      <button class="btn-nav-link" @click="goToProducts">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1" />
          <circle cx="20" cy="21" r="1" />
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
        </svg>
        Browse Products
      </button>
      <button class="btn-nav-link" @click="goToConsultations">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
        My Consultations
      </button>
    </div>
  </aside>
</template>


