<script setup>
import { computed } from 'vue'

const props = defineProps({
  message: {
    type: Object,
    required: true,
    // shape: { id, role: 'USER'|'ASSISTANT'|'DOCTOR', content, timestamp, provider, model }
  },
})

// Map role to a CSS layout class. USER → right side; ASSISTANT & DOCTOR → left side.
const rowClass = computed(() => (props.message.role === 'USER' ? 'user' : 'assistant'))
const avatarClass = computed(() =>
  props.message.role === 'USER'
    ? 'avatar-user'
    : props.message.role === 'DOCTOR'
      ? 'avatar-doctor'
      : 'avatar-ai',
)
const bubbleClass = computed(() =>
  props.message.role === 'USER'
    ? 'bubble-user'
    : props.message.role === 'DOCTOR'
      ? 'bubble-doctor'
      : 'bubble-ai',
)
const isDoctor = computed(() => props.message.role === 'DOCTOR')

function formatTime(isoString) {
  if (!isoString) return ''
  return new Date(isoString).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <div class="message-row" :class="rowClass">
    <!-- Avatar -->
    <div class="avatar" :class="avatarClass">
      <template v-if="message.role === 'USER'">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
      </template>
      <template v-else-if="isDoctor">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z" />
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
      <div class="bubble" :class="bubbleClass">
        <p class="bubble-text">{{ message.content }}</p>
      </div>
      <div class="bubble-meta">
        <span v-if="isDoctor" class="doctor-badge">Doctor</span>
        <span>{{ formatTime(message.timestamp) }}</span>
        <template v-if="message.role === 'ASSISTANT' && message.model">
          <span class="dot">&middot;</span>
          <span class="model-badge">{{ message.model }}</span>
        </template>
      </div>
    </div>
  </div>
</template>
