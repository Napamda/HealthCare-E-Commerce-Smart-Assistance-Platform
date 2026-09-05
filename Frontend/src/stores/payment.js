import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePaymentStore = defineStore('payment', () => {
  const methods = ref([])
  const currentPayment = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchPaymentMethods() {
    loading.value = true
    error.value = null
    try {
      const response = await fetch('/api/payments/methods')
      if (!response.ok) throw new Error('Failed to fetch payment methods')
      methods.value = await response.json()
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
      const response = await fetch('/api/payments/initiate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ orderId, method })
      })
      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'Failed to initiate payment')
      }
      currentPayment.value = await response.json()
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
      const response = await fetch(`/api/payments/${paymentId}/execute/card`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(cardDetails)
      })
      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'Card payment failed')
      }
      return await response.json()
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
      const response = await fetch(`/api/payments/${paymentId}/execute/paypal`, {
        method: 'POST'
      })
      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'PayPal payment failed')
      }
      return await response.json()
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