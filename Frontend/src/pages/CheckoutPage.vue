<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart.js'
import { useOrderStore } from '../stores/order.js'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const cartStore = useCartStore()
const orderStore = useOrderStore()
const authStore = useAuthStore()

const form = ref({
  shippingAddress: '',
  shippingCity: '',
  shippingPhone: '',
  paymentMethod: 'CASH_ON_DELIVERY',
  notes: '',
})

const submitting = ref(false)
const orderPlaced = ref(null)
const formErrors = ref({})

function validate() {
  const errors = {}
  if (!form.value.shippingAddress.trim()) errors.shippingAddress = 'Address is required'
  if (!form.value.shippingCity.trim()) errors.shippingCity = 'City is required'
  if (!form.value.shippingPhone.trim()) errors.shippingPhone = 'Phone number is required'
  formErrors.value = errors
  return Object.keys(errors).length === 0
}

async function placeOrder() {
  if (!validate()) return
  submitting.value = true
  try {
    const order = await orderStore.placeOrder(form.value)
    orderPlaced.value = order
    cartStore.itemCount = 0
    cartStore.items = []
  } catch (_) {
    // error shown via store
  } finally {
    submitting.value = false
  }
}

function goToOrders() {
  router.push('/orders')
}

function goToCart() {
  router.push('/cart')
}

function goToProducts() {
  router.push('/products')
}

const totalFormatted = computed(() => cartStore.formatPrice(cartStore.total))

onMounted(async () => {
  await cartStore.fetchCart()
  if (cartStore.isEmpty) {
    router.push('/cart')
  }
})
</script>

<template>
  <div class="checkout-page">
    <div class="checkout-header">
      <button class="back-btn" @click="goToCart">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
        </svg>
        Back to Cart
      </button>
      <h1>Checkout</h1>
    </div>

    <!-- Order placed success -->
    <div v-if="orderPlaced" class="order-success">
      <div class="success-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
        </svg>
      </div>
      <h2>Order Placed Successfully!</h2>
      <p class="order-id">Order #{{ orderPlaced.id }}</p>
      <p class="order-total">Total: {{ cartStore.formatPrice(orderPlaced.totalAmount) }}</p>
      <div class="success-actions">
        <button class="btn-primary" @click="goToOrders">View My Orders</button>
        <button class="btn-secondary" @click="goToProducts">Continue Shopping</button>
      </div>
    </div>

    <template v-else>
      <div class="checkout-layout">
        <!-- Shipping form -->
        <div class="checkout-form-section">
          <h2>Shipping Information</h2>

          <div class="form-group">
            <label>Full Address <span class="required">*</span></label>
            <input
              v-model="form.shippingAddress"
              type="text"
              placeholder="Street address, apartment, etc."
              :class="{ 'input-error': formErrors.shippingAddress }"
            />
            <span v-if="formErrors.shippingAddress" class="field-error">{{ formErrors.shippingAddress }}</span>
          </div>

          <div class="form-group">
            <label>City <span class="required">*</span></label>
            <input
              v-model="form.shippingCity"
              type="text"
              placeholder="City, State, ZIP"
              :class="{ 'input-error': formErrors.shippingCity }"
            />
            <span v-if="formErrors.shippingCity" class="field-error">{{ formErrors.shippingCity }}</span>
          </div>

          <div class="form-group">
            <label>Phone Number <span class="required">*</span></label>
            <input
              v-model="form.shippingPhone"
              type="tel"
              placeholder="+1 (555) 000-0000"
              :class="{ 'input-error': formErrors.shippingPhone }"
            />
            <span v-if="formErrors.shippingPhone" class="field-error">{{ formErrors.shippingPhone }}</span>
          </div>

          <h2 style="margin-top: 24px;">Payment Method</h2>
          <div class="payment-options">
            <label class="payment-option" :class="{ selected: form.paymentMethod === 'CASH_ON_DELIVERY' }">
              <input type="radio" v-model="form.paymentMethod" value="CASH_ON_DELIVERY" />
              <div class="payment-info">
                <span class="payment-title">Cash on Delivery</span>
                <span class="payment-desc">Pay when you receive your order</span>
              </div>
            </label>
            <label class="payment-option" :class="{ selected: form.paymentMethod === 'CARD' }">
              <input type="radio" v-model="form.paymentMethod" value="CARD" />
              <div class="payment-info">
                <span class="payment-title">Credit / Debit Card</span>
                <span class="payment-desc">Coming soon</span>
              </div>
            </label>
          </div>

          <div class="form-group">
            <label>Order Notes (optional)</label>
            <textarea
              v-model="form.notes"
              placeholder="Special delivery instructions..."
              rows="3"
            ></textarea>
          </div>

          <div v-if="orderStore.error" class="submit-error">{{ orderStore.error }}</div>
        </div>

        <!-- Order summary -->
        <div class="checkout-summary-section">
          <div class="summary-card">
            <h3>Order Summary</h3>
            <div class="summary-items">
              <div v-for="item in cartStore.items" :key="item.id" class="summary-item">
                <div class="si-left">
                  <span class="si-name">{{ item.productName }}</span>
                  <span class="si-qty">Qty: {{ item.quantity }}</span>
                </div>
                <span class="si-price">{{ cartStore.formatPrice(item.subtotal) }}</span>
              </div>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-total-row">
              <span>Total</span>
              <span class="total-amount">{{ totalFormatted }}</span>
            </div>
            <button
              class="btn-place-order"
              :disabled="submitting || cartStore.isEmpty"
              @click="placeOrder"
            >
              <span v-if="submitting">Placing Order...</span>
              <span v-else>Place Order — {{ totalFormatted }}</span>
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.checkout-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 80vh;
}
.checkout-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}
.checkout-header h1 { font-size: 28px; font-weight: 700; color: var(--color-text); }
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: background 0.15s;
  border: none;
  background: none;
  cursor: pointer;
}
.back-btn:hover { background: var(--color-bg); }

