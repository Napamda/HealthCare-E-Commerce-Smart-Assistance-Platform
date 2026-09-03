import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getMyPrescriptions,
  getPrescription,
  updateOcrText as updateOcrApi,
  orderPrescription as orderPrescriptionApi,
} from '../services/prescription.js'

export const usePrescriptionStore = defineStore('prescription', () => {
  const prescriptions = ref([])
  const currentPrescription = ref(null)
  const loading = ref(false)
  const saving = ref(false)
  const ordering = ref(false)
  const orderSuccess = ref(false)
  const error = ref(null)
  const saveSuccess = ref(false)

  // Uses the authenticated user's session on the backend; no id needs to be
  // supplied by the client, so we can't accidentally attach to the wrong patient.
  async function fetchMyPrescriptions() {
    loading.value = true
    error.value = null
    try {
      prescriptions.value = await getMyPrescriptions()
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to load prescriptions'
    } finally {
      loading.value = false
    }
  }

  // Kept for backward compatibility; prefer fetchMyPrescriptions for patient UI.
  async function fetchPatientPrescriptions() {
    return fetchMyPrescriptions()
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

  async function updateOcrText(id, ocrText) {
    saving.value = true
    error.value = null
    saveSuccess.value = false
    try {
      const updated = await updateOcrApi(id, ocrText)
      if (currentPrescription.value && currentPrescription.value.id === id) {
        currentPrescription.value = updated
      }
      const idx = prescriptions.value.findIndex((p) => p.id === id)
      if (idx !== -1) {
        prescriptions.value[idx] = updated
      }
      saveSuccess.value = true
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to save OCR text'
    } finally {
      saving.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  // Patient adds all pharmacist-selected medications to their own cart.
  async function orderMedications(id) {
    ordering.value = true
    error.value = null
    orderSuccess.value = false
    try {
      const updated = await orderPrescriptionApi(id)
      if (currentPrescription.value && currentPrescription.value.id === id) {
        currentPrescription.value = updated
      }
      orderSuccess.value = true
      return true
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to add medications to cart'
      return false
    } finally {
      ordering.value = false
    }
  }

  function clearOrderSuccess() {
    orderSuccess.value = false
  }

  function clearSaveSuccess() {
    saveSuccess.value = false
  }

  function clearCurrentPrescription() {
    currentPrescription.value = null
  }

  return {
    prescriptions,
    currentPrescription,
    loading,
    saving,
    ordering,
    orderSuccess,
    error,
    saveSuccess,
    fetchMyPrescriptions,
    fetchPatientPrescriptions,
    fetchPrescription,
    updateOcrText,
    orderMedications,
    clearOrderSuccess,
    clearError,
    clearSaveSuccess,
    clearCurrentPrescription,
  }
})
