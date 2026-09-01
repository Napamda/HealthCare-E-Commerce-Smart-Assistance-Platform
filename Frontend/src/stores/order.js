import { defineStore } from 'pinia'
import { ref } from 'vue'
import { createOrder, getUserOrders, getOrderById, cancelOrder } from '../services/order.js'

export const useOrderStore = defineStore('order', () => {
  const orders = ref([])
  const selectedOrder = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const pagination = ref({ page: 0, totalPages: 0, totalElements: 0 })

  async function placeOrder(orderData) {
    loading.value = true
    error.value = null
    try {
      const data = await createOrder(orderData)
      return data
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to place order'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchOrders(page = 0, size = 10) {
    loading.value = true
    error.value = null
    try {
      const data = await getUserOrders(page, size)
      orders.value = data.content || []
      pagination.value = {
        page: data.page || 0,
        totalPages: data.totalPages || 0,
        totalElements: data.totalElements || 0,
      }
    } catch (e) {
      error.value = 'Failed to load orders'
    } finally {
      loading.value = false
    }
  }

  async function fetchOrderById(orderId) {
    loading.value = true
    error.value = null
    try {
      selectedOrder.value = await getOrderById(orderId)
    } catch (e) {
      error.value = 'Failed to load order'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function cancelExistingOrder(orderId) {
    try {
      const data = await cancelOrder(orderId)
      const idx = orders.value.findIndex((o) => o.id === orderId)
      if (idx !== -1) orders.value[idx] = data
      if (selectedOrder.value?.id === orderId) selectedOrder.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to cancel order'
      throw e
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    orders, selectedOrder, loading, error, pagination,
    placeOrder, fetchOrders, fetchOrderById, cancelExistingOrder, clearError,
  }
})
