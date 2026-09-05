/**
 * Guest cart — localStorage-based cart for non-authenticated users.
 * Mirrors the server CartResponse shape for seamless transition.
 */

const STORAGE_KEY = 'guest_cart_items'

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function save(items) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
}

/** Get all guest cart items */
export function getGuestCart() {
  return load()
}

/** Get total item count (sum of quantities) */
export function getGuestCartCount() {
  return load().reduce((sum, item) => sum + item.quantity, 0)
}

/** Get cart total */
export function getGuestCartTotal() {
  return load().reduce((sum, item) => sum + item.unitPrice * item.quantity, 0)
}

/**
 * Add a product to the guest cart or increment quantity if already present.
 * @param {{ id: number, productId: number, productName: string, productImage?: string, quantity: number, unitPrice: number }} product
 * @returns {object} the added/updated item
 */
export function addToGuestCart(product) {
  const items = load()
  const existing = items.find((i) => i.productId === product.productId)
  if (existing) {
    existing.quantity += product.quantity || 1
  } else {
    items.push({
      id: product.productId, // use productId as id so CartPage update/remove work
      productId: product.productId,
      productName: product.productName,
      productImage: product.productImage || null,
      quantity: product.quantity || 1,
      unitPrice: product.unitPrice || product.price || 0,
      subtotal: (product.unitPrice || product.price || 0) * (product.quantity || 1),
    })
  }
  save(items)
  return items.find((i) => i.productId === product.productId)
}

/**
 * Update quantity of a guest cart item.
 * @param {number} productId
 * @param {number} quantity — new absolute quantity (must be >= 1)
 */
export function updateGuestCartQuantity(productId, quantity) {
  if (quantity < 1) {
    removeFromGuestCart(productId)
    return
  }
  const items = load()
  const item = items.find((i) => i.productId === productId)
  if (item) {
    item.quantity = quantity
    item.subtotal = item.unitPrice * quantity
    save(items)
  }
}

/**
 * Remove an item from the guest cart.
 * @param {number} productId
 */
export function removeFromGuestCart(productId) {
  const items = load().filter((i) => i.productId !== productId)
  save(items)
}

/** Clear the entire guest cart */
export function clearGuestCart() {
  localStorage.removeItem(STORAGE_KEY)
}

/** Check if the guest cart has any items */
export function isGuestCartEmpty() {
  return load().length === 0
}