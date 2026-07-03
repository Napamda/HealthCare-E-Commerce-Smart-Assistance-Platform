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

<style scoped>
.chat-input-bar {
  padding: 16px 24px;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
  transition: border-color 0.15s, box-shadow 0.15s;
}
.input-wrapper:focus-within {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.input-field {
  flex: 1;
  resize: none;
  border: none;
  outline: none;
  background: transparent;
  padding: 4px 0;
  font-size: 14px;
  line-height: 1.5;
  max-height: 160px;
}
.input-field::placeholder {
  color: var(--color-text-muted);
}

.btn-send {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: #fff;
  transition: background 0.15s, opacity 0.15s;
}
.btn-send:hover:not(:disabled) {
  background: var(--color-primary-dark);
}
.btn-send:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-hint {
  margin-top: 8px;
  font-size: 11px;
  color: var(--color-text-muted);
  text-align: center;
}
kbd {
  display: inline-block;
  padding: 1px 5px;
  font-size: 10px;
  font-family: inherit;
  border-radius: 3px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  line-height: 1.4;
}
</style>
