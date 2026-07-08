<script setup>
import { ref, nextTick } from 'vue'

const emit = defineEmits(['send'])

const inputText = ref('')
const textareaRef = ref(null)

function onSubmit() {
  const text = inputText.value.trim()
  if (!text) return
  emit('send', text)
  inputText.value = ''
  nextTick(() => textareaRef.value?.focus())
}

function onKeydown(event) {
  // Enter sends; Shift+Enter inserts newline
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    onSubmit()
  }
}

// Auto-resize textarea
function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}

defineExpose({ focus: () => textareaRef.value?.focus() })
</script>

<template>
  <form class="chat-input-bar" @submit.prevent="onSubmit">
    <div class="input-wrapper">
      <textarea
        ref="textareaRef"
        v-model="inputText"
        class="input-field"
        placeholder="Describe your symptoms or ask a health question..."
        rows="1"
        @keydown="onKeydown"
        @input="autoResize"
      />
      <button
        type="submit"
        class="btn-send"
        :disabled="!inputText.trim()"
        title="Send message"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13" />
          <polygon points="22 2 15 22 11 13 2 9 22 2" />
        </svg>
      </button>
    </div>
    <p class="input-hint">
      Press <kbd>Enter</kbd> to send, <kbd>Shift+Enter</kbd> for new line.
      AI responses are for informational purposes only.
    </p>
  </form>
</template>


