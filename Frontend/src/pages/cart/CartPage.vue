<script setup>
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart.js'
import { useAuthStore } from '../../stores/auth.js'
import { useCategoryStore } from '../../stores/category.js'

const router = useRouter()
const store = useCartStore()
const authStore = useAuthStore()
const categoryStore = useCategoryStore()

const isEmpty = computed(() => store.isEmpty)
const totalFormatted = computed(() => store.formatPrice(store.total))
const cartItems = computed(() => store.items)
const isGuest = computed(() => !authStore.isAuthenticated)

const suggestedCategories = computed(() => {
  const roots = categoryStore.roots || []
  return roots.slice(0, 4).map(c => ({
    name: c.name,
    icon: CATEGORY_ICONS[c.name] || HEALTH,
  }))
})

const CATEGORY_ICONS = {
  'Vitamins & Supplements': VITAMINS,
  'Personal Care': PERSONAL,
  'Medications': MEDS,
  'First Aid': AID,
  'Health Devices': DEVICES,
}

const HEALTH = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>`
const VITAMINS = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>`
const PERSONAL = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`
const MEDS = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M16 4v2a2 2 0 0 1-2 2H10a2 2 0 0 1-2-2V4"/><path d="M4 10h16"/><path d="M8 14h8"/><path d="M8 18h8"/></svg>`
const AID = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>`
const DEVICES = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="14" y2="14"/></svg>`

function goToProducts() {
  router.push('/products')
}

function goToCategory(name) {
  router.push({ name: 'Products', query: { category: name } })
}

function goToCheckout() {
  if (isGuest.value) {
    router.push({ name: 'Login', query: { redirect: '/checkout' } })
    return
  }
  router.push('/checkout')
}

function clearCart() {
  if (confirm('Are you sure you want to clear your cart?')) {
    store.items.forEach(item => store.removeItem(item.id))
  }
}

onMounted(() => {
  store.fetchCart()
  categoryStore.fetchTree()
})
</script>

<template>
  <div class="cart-page">
    <div class="cart-header">
      <div class="header-content">
        <h1>Shopping Cart</h1>
        <p v-if="!isEmpty" class="cart-subtitle">{{ cartItems.length }} item{{ cartItems.length !== 1 ? 's' : '' }} in your cart</p>
      </div>
      <div v-if="!isEmpty" class="cart-actions">
        <button class="btn-clear" @click="clearCart" title="Clear cart">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          </svg>
          Clear Cart
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="cart-loading">
      <div class="loading-spinner">
        <svg class="spinner" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
      </div>
      <p>Loading your cart...</p>
    </div>

    <!-- Empty cart -->
    <div v-else-if="isEmpty" class="cart-empty">
      <div class="empty-cart-illustration">
        <svg width="120" height="120" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="1" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1" />
          <circle cx="20" cy="21" r="1" />
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
        </svg>
      </div>
      <h2>Your cart is empty</h2>
      <p>Looks like you haven't added any healthcare products yet.</p>
      <div class="empty-suggestions">
        <div class="suggestion-item">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.42 4.58a5.4 5.4 0 0 0-7.65 0l-.77.78-.77-.78a5.4 5.4 0 0 0-7.65 0C1.46 6.7 1.33 10.28 4 13l8 8 8-8c2.67-2.72 2.54-6.3.42-8.42z" />
          </svg>
          <span>Browse our catalog</span>
        </div>
        <div class="suggestion-item">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76" />
          </svg>
          <span>Explore categories</span>
        </div>
      </div>
      <button class="btn-shop" @click="goToProducts">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1" />
          <circle cx="20" cy="21" r="1" />
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
        </svg>
        Start Shopping
      </button>
    </div>

    <!-- Cart with items -->
    <div v-else class="cart-content">
      <!-- Guest banner -->
      <div v-if="isGuest" class="guest-banner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
          <line x1="12" y1="9" x2="12" y2="13"/>
          <line x1="12" y1="17" x2="12.01" y2="17"/>
        </svg>
        <span>Your cart is saved locally. <router-link to="/login" class="guest-login-link">Log in</router-link> to sync it across devices.</span>
      </div>
      <div class="cart-items">
        <div v-for="item in cartItems" :key="item.id" class="cart-item">
          <div class="item-image">
            <img v-if="item.productImage" :src="item.productImage" :alt="item.productName" />
            <div v-else class="item-image-placeholder">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
            </div>
          </div>

          <div class="item-details">
            <h3 class="item-name">{{ item.productName }}</h3>
            <span class="item-price">{{ store.formatPrice(item.unitPrice) }} each</span>
            <span class="item-stock">In Stock</span>
          </div>

          <div class="item-actions">
            <div class="item-qty">
              <button
                class="qty-btn"
                :disabled="item.quantity <= 1"
                @click="store.updateItem(item.id, item.quantity - 1)"
              >−</button>
              <span class="qty-value">{{ item.quantity }}</span>
              <button
                class="qty-btn"
                @click="store.updateItem(item.id, item.quantity + 1)"
              >+</button>
            </div>
            <div class="item-subtotal">
              <span class="subtotal-label">Subtotal</span>
              <span class="subtotal-amount">{{ store.formatPrice(item.subtotal) }}</span>
            </div>
            <button class="item-remove" @click="store.removeItem(item.id)" title="Remove item">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
              Remove
            </button>
          </div>
        </div>
      </div>

      <!-- Summary -->
      <div class="cart-summary">
        <div class="summary-header">
          <h3>Order Summary</h3>
          <span class="item-count">{{ cartItems.length }} item{{ cartItems.length !== 1 ? 's' : '' }}</span>
        </div>

        <div class="summary-body">
          <div class="summary-row">
            <span>Subtotal</span>
            <span>{{ totalFormatted }}</span>
          </div>
          <div class="summary-row">
            <span>Shipping</span>
            <span class="text-muted">Calculated at checkout</span>
          </div>
          <div class="summary-row discount-row">
            <span>Discount</span>
            <span class="discount-amount">-$0.00</span>
          </div>
          <div class="summary-divider"></div>
          <div class="summary-row summary-total">
            <span>Estimated Total</span>
            <span class="total-price">{{ totalFormatted }}</span>
          </div>
        </div>

        <div class="summary-actions">
          <button class="btn-checkout" @click="goToCheckout">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="1" y="4" width="22" height="16" rx="2" ry="2" />
              <line x1="1" y1="10" x2="23" y2="10" />
            </svg>
            Proceed to Checkout
          </button>
          <button class="btn-continue" @click="goToProducts">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 2.1l4 4-4 4" />
              <path d="M3 12.2v-2a4 4 0 0 1 4-4h12.8M7 21.9l-4-4 4-4" />
              <path d="M21 11.8v2a4 4 0 0 1-4 4H4.2" />
            </svg>
            Continue Shopping
          </button>
        </div>

        <div class="summary-footer">
          <div class="trust-badges">
            <div class="trust-badge">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <span>Secure Checkout</span>
            </div>
            <div class="trust-badge">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
              <span>Healthcare Verified</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 80vh;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  gap: 20px;
}
.header-content h1 {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}
.cart-subtitle {
  font-size: 15px;
  color: var(--color-text-muted);
}
.cart-actions {
  display: flex;
  gap: 12px;
}
.btn-clear {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: transparent;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-clear:hover {
  color: #dc2626;
  border-color: #dc2626;
  background: #fef2f2;
}

.cart-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--color-text-secondary);
}
.loading-spinner {
  margin-bottom: 16px;
}
.spinner {
  animation: spin 1s linear infinite;
  color: var(--color-primary);
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.cart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  color: var(--color-text-secondary);
}
.empty-cart-illustration {
  color: var(--color-text-muted);
  margin-bottom: 24px;
  opacity: 0.6;
}
.cart-empty h2 {
  font-size: 24px;
  color: var(--color-text);
  margin-bottom: 8px;
}
.cart-empty p {
  font-size: 15px;
  color: var(--color-text-muted);
  margin-bottom: 32px;
  max-width: 400px;
}
.empty-suggestions {
  display: flex;
  gap: 24px;
  margin-bottom: 32px;
}
.suggestion-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--color-text-muted);
  font-size: 13px;
}
.suggestion-item svg {
  color: var(--color-primary);
  opacity: 0.7;
}
.btn-shop {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}
.btn-shop:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

