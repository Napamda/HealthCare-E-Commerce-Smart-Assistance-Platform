<script setup>
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart.js'

const router = useRouter()
const store = useCartStore()

const isEmpty = computed(() => store.isEmpty)
const totalFormatted = computed(() => store.formatPrice(store.total))
const cartItems = computed(() => store.items)

function goToProducts() {
  router.push('/products')
}

function goToCheckout() {
  router.push('/checkout')
}

onMounted(() => {
  store.fetchCart()
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
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" />
        <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
      </svg>
      <h2>Your cart is empty</h2>
      <p>Browse our products and add items to your cart.</p>
      <button class="btn-shop" @click="goToProducts">Browse Products</button>
    </div>

    <!-- Cart with items -->
    <div v-else class="cart-content">
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
          Proceed to Checkout
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
  padding: 80px 20px;
  text-align: center;
  color: var(--color-text-secondary);
}
.cart-empty svg { color: var(--color-text-muted); margin-bottom: 16px; }
.cart-empty h2 { font-size: 20px; color: var(--color-text); margin-bottom: 8px; }
.cart-empty p { margin-bottom: 24px; }

.btn-shop {
  padding: 12px 28px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-shop:hover { background: #1d4ed8; }

/* Cart items */
.cart-content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 32px;
  align-items: start;
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
