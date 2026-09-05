import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  escalateToDoctor,
  getConsultation,
  getPatientConsultations,
  updateConsultationStatus,
  getDoctorQueue,
  getDoctorUpcoming,
  updateConsultationPriority,
} from '../services/consultation.js'

export const useConsultationStore = defineStore('consultation', () => {
  const consultations = ref([])
  const doctorQueue = ref([])
  const upcomingConsultations = ref([])
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

  async function fetchDoctorQueue() {
    try {
      const data = await getDoctorQueue()
      doctorQueue.value = data
    } catch (err) {
      escalationError.value = err.response?.data?.error || err.message
    }
  }

  async function fetchUpcoming() {
    try {
      const data = await getDoctorUpcoming()
      upcomingConsultations.value = data
    } catch (err) {
      escalationError.value = err.response?.data?.error || err.message
    }
  }

  async function updateStatus(id, status, { rejectionReason, scheduledAt } = {}) {
    try {
      const updated = await updateConsultationStatus(id, status, { rejectionReason, scheduledAt })
      const idx = doctorQueue.value.findIndex((c) => c.id === id)
      if (idx !== -1) {
        if (status === 'REJECTED' || status === 'CLOSED') {
          doctorQueue.value.splice(idx, 1)
        } else {
          doctorQueue.value[idx] = updated
        }
      }
      const upcomingIdx = upcomingConsultations.value.findIndex((c) => c.id === id)
      if (upcomingIdx !== -1) {
        if (status === 'REJECTED' || status === 'CLOSED') {
          upcomingConsultations.value.splice(upcomingIdx, 1)
        } else {
          upcomingConsultations.value[upcomingIdx] = updated
        }
      }
      const patientIdx = consultations.value.findIndex((c) => c.id === id)
      if (patientIdx !== -1) {
        consultations.value[patientIdx] = updated
      }
      return updated
    } catch (err) {
      escalationError.value = err.response?.data?.error || err.message
      throw err
    }
  }

  async function updatePriority(id, priority) {
    try {
      const updated = await updateConsultationPriority(id, priority)
      const qIdx = doctorQueue.value.findIndex((c) => c.id === id)
      if (qIdx !== -1) doctorQueue.value[qIdx] = updated
      const pIdx = consultations.value.findIndex((c) => c.id === id)
      if (pIdx !== -1) consultations.value[pIdx] = updated
      return updated
    } catch (err) {
      escalationError.value = err.response?.data?.error || err.message
      throw err
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
    doctorQueue,
    upcomingConsultations,
    activeConsultation,
    isEscalating,
    escalationError,
    escalateFromChat,
    fetchPatientConsultations,
    fetchConsultation,
    fetchDoctorQueue,
    fetchUpcoming,
    updateStatus,
    updatePriority,
    clearEscalationError,
    resetActiveConsultation,
  }
})
