<script setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useConsultationStore } from '../../stores/consultation.js'

const props = defineProps({
  conversationId: { type: Number, required: true },
  patientUserId: { type: Number, default: 1 },
})

const emit = defineEmits(['close', 'escalated'])

const consultationStore = useConsultationStore()
const { isEscalating, escalationError } = storeToRefs(consultationStore)

const reason = ref('')
const priority = ref('NORMAL')
const validationError = ref('')

const priorityOptions = [
  { value: 'LOW', label: 'Low', description: 'Non-urgent, can wait' },
  { value: 'NORMAL', label: 'Normal', description: 'Standard priority' },
  { value: 'HIGH', label: 'High', description: 'Needs prompt attention' },
  { value: 'URGENT', label: 'Urgent', description: 'Requires immediate attention' },
]

async function submitEscalation() {
  validationError.value = ''

  if (!reason.value.trim()) {
    validationError.value = 'Please provide a reason for escalation.'
    return
  }

  try {
    await consultationStore.escalateFromChat({
      conversationId: props.conversationId,
      patientUserId: props.patientUserId,
      reason: reason.value.trim(),
      priority: priority.value,
    })
    emit('escalated', consultationStore.activeConsultation)
  } catch {
  }
}

function cancel() {
  emit('close')
}
</script>

<template>
  <div class="dialog-overlay" @click.self="cancel">
    <div class="dialog-card">
      <div class="dialog-header">
        <div class="dialog-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
        </div>
        <div>
          <h3 class="dialog-title">Escalate to a Doctor</h3>
          <p class="dialog-subtitle">
            Your chat context will be transferred to a healthcare professional for review.
          </p>
        </div>
        <button class="btn-close" @click="cancel" title="Close">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div class="dialog-body">
        <!-- Validation error -->
        <div v-if="validationError" class="field-error">
          {{ validationError }}
        </div>

        <!-- API error -->
        <div v-if="escalationError" class="api-error">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span>{{ escalationError }}</span>
        </div>

        <!-- Reason field -->
        <label class="field-label" for="escalate-reason">
          Reason for escalation <span class="required">*</span>
        </label>
        <textarea
          id="escalate-reason"
          v-model="reason"
          class="field-textarea"
          placeholder="Describe why you need a doctor's review (e.g., severe symptoms, need prescription, complex condition...)"
          rows="4"
          :disabled="isEscalating"
        />
        <span class="char-count">{{ reason.length }}/500</span>

        <!-- Priority selection -->
        <label class="field-label">Priority</label>
        <div class="priority-grid">
          <button
            v-for="opt in priorityOptions"
            :key="opt.value"
            type="button"
            class="priority-option"
            :class="{ active: priority === opt.value }"
            :disabled="isEscalating"
            @click="priority = opt.value"
          >
            <span class="priority-label">{{ opt.label }}</span>
            <span class="priority-desc">{{ opt.description }}</span>
          </button>
        </div>
      </div>

      <div class="dialog-footer">
        <button class="btn-cancel" :disabled="isEscalating" @click="cancel">
          Cancel
        </button>
        <button
          class="btn-escalate"
          :disabled="isEscalating"
          @click="submitEscalation"
        >
          <span v-if="isEscalating" class="spinner" />
          <span v-else>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <polyline points="16 11 19 14 23 10" />
            </svg>
            Escalate to Doctor
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.15s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.dialog-card {
  width: 100%;
  max-width: 480px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: slideUp 0.2s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.dialog-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 24px 24px 0;
}

.dialog-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: #fef3c7;
  color: #d97706;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dialog-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.dialog-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.btn-close {
  margin-left: auto;
  padding: 6px;
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  transition: background 0.15s, color 0.15s;
}
.btn-close:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

.dialog-body {
  padding: 20px 24px;
}

.field-error {
  background: #fef2f2;
  color: var(--color-danger);
  border: 1px solid #fecaca;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  margin-bottom: 12px;
}

.api-error {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fef2f2;
  color: var(--color-danger);
  border: 1px solid #fecaca;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  margin-bottom: 12px;
}

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
  margin-top: 12px;
}
.field-label:first-of-type {
  margin-top: 0;
}

.required {
  color: var(--color-danger);
}

.field-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
  min-height: 90px;
  transition: border-color 0.15s, box-shadow 0.15s;
  background: var(--color-bg);
}
.field-textarea:focus {
  outline: none;
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
.field-textarea::placeholder {
  color: var(--color-text-muted);
}
.field-textarea:disabled {
  opacity: 0.6;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.priority-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 8px;
}

.priority-option {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  text-align: left;
  transition: border-color 0.15s, background 0.15s;
}
.priority-option:hover:not(:disabled) {
  border-color: var(--color-primary-light);
  background: var(--color-primary-bg);
}
.priority-option.active {
  border-color: var(--color-primary);
  background: var(--color-primary-bg);
  box-shadow: 0 0 0 1px var(--color-primary);
}
.priority-option:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.priority-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.priority-desc {
  font-size: 11px;
  color: var(--color-text-muted);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg);
}

.btn-cancel {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  transition: background 0.15s;
}
.btn-cancel:hover:not(:disabled) {
  background: var(--color-surface);
}
.btn-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-escalate {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  background: #d97706;
  color: #fff;
  transition: background 0.15s;
}
.btn-escalate:hover:not(:disabled) {
  background: #b45309;
}
.btn-escalate:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
