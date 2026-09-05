import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getCart, getCartCount, addToCart, updateCartQuantity,
  removeFromCart, clearCart, mergeGuestCart,
} from '../services/cart.js'
import {
  getGuestCart, getGuestCartCount, getGuestCartTotal,
  addToGuestCart, updateGuestCartQuantity,
  removeFromGuestCart, clearGuestCart, isGuestCartEmpty,
} from '../services/guestCart.js'
import { useAuthStore } from './auth.js'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const total = ref(0)
  const itemCount = ref(0)
  const loading = ref(false)
  const error = ref(null)

  const isEmpty = computed(() => items.value.length === 0)

  // ---- helpers ----

  function isGuest() {
    const auth = useAuthStore()
    return !auth.isAuthenticated
  }

  function formatPrice(price) {
    if (price == null) return '$0.00'
    return '$' + Number(price).toFixed(2)
  }

  function loadGuestState() {
    const guestItems = getGuestCart()
    items.value = guestItems
    itemCount.value = getGuestCartCount()
    total.value = getGuestCartTotal()
  }

  function loadServerState(data) {
    items.value = data.items || []
    total.value = data.total || 0
    itemCount.value = data.itemCount || 0
  }

  // ---- public actions ----

  /** Fetch cart — auto-detects guest vs authenticated */
  async function fetchCart() {
    if (isGuest()) {
      loadGuestState()
      return
    }
    loading.value = true
    error.value = null
    try {
      const data = await getCart()
      loadServerState(data)
    } catch (e) {
      error.value = 'Failed to load cart'
    } finally {
      loading.value = false
    }
  }

  /** Fetch count only (for navbar badge) */
  async function fetchCount() {
    if (isGuest()) {
      itemCount.value = getGuestCartCount()
      return
    }
    try {
      const data = await getCartCount()
      itemCount.value = data.count || 0
    } catch (_) { /* silent */ }
  }

  /**
   * Add item to cart.
   * @param {{ id: number, productId?: number, name?: string, productName?: string, imageUrl?: string, productImage?: string, quantity?: number, price?: number, unitPrice?: number }} product
   */
  async function addItem(product) {
    if (isGuest()) {
      addToGuestCart({
        productId: product.productId || product.id,
        productName: product.productName || product.name,
        productImage: product.productImage || product.imageUrl || null,
        quantity: product.quantity || 1,
        unitPrice: product.unitPrice || product.price || 0,
      })
      loadGuestState()
      return true
    }
    try {
      await addToCart({
        productId: product.productId || product.id,
        productName: product.productName || product.name,
        productImage: product.productImage || product.imageUrl || null,
        quantity: product.quantity || 1,
        unitPrice: product.unitPrice || product.price || 0,
      })
      await fetchCart()
      return true
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to add item'
      return false
    }
  }

  /**
   * Update quantity of a cart item.
   * @param {number} cartItemId — server cart item id (logged in) or productId (guest)
   * @param {number} quantity
   */
  async function updateItem(cartItemId, quantity) {
    if (isGuest()) {
      updateGuestCartQuantity(cartItemId, quantity)
      loadGuestState()
      return
    }
    try {
      await updateCartQuantity(cartItemId, quantity)
      await fetchCart()
    } catch (e) {
      error.value = e.response?.data?.error || 'Failed to update quantity'
    }
  }

  /**
   * Remove item from cart.
   * @param {number} cartItemId — server cart item id (logged in) or productId (guest)
   */
  async function removeItem(cartItemId) {
    if (isGuest()) {
      removeFromGuestCart(cartItemId)
      loadGuestState()
      return
    }
    try {
      await removeFromCart(cartItemId)
      items.value = items.value.filter((i) => i.id !== cartItemId)
      await fetchCart()
    } catch (e) {
      error.value = 'Failed to remove item'
    }
  }

  /** Clear entire cart */
  async function clearAll() {
    if (isGuest()) {
      clearGuestCart()
      items.value = []
      total.value = 0
      itemCount.value = 0
      return
    }
    try {
      await clearCart()
      items.value = []
      total.value = 0
      itemCount.value = 0
    } catch (e) {
      error.value = 'Failed to clear cart'
    }
  }

  /**
   * Merge guest cart into server cart after login.
   * Should be called after a successful login or session restore.
   */
  async function mergeAfterLogin() {
    if (isGuestCartEmpty()) return
    const guestItems = getGuestCart()
    const mergePayload = guestItems.map((i) => ({
      productId: i.productId,
      quantity: i.quantity,
    }))
    try {
      const data = await mergeGuestCart(mergePayload)
      clearGuestCart()
      loadServerState(data)
    } catch (e) {
      // If merge fails, keep guest cart — user can retry
      error.value = 'Failed to merge cart — your items are saved locally'
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    items, total, itemCount, loading, error, isEmpty,
    formatPrice,
    fetchCart, fetchCount, addItem, updateItem, removeItem, clearAll,
    mergeAfterLogin, clearError,
  }
})