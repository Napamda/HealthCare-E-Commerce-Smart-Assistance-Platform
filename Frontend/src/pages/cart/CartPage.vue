<script setup>
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart.js'
import { useAuthStore } from '../stores/auth.js'
import { useCategoryStore } from '../stores/category.js'

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

onMounted(() => {
  store.fetchCart()
  categoryStore.fetchTree()
})
</script>

<template>
  <div class="cart-page">
    <div class="cart-header">
      <h1>Shopping Cart</h1>
      <span v-if="!isEmpty" class="cart-count">{{ cartItems.length }} item{{ cartItems.length !== 1 ? 's' : '' }}</span>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="cart-loading">
      <p>Loading cart...</p>
    </div>

    <!-- Empty cart -->
    <div v-else-if="isEmpty" class="cart-empty">
      <div class="empty-illustration">
        <div class="empty-icon-circle">
          <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
          </svg>
        </div>
        <div class="empty-dots">
          <span v-for="i in 3" :key="i" class="dot" :style="{ animationDelay: (i * 0.15) + 's' }"></span>
        </div>
      </div>
      <h2>Your cart is empty</h2>
      <p class="empty-subtitle">Looks like you haven't added anything yet. Start exploring our health & wellness products.</p>

      <div v-if="suggestedCategories.length > 0" class="empty-categories">
        <span class="empty-cats-label">Browse by category</span>
        <div class="empty-cats-row">
          <button
            v-for="cat in suggestedCategories"
            :key="cat.name"
            class="empty-cat-chip"
            @click="goToCategory(cat.name)"
          >
            <span class="cat-chip-icon" v-html="cat.icon"></span>
            {{ cat.name }}
          </button>
        </div>
      </div>

      <button class="btn-shop" @click="goToProducts">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        Browse All Products
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

          <div class="item-info">
            <h3 class="item-name">{{ item.productName }}</h3>
            <span class="item-price">{{ store.formatPrice(item.unitPrice) }} each</span>
          </div>

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
            {{ store.formatPrice(item.subtotal) }}
          </div>

          <button class="item-remove" @click="store.removeItem(item.id)" title="Remove item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Summary -->
      <div class="cart-summary">
        <div class="summary-row">
          <span>Subtotal</span>
          <span>{{ totalFormatted }}</span>
        </div>
        <div class="summary-row">
          <span>Shipping</span>
          <span class="text-muted">Calculated at checkout</span>
        </div>
        <div class="summary-row summary-total">
          <span>Estimated Total</span>
          <span class="total-price">{{ totalFormatted }}</span>
        </div>
        <button class="btn-checkout" @click="goToCheckout">
          {{ isGuest ? 'Login to Checkout' : 'Proceed to Checkout' }}
        </button>
        <button class="btn-continue" @click="goToProducts">
          Continue Shopping
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 80vh;
}

.cart-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
}
.cart-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
}
.cart-count {
  font-size: 14px;
  color: var(--color-text-muted);
}

.cart-loading, .cart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  min-height: 60vh;
}
.cart-empty { padding: 40px 20px; }

/* Illustration */
.empty-illustration {
  position: relative;
  margin-bottom: 24px;
}
.empty-icon-circle {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 50%, #e0e7ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  box-shadow: 0 0 0 12px rgba(59, 130, 246, 0.06), 0 0 0 24px rgba(59, 130, 246, 0.03);
}
.empty-dots {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 20px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  opacity: 0.3;
  animation: dotPulse 1.6s ease-in-out infinite;
}
@keyframes dotPulse {
  0%, 100% { opacity: 0.15; transform: scale(0.8); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

.cart-empty h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
}
.empty-subtitle {
  font-size: 14px;
  color: var(--color-text-muted);
  max-width: 400px;
  margin-bottom: 28px;
  line-height: 1.5;
}

/* Category chips */
.empty-categories {
  margin-bottom: 24px;
}
.empty-cats-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}
.empty-cats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
.empty-cat-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.15s;
}
.empty-cat-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #eff6ff;
}
.cat-chip-icon {
  display: flex;
  align-items: center;
  opacity: 0.7;
}

.btn-shop {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}
.btn-shop:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
}
.btn-shop:active { transform: translateY(0); }

/* Cart items */
.cart-content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 32px;
  align-items: start;
}

.guest-banner {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: var(--radius-md);
  font-size: 13px;
  color: #92400e;
}
.guest-login-link {
  color: var(--color-primary);
  font-weight: 600;
  text-decoration: underline;
}

@media (max-width: 768px) {
  .cart-content { grid-template-columns: 1fr; }
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.item-image {
  width: 72px;
  height: 72px;
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

.item-info { flex: 1; min-width: 0; }
.item-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-price {
  font-size: 13px;
  color: var(--color-text-secondary);
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
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
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
  width: 36px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.item-subtotal {
  width: 80px;
  text-align: right;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
}

.item-remove {
  padding: 6px;
  color: var(--color-text-muted);
  background: none;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
}
.item-remove:hover {
  color: #dc2626;
  background: #fef2f2;
}

/* Summary */
.cart-summary {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  position: sticky;
  top: 80px;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.summary-total {
  border-top: 1px solid var(--color-border);
  margin-top: 8px;
  padding-top: 14px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}
.total-price {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
}
.text-muted { color: var(--color-text-muted); font-size: 13px; }

.btn-checkout {
  width: 100%;
  margin-top: 20px;
  padding: 14px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-checkout:hover { background: #1d4ed8; }

.btn-continue {
  width: 100%;
  margin-top: 10px;
  padding: 12px;
  background: transparent;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-continue:hover {
  background: var(--color-bg);
  color: var(--color-text);
}
</style>
