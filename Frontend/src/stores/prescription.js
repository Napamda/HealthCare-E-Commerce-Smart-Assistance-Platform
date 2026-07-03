import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getPatientPrescriptions,
  getPrescription,
  updateOcrText as updateOcrApi,
} from '../services/prescription.js'

export const usePrescriptionStore = defineStore('prescription', () => {
  const prescriptions = ref([])
  const currentPrescription = ref(null)
  const loading = ref(false)
  const saving = ref(false)
  const error = ref(null)
  const saveSuccess = ref(false)

  async function fetchPatientPrescriptions(patientUserId) {
    loading.value = true
    error.value = null
    try {
      prescriptions.value = await getPatientPrescriptions(patientUserId)
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to load prescriptions'
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
    error,
    saveSuccess,
    fetchPatientPrescriptions,
    fetchPrescription,
    updateOcrText,
    clearError,
    clearSaveSuccess,
    clearCurrentPrescription,
  }
})
