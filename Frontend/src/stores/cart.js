import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getCart, getCartCount, addToCart, updateCartQuantity,
  removeFromCart, clearCart,
} from '../services/cart.js'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const total = ref(0)
  const itemCount = ref(0)
  const loading = ref(false)
  const error = ref(null)

  const isEmpty = computed(() => items.value.length === 0)

  function formatPrice(price) {
    if (price == null) return '$0.00'
    return '$' + Number(price).toFixed(2)
  }

  async function fetchCart() {
    loading.value = true
    error.value = null
    try {
      const data = await getCart()
      items.value = data.items || []
      total.value = data.total || 0
      itemCount.value = data.itemCount || 0
    } catch (e) {
      error.value = 'Failed to load cart'
    } finally {
      loading.value = false
    }
  }

  async function fetchCount() {
    try {
      const data = await getCartCount()
      itemCount.value = data.count || 0
    } catch (_) { /* silent */ }
  }

  async function addItem(product) {
    try {
      await addToCart({
        productId: product.id,
        productName: product.name,
        productImage: product.imageUrl || null,
        quantity: product.quantity || 1,
        unitPrice: product.price,
      })
      await fetchCart()
      return true
    } catch (e) {
      error.value = 'Failed to add item'
      return false
    }
  }

  async function updateItem(cartItemId, quantity) {
    try {
      await updateCartQuantity(cartItemId, quantity)
      await fetchCart()
    } catch (e) {
      error.value = 'Failed to update quantity'
    }
  }

  async function removeItem(cartItemId) {
    try {
      await removeFromCart(cartItemId)
      items.value = items.value.filter((i) => i.id !== cartItemId)
      await fetchCart()
    } catch (e) {
      error.value = 'Failed to remove item'
    }
  }

  async function clearAll() {
    try {
      await clearCart()
      items.value = []
      total.value = 0
      itemCount.value = 0
    } catch (e) {
      error.value = 'Failed to clear cart'
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    items, total, itemCount, loading, error, isEmpty,
    formatPrice, fetchCart, fetchCount, addItem, updateItem,
    removeItem, clearAll, clearError,
  }
})