.checkout-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 32px;
  align-items: start;
}
@media (max-width: 768px) {
  .checkout-layout { grid-template-columns: 1fr; }
}

/* Form */
.checkout-form-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}
.checkout-form-section h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}
.form-group { margin-bottom: 16px; }
.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
}
.required { color: #dc2626; }
.form-group input, .form-group textarea {
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text);
  outline: none;
  transition: border 0.15s;
  box-sizing: border-box;
}
.form-group input:focus, .form-group textarea:focus { border-color: var(--color-primary); }
.input-error { border-color: #dc2626 !important; }
.field-error { font-size: 12px; color: #dc2626; margin-top: 4px; display: block; }

.payment-options { display: flex; flex-direction: column; gap: 10px; margin-bottom: 20px; }
.payment-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border 0.15s;
}
.payment-option.selected { border-color: var(--color-primary); }
.payment-option input[type="radio"] { accent-color: var(--color-primary); }
.payment-title { display: block; font-size: 14px; font-weight: 600; color: var(--color-text); }
.payment-desc { font-size: 12px; color: var(--color-text-muted); }

/* Summary */
.checkout-summary-section { position: sticky; top: 24px; }
.summary-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}
.summary-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}
.summary-items { display: flex; flex-direction: column; gap: 12px; }
.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.si-left { display: flex; flex-direction: column; gap: 2px; }
.si-name { font-size: 14px; color: var(--color-text); }
.si-qty { font-size: 12px; color: var(--color-text-muted); }
.si-price { font-size: 14px; font-weight: 600; color: var(--color-text); }
.summary-divider { height: 1px; background: var(--color-border); margin: 14px 0; }
.summary-total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}
.total-amount { font-size: 24px; font-weight: 700; color: var(--color-primary); }
.btn-place-order {
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
.btn-place-order:hover:not(:disabled) { background: #1d4ed8; }
.btn-place-order:disabled { opacity: 0.6; cursor: default; }
.submit-error {
  padding: 10px 14px;
  background: #fef2f2;
  color: #dc2626;
  border-radius: var(--radius-md);
  font-size: 13px;
  margin-top: 12px;
}

/* Success */
.order-success {
  text-align: center;
  padding: 60px 20px;
}
.success-icon { color: #16a34a; margin-bottom: 16px; }
.order-success h2 { font-size: 24px; color: var(--color-text); margin-bottom: 8px; }
.order-id { font-size: 16px; color: var(--color-primary); font-weight: 600; margin-bottom: 4px; }
.order-total { font-size: 18px; color: var(--color-text-secondary); margin-bottom: 24px; }
.success-actions { display: flex; gap: 12px; justify-content: center; }
.btn-primary {
  padding: 12px 24px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.btn-secondary {
  padding: 12px 24px;
  background: transparent;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
</style>
