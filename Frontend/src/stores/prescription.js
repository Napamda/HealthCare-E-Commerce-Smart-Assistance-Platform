import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPatientPrescriptions, getPrescription } from '../services/prescription.js'

export const usePrescriptionStore = defineStore('prescription', () => {
  const prescriptions = ref([])
  const currentPrescription = ref(null)
  const loading = ref(false)
  const error = ref(null)

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

  function clearError() {
    error.value = null
  }

  function clearCurrentPrescription() {
    currentPrescription.value = null
  }

  return {
    prescriptions,
    currentPrescription,
    loading,
    error,
    fetchPatientPrescriptions,
    fetchPrescription,
    clearError,
    clearCurrentPrescription,
  }
})
