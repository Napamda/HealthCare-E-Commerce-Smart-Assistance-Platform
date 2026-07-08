<script setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useConsultationStore } from '../../stores/consultation.js'

const props = defineProps({
  conversationId: { type: Number, required: true },
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


