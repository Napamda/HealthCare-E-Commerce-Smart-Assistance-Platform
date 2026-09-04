import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as inventoryApi from '../services/inventory.js'

function suggestedRestockQty(item) {
  const stock = Number(item.stockQuantity) || 0
  const threshold = Number(item.lowStockThreshold) || 5
  const target = Math.max(threshold * 2, threshold + 10)
  return Math.max(1, target - stock)
}

export const useInventoryStore = defineStore('inventory', () => {
  const stock = ref([])
  const reservations = ref([])
  const recentMovements = ref([])
  const loading = ref(false)
  const reservationsLoading = ref(false)
  const updating = ref(false)
  const error = ref('')
  const success = ref('')

  const historyVisible = ref(false)
  const historyLoading = ref(false)
  const historyProduct = ref(null)
  const history = ref([])

  const totalProducts = computed(() => stock.value.length)
  const totalUnits = computed(() =>
    stock.value.reduce((sum, item) => sum + (Number(item.stockQuantity) || 0), 0),
  )
  const totalAvailable = computed(() =>
    stock.value.reduce((sum, item) => sum + (Number(item.availableQuantity) || 0), 0),
  )
  const totalReservedUnits = computed(() =>
    stock.value.reduce((sum, item) => sum + (Number(item.reservedQuantity) || 0), 0),
  )
  const lowStockCount = computed(
    () => stock.value.filter((item) => item.lowStock && !item.outOfStock).length,
  )
  const outOfStockCount = computed(() => stock.value.filter((item) => item.outOfStock).length)
  const reservedCount = computed(
    () => stock.value.filter((item) => (Number(item.reservedQuantity) || 0) > 0).length,
  )
  const healthyCount = computed(
    () => stock.value.filter((item) => !item.lowStock && !item.outOfStock).length,
  )
  const stockHealthScore = computed(() => {
    if (!stock.value.length) return 100
    return Math.round((healthyCount.value / stock.value.length) * 100)
  })
  const activeReservations = computed(() =>
    reservations.value.filter((r) => String(r.status).toUpperCase() === 'ACTIVE'),
  )
  const attentionItems = computed(() =>
    stock.value
      .filter((item) => item.outOfStock || item.lowStock)
      .slice()
      .sort((a, b) => {
        if (a.outOfStock !== b.outOfStock) return a.outOfStock ? -1 : 1
        return (a.stockQuantity || 0) - (b.stockQuantity || 0)
      }),
  )
  const categoryBreakdown = computed(() => {
    const map = new Map()
    for (const item of stock.value) {
      const key = item.category || 'UNCATEGORIZED'
      if (!map.has(key)) {
        map.set(key, { category: key, products: 0, units: 0, low: 0, out: 0 })
      }
      const row = map.get(key)
      row.products += 1
      row.units += Number(item.stockQuantity) || 0
      if (item.outOfStock) row.out += 1
      else if (item.lowStock) row.low += 1
    }
    return [...map.values()].sort((a, b) => b.units - a.units)
  })

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

  async function fetchReservations() {
    reservationsLoading.value = true
    try {
      reservations.value = await inventoryApi.getReservations()
    } catch (e) {
      error.value = extractError(e)
    } finally {
      reservationsLoading.value = false
    }
  }

  async function fetchRecentActivity() {
    try {
      recentMovements.value = await inventoryApi.getRecentActivity()
    } catch {
      recentMovements.value = []
    }
  }

  async function refreshAll() {
    await Promise.all([fetchStock(), fetchReservations(), fetchRecentActivity()])
  }

  function clearMessages() {
    error.value = ''
    success.value = ''
  }

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
      fetchRecentActivity()
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

  async function quickRestock(item, quantity, note = 'Quick restock') {
    const qty = Number(quantity) || suggestedRestockQty(item)
    return adjust({
      productId: item.productId,
      mode: 'increment',
      quantity: qty,
      note,
    })
  }

  async function restockToSafeLevel(item) {
    return quickRestock(item, suggestedRestockQty(item), 'Restocked to safe level')
  }

  async function bulkRestock(items, note = 'Bulk restock') {
    if (!items?.length) return false
    updating.value = true
    clearMessages()
    let okCount = 0
    try {
      for (const item of items) {
        const qty = suggestedRestockQty(item)
        const updated = await inventoryApi.incrementStock(item.productId, qty, note)
        const idx = stock.value.findIndex((row) => row.productId === updated.productId)
        if (idx !== -1) stock.value[idx] = updated
        okCount += 1
      }
      success.value = `Restocked ${okCount} product${okCount === 1 ? '' : 's'}`
      fetchRecentActivity()
      return true
    } catch (e) {
      error.value = extractError(e)
      if (okCount > 0) {
        success.value = `Restocked ${okCount} product${okCount === 1 ? '' : 's'} before an error`
      }
      return false
    } finally {
      updating.value = false
    }
  }

  async function setThreshold({ productId, threshold }) {
    updating.value = true
    clearMessages()
    try {
      const updated = await inventoryApi.setThreshold(productId, threshold)
      const idx = stock.value.findIndex((item) => item.productId === updated.productId)
      if (idx !== -1) {
        stock.value[idx] = updated
      }
      success.value = `Low stock threshold updated for "${updated.productName}"`
      return true
    } catch (e) {
      error.value = extractError(e)
      return false
    } finally {
      updating.value = false
    }
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
    reservations,
    recentMovements,
    loading,
    reservationsLoading,
    updating,
    error,
    success,
    historyVisible,
    historyLoading,
    historyProduct,
    history,
    totalProducts,
    totalUnits,
    totalAvailable,
    totalReservedUnits,
    lowStockCount,
    outOfStockCount,
    reservedCount,
    healthyCount,
    stockHealthScore,
    activeReservations,
    attentionItems,
    categoryBreakdown,
    fetchStock,
    fetchReservations,
    fetchRecentActivity,
    refreshAll,
    adjust,
    quickRestock,
    restockToSafeLevel,
    bulkRestock,
    setThreshold,
    openHistory,
    closeHistory,
    clearMessages,
    suggestedRestockQty,
  }
})
