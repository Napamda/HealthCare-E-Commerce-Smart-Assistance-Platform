import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as inventoryApi from '../services/inventory.js'

export const useInventoryStore = defineStore('inventory', () => {
  // List of StockInfoResponse for every product
  const stock = ref([])
  const loading = ref(false)
  const updating = ref(false)
  const error = ref('')
  const success = ref('')

  // History log modal state
  const historyVisible = ref(false)
  const historyLoading = ref(false)
  const historyProduct = ref(null)
  const history = ref([])

  // ---- Aggregates (drives the summary cards / badges) ----
  const totalProducts = computed(() => stock.value.length)
  const totalUnits = computed(() =>
    stock.value.reduce((sum, item) => sum + (Number(item.stockQuantity) || 0), 0),
  )
  const lowStockCount = computed(
    () => stock.value.filter((item) => item.lowStock && !item.outOfStock).length,
  )
  const outOfStockCount = computed(() => stock.value.filter((item) => item.outOfStock).length)

  function clearMessages() {
    error.value = ''
    success.value = ''
  }

  async function fetchStock() {
    loading.value = true
    error.value = ''
    try {
      stock.value = await inventoryApi.getStockList()
    } catch (e) {
      error.value = extractError(e)
    } finally {
      loading.value = false
    }
  }

  /**
   * Apply a stock change.
   * @param {object} payload
   * @param {number} payload.productId
   * @param {'increment'|'decrement'|'set'} payload.mode
   * @param {number} payload.quantity
   * @param {string} [payload.note]
   * @returns {Promise<boolean>} true when the operation succeeded
   */
  async function adjust({ productId, mode, quantity, note }) {
    updating.value = true
    clearMessages()
    try {
      const updated = await callAdjust(productId, mode, quantity, note)
      const idx = stock.value.findIndex((item) => item.productId === updated.productId)
      if (idx !== -1) {
        stock.value[idx] = updated
      } else {
        stock.value.unshift(updated)
      }
      success.value = `Stock updated for "${updated.productName}"`
      return true
    } catch (e) {
      error.value = extractError(e)
      return false
    } finally {
      updating.value = false
    }
  }

  function callAdjust(productId, mode, quantity, note) {
    if (mode === 'increment') return inventoryApi.incrementStock(productId, quantity, note)
    if (mode === 'decrement') return inventoryApi.decrementStock(productId, quantity, note)
    return inventoryApi.adjustStock(productId, quantity, note)
  }

  async function openHistory(item) {
    historyProduct.value = item
    history.value = []
    historyVisible.value = true
    historyLoading.value = true
    error.value = ''
    try {
      history.value = await inventoryApi.getStockHistory(item.productId)
    } catch (e) {
      error.value = extractError(e)
    } finally {
      historyLoading.value = false
    }
  }

  function closeHistory() {
    historyVisible.value = false
    historyProduct.value = null
    history.value = []
  }

  function extractError(e) {
    return (
      e?.response?.data?.message ||
      e?.response?.data?.error ||
      e?.message ||
      'Something went wrong'
    )
  }

  return {
    stock,
    loading,
    updating,
    error,
    success,
    historyVisible,
    historyLoading,
    historyProduct,
    history,
    totalProducts,
    totalUnits,
    lowStockCount,
    outOfStockCount,
    fetchStock,
    adjust,
    openHistory,
    closeHistory,
    clearMessages,
  }
})
