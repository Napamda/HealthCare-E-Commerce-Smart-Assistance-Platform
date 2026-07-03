<script setup>
defineProps({
  message: {
    type: Object,
    required: true,
    // shape: { id, role: 'USER'|'ASSISTANT', content, timestamp, provider, model }
  },
})
</script>

<template>
  <div class="message-row" :class="message.role === 'USER' ? 'user' : 'assistant'">
    <!-- Avatar -->
    <div class="avatar" :class="message.role === 'USER' ? 'avatar-user' : 'avatar-ai'">
      <template v-if="message.role === 'USER'">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
      </template>
      <template v-else>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="4" />
          <line x1="9" y1="9" x2="15" y2="9" />
          <line x1="9" y1="13" x2="13" y2="13" />
        </svg>
      </template>
    </div>

    <!-- Bubble -->
    <div class="bubble-wrapper">
      <div class="bubble" :class="message.role === 'USER' ? 'bubble-user' : 'bubble-ai'">
        <p class="bubble-text">{{ message.content }}</p>
      </div>
      <div class="bubble-meta">
        <span>{{ formatTime(message.timestamp) }}</span>
        <template v-if="message.role === 'ASSISTANT' && message.model">
          <span class="dot">&middot;</span>
          <span class="model-badge">{{ message.model }}</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  methods: {
    formatTime(isoString) {
      if (!isoString) return ''
      return new Date(isoString).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
      })
    },
  },
}
</script>

<style scoped>
.message-row {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  max-width: 90%;
}
.message-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.message-row.assistant {
  align-self: flex-start;
}

/* Avatar */
.avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.avatar-user {
  background: var(--color-primary);
  color: #fff;
}
.avatar-ai {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

/* Bubble */
.bubble-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.bubble {
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  line-height: 1.55;
  font-size: 14px;
}

.bubble-user {
  background: var(--color-user-bubble);
  color: #fff;
  border-bottom-right-radius: var(--radius-sm);
}

.bubble-ai {
  background: var(--color-ai-bubble);
  color: var(--color-text);
  border-bottom-left-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
}

.bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--color-text-muted);
}

.message-row.user .bubble-meta {
  justify-content: flex-end;
}

.model-badge {
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--color-border);
  font-size: 10px;
  font-weight: 500;
}
</style>
