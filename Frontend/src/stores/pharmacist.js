import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getPendingPrescriptions,
  searchPrescriptions,
  getPrescription,
  reviewPrescription,
} from '../services/prescription.js'

export const usePharmacistStore = defineStore('pharmacist', () => {
  const prescriptions = ref([])
  const currentPrescription = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const activeFilter = ref('PENDING_REVIEW')
  const submitting = ref(false)
  const reviewSuccess = ref(false)
  const reviewMessage = ref('')

  async function fetchPendingPrescriptions() {
    loading.value = true
    error.value = null
    try {
      prescriptions.value = await getPendingPrescriptions()
      activeFilter.value = 'PENDING_REVIEW'
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to load prescriptions'
    } finally {
      loading.value = false
    }
  }

  async function fetchSearchPrescriptions(params = {}) {
    loading.value = true
    error.value = null
    try {
      prescriptions.value = await searchPrescriptions(params)
      activeFilter.value = params.status || 'ALL'
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to search prescriptions'
    } finally {
      loading.value = false
    }
  }

  async function fetchPrescription(id) {
    loading.value = true
    error.value = null
    try {
      currentPrescription.value = await getPrescription(id)
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to load prescription'
    } finally {
      loading.value = false
    }
  }

  async function submitReview(id, status, pharmacistId, comments = '') {
    submitting.value = true
    error.value = null
    reviewSuccess.value = false
    reviewMessage.value = ''
    try {
      const result = await reviewPrescription(id, {
        status,
        pharmacistId,
        pharmacistComments: comments,
      })
      currentPrescription.value = result
      reviewSuccess.value = true
      reviewMessage.value = status === 'APPROVED'
        ? 'Prescription approved successfully. Patient has been notified.'
        : 'Prescription rejected. Patient has been notified.'
      const index = prescriptions.value.findIndex((p) => p.id === id)
      if (index !== -1) {
        prescriptions.value[index] = result
      }
      return result
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to submit review'
      throw e
    } finally {
      submitting.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentPrescription() {
    currentPrescription.value = null
    reviewSuccess.value = false
    reviewMessage.value = ''
  }

  function resetReviewState() {
    reviewSuccess.value = false
    reviewMessage.value = ''
    error.value = null
  }

  return {
    prescriptions,
    currentPrescription,
    loading,
    error,
    activeFilter,
    submitting,
    reviewSuccess,
    reviewMessage,
    fetchPendingPrescriptions,
    fetchSearchPrescriptions,
    fetchPrescription,
    submitReview,
    clearError,
    clearCurrentPrescription,
    resetReviewState,
  }
})
