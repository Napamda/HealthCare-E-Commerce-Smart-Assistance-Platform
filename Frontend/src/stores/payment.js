import { defineStore } from 'pinia'
import { ref } from 'vue'
import apiClient from '../services/api.js'

export const usePaymentStore = defineStore('payment', () => {
  const methods = ref([])
  const currentPayment = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchPaymentMethods() {
    loading.value = true
    error.value = null
    try {
      const response = await apiClient.get('/api/payments/methods')
      methods.value = response.data
    } catch (err) {
      error.value = err.message
      console.error('Failed to fetch payment methods:', err)
    } finally {
      loading.value = false
    }
  }

  async function initiatePayment(orderId, method) {
    loading.value = true
    error.value = null
    try {
      const response = await apiClient.post('/api/payments/initiate', { orderId, method })
      currentPayment.value = response.data
      return currentPayment.value
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function executeCardPayment(paymentId, cardDetails) {
    loading.value = true
    error.value = null
    try {
      const response = await apiClient.post(`/api/payments/${paymentId}/execute/card`, cardDetails)
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  async function executePayPalPayment(paymentId) {
    loading.value = true
    error.value = null
    try {
      const response = await apiClient.post(`/api/payments/${paymentId}/execute/paypal`)
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  function formatPrice(price) {
    if (price == null) return '$0.00'
    return '$' + Number(price).toFixed(2)
  }

  return {
    methods,
    currentPayment,
    loading,
    error,
    fetchPaymentMethods,
    initiatePayment,
    executeCardPayment,
    executePayPalPayment,
    formatPrice
  }
})