/* Cart items */
.cart-content {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 32px;
  align-items: start;
}

@media (max-width: 1024px) {
  .cart-content { grid-template-columns: 1fr; }
  .cart-summary { position: static; }
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cart-item {
  display: flex;
  gap: 20px;
  padding: 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition: box-shadow 0.15s;
}
.cart-item:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.item-image {
  width: 88px;
  height: 88px;
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}
.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.item-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  opacity: 0.5;
}

.item-details {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.item-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.4;
}
.item-price {
  font-size: 14px;
  color: var(--color-text-secondary);
}
.item-stock {
  font-size: 12px;
  color: #16a34a;
  font-weight: 500;
}

.item-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.item-qty {
  display: flex;
  align-items: center;
  gap: 2px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.qty-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  background: var(--color-bg);
  border: none;
  cursor: pointer;
  transition: background 0.1s;
}
.qty-btn:hover:not(:disabled) { background: var(--color-border); }
.qty-btn:disabled { opacity: 0.3; cursor: default; }
.qty-value {
  width: 40px;
  text-align: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.item-subtotal {
  text-align: right;
}
.subtotal-label {
  display: block;
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 2px;
}
.subtotal-amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
}

.item-remove {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  color: var(--color-text-muted);
  background: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.item-remove:hover {
  color: #dc2626;
  border-color: #dc2626;
  background: #fef2f2;
}

/* Summary */
.cart-summary {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  position: sticky;
  top: 24px;
}
.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}
.summary-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}
.item-count {
  font-size: 13px;
  color: var(--color-text-muted);
}

.summary-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.discount-row {
  color: #16a34a;
}
.discount-amount {
  font-weight: 600;
}
.summary-divider {
  height: 1px;
  background: var(--color-border);
  margin: 8px 0;
}
.summary-total {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  padding-top: 8px;
}
.total-price {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-primary);
}
.text-muted { color: var(--color-text-muted); font-size: 13px; }

.summary-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 24px;
}
.btn-checkout {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 16px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}
.btn-checkout:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}
.btn-continue {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px;
  background: transparent;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-continue:hover {
  background: var(--color-bg);
  color: var(--color-text);
  border-color: var(--color-text-muted);
}

.summary-footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}
.trust-badges {
  display: flex;
  gap: 16px;
  justify-content: center;
}
.trust-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.trust-badge svg {
  color: #16a34a;
}

@media (max-width: 640px) {
  .cart-item {
    flex-direction: column;
    align-items: stretch;
  }
  .item-image {
    width: 100%;
    height: 160px;
  }
  .item-actions {
    align-items: stretch;
    flex-direction: row;
    justify-content: space-between;
  }
  .item-qty {
    flex: 1;
  }
  .empty-suggestions {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
