import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  escalateToDoctor,
  getConsultation,
  getPatientConsultations,
  updateConsultationStatus,
} from '../services/consultation.js'

export const useConsultationStore = defineStore('consultation', () => {
  const consultations = ref([])
  const activeConsultation = ref(null)
  const isEscalating = ref(false)
  const escalationError = ref(null)

  async function escalateFromChat({ conversationId, reason, priority }) {
    isEscalating.value = true
    escalationError.value = null

    try {
      const result = await escalateToDoctor({
        conversationId,
        reason,
        priority,
      })
      activeConsultation.value = result
      consultations.value.unshift(result)
      return result
    } catch (err) {
      escalationError.value = err.response?.data?.error || err.message
      throw err
    } finally {
      isEscalating.value = false
    }
  }

  async function fetchPatientConsultations(patientUserId) {
    try {
      const data = await getPatientConsultations(patientUserId)
      consultations.value = data
    } catch (err) {
      escalationError.value = err.response?.data?.error || err.message
    }
  }

  async function fetchConsultation(id) {
    try {
      const data = await getConsultation(id)
      activeConsultation.value = data
    } catch (err) {
      escalationError.value = err.response?.data?.error || err.message
    }
  }

  function clearEscalationError() {
    escalationError.value = null
  }

  function resetActiveConsultation() {
    activeConsultation.value = null
  }

  return {
    consultations,
    activeConsultation,
    isEscalating,
    escalationError,
    escalateFromChat,
    fetchPatientConsultations,
    fetchConsultation,
    clearEscalationError,
    resetActiveConsultation,
  }
})
