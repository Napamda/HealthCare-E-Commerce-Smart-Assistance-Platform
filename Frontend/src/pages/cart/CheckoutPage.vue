<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart.js'
import { useOrderStore } from '../../stores/order.js'
import { useAuthStore } from '../../stores/auth.js'
import { validateStock } from '../../services/inventory.js'
import { previewCheckout } from '../../services/order.js'

const router = useRouter()
const cartStore = useCartStore()
const orderStore = useOrderStore()
const authStore = useAuthStore()

const form = ref({
  shippingAddress: '',
  shippingCity: '',
  shippingPhone: '',
  paymentMethod: 'CASH_ON_DELIVERY',
  shippingMethod: 'STANDARD',
  discountCode: '',
  confirmPrescription: false,
  notes: '',
})

const submitting = ref(false)
const validatingStock = ref(false)
const previewLoading = ref(false)
const discountApplying = ref(false)
const orderPlaced = ref(null)
const formErrors = ref({})
const inventoryErrors = ref([])
const discountError = ref('')

// Preview state — populated by /api/orders/preview
const preview = ref({
  subtotalAmount: 0,
  discountAmount: 0,
  discountedSubtotal: 0,
  shippingAmount: 0,
  taxAmount: 0,
  totalAmount: 0,
  shippingMethod: 'STANDARD',
  discountCode: null,
  hasPrescriptionRequired: false,
  discountValid: false,
  itemCount: 0,
})

const hasPrescriptionItems = computed(() => preview.value.hasPrescriptionRequired)
const hasDiscount = computed(() => preview.value.discountValid && preview.value.discountAmount > 0)

const SHIPPING_METHODS = [
  { value: 'STANDARD', label: 'Standard', desc: '5–7 business days', price: '$4.99' },
  { value: 'EXPRESS', label: 'Express', desc: '2–3 business days', price: '$9.99' },
  { value: 'SAME_DAY', label: 'Same Day', desc: 'Delivery today', price: '$14.99' },
]

function formatPrice(price) {
  if (price == null) return '$0.00'
  return '$' + Number(price).toFixed(2)
}

function validate() {
  const errors = {}
  if (!form.value.shippingAddress.trim()) errors.shippingAddress = 'Address is required'
  if (!form.value.shippingCity.trim()) errors.shippingCity = 'City is required'
  if (!form.value.shippingPhone.trim()) errors.shippingPhone = 'Phone number is required'
  if (hasPrescriptionItems.value && !form.value.confirmPrescription) {
    errors.confirmPrescription = 'You must confirm you have a valid prescription'
  }
  formErrors.value = errors
  return Object.keys(errors).length === 0
}

async function fetchPreview() {
  previewLoading.value = true
  try {
    const data = await previewCheckout({
      shippingMethod: form.value.shippingMethod,
      discountCode: form.value.discountCode || undefined,
    })
    preview.value = data
    discountError.value = data.discountCode && !data.discountValid ? data.discountError || '' : ''
  } catch (e) {
    discountError.value = e.response?.data?.error || 'Failed to load pricing'
  } finally {
    previewLoading.value = false
  }
}

async function applyDiscount() {
  if (!form.value.discountCode.trim()) {
    discountError.value = ''
    await fetchPreview()
    return
  }
  discountApplying.value = true
  discountError.value = ''
  try {
    await fetchPreview()
  } catch (_) {
    discountError.value = 'Failed to apply discount code'
  } finally {
    discountApplying.value = false
  }
}

function clearDiscount() {
  form.value.discountCode = ''
  discountError.value = ''
  fetchPreview()
}

// Re-fetch preview when shipping method changes
watch(() => form.value.shippingMethod, () => {
  fetchPreview()
})

async function placeOrder() {
  if (!validate()) return

  validatingStock.value = true
  inventoryErrors.value = []
  try {
    const items = cartStore.items.map(item => ({
      productId: item.productId,
      quantity: item.quantity,
    }))
    const result = await validateStock(items)
    if (!result.valid) {
      inventoryErrors.value = result.errors
      return
    }
  } catch (e) {
    inventoryErrors.value = [{ message: 'Could not verify stock availability. Please try again.' }]
    return
  } finally {
    validatingStock.value = false
  }

  submitting.value = true
  try {
    const order = await orderStore.placeOrder({
      shippingAddress: form.value.shippingAddress,
      shippingCity: form.value.shippingCity,
      shippingPhone: form.value.shippingPhone,
      paymentMethod: form.value.paymentMethod,
      shippingMethod: form.value.shippingMethod,
      discountCode: form.value.discountCode || undefined,
      confirmPrescription: form.value.confirmPrescription || undefined,
      notes: form.value.notes,
    })
    orderPlaced.value = order
    cartStore.itemCount = 0
    cartStore.items = []
  } catch (_) {
    // error shown via store
  } finally {
    submitting.value = false
  }
}

function goToOrders() { router.push('/orders') }
function goToCart() { router.push('/cart') }
function goToProducts() { router.push('/products') }

onMounted(async () => {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'Login', query: { redirect: '/checkout' } })
    return
  }
  await cartStore.fetchCart()
  if (cartStore.isEmpty) {
    router.push('/cart')
    return
  }
  fetchPreview()
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
      <p class="order-total">Total: {{ formatPrice(orderPlaced.totalAmount) }}</p>
      <div class="success-actions">
        <button class="btn-primary" @click="goToOrders">View My Orders</button>
        <button class="btn-secondary" @click="goToProducts">Continue Shopping</button>
      </div>
    </div>

    <template v-else>
      <div class="checkout-layout">
        <!-- Left column: form -->
        <div class="checkout-form-section">
          <h2>Shipping Information</h2>

          <div class="form-group">
            <label>Full Address <span class="required">*</span></label>
            <input v-model="form.shippingAddress" type="text"
              placeholder="Street address, apartment, etc."
              :class="{ 'input-error': formErrors.shippingAddress }" />
            <span v-if="formErrors.shippingAddress" class="field-error">{{ formErrors.shippingAddress }}</span>
          </div>

          <div class="form-group">
            <label>City <span class="required">*</span></label>
            <input v-model="form.shippingCity" type="text"
              placeholder="City, State, ZIP"
              :class="{ 'input-error': formErrors.shippingCity }" />
            <span v-if="formErrors.shippingCity" class="field-error">{{ formErrors.shippingCity }}</span>
          </div>

          <div class="form-group">
            <label>Phone Number <span class="required">*</span></label>
            <input v-model="form.shippingPhone" type="tel"
              placeholder="+1 (555) 000-0000"
              :class="{ 'input-error': formErrors.shippingPhone }" />
            <span v-if="formErrors.shippingPhone" class="field-error">{{ formErrors.shippingPhone }}</span>
          </div>

          <!-- Shipping method -->
          <h2 style="margin-top: 28px;">Shipping Method</h2>
          <div class="shipping-options">
            <label v-for="s in SHIPPING_METHODS" :key="s.value"
              class="shipping-option"
              :class="{ selected: form.shippingMethod === s.value }">
              <input type="radio" v-model="form.shippingMethod" :value="s.value" />
              <div class="shipping-info">
                <span class="shipping-label">{{ s.label }}</span>
                <span class="shipping-desc">{{ s.desc }}</span>
              </div>
              <span class="shipping-price">{{ s.price }}</span>
            </label>
          </div>

          <!-- Discount code -->
          <h2 style="margin-top: 28px;">Discount Code</h2>
          <div class="discount-row">
            <input v-model="form.discountCode" type="text"
              placeholder="Enter discount code"
              class="discount-input"
              :disabled="hasDiscount"
              @keyup.enter="applyDiscount" />
            <button v-if="!hasDiscount" class="btn-apply-discount"
              :disabled="discountApplying || !form.discountCode.trim()"
              @click="applyDiscount">
              {{ discountApplying ? 'Applying...' : 'Apply' }}
            </button>
            <button v-else class="btn-remove-discount" @click="clearDiscount">
              Remove
            </button>
          </div>
          <div v-if="hasDiscount" class="discount-success">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            Code <strong>{{ preview.discountCode }}</strong> applied — saved {{ formatPrice(preview.discountAmount) }}
          </div>
          <div v-if="discountError" class="discount-error">{{ discountError }}</div>

          <!-- Prescription confirmation -->
          <div v-if="hasPrescriptionItems" class="rx-confirm">
            <div class="rx-alert">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              This order contains prescription-only products.
            </div>
            <label class="rx-checkbox" :class="{ 'input-error': formErrors.confirmPrescription }">
              <input type="checkbox" v-model="form.confirmPrescription" />
              <span>I confirm I have a valid prescription for these items</span>
            </label>
            <span v-if="formErrors.confirmPrescription" class="field-error">{{ formErrors.confirmPrescription }}</span>
          </div>

          <!-- Payment method -->
          <h2 style="margin-top: 28px;">Payment Method</h2>
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
            <textarea v-model="form.notes" placeholder="Special delivery instructions..." rows="3"></textarea>
          </div>

          <div v-if="inventoryErrors.length > 0" class="submit-error">
            <div v-for="(err, i) in inventoryErrors" :key="i" class="inv-error-item">
              <strong>{{ err.productName || 'Stock' }}:</strong> {{ err.message }}
            </div>
          </div>
          <div v-if="orderStore.error" class="submit-error">{{ orderStore.error }}</div>
        </div>

        <!-- Right column: summary -->
        <div class="checkout-summary-section">
          <div class="summary-card">
            <h3>Order Summary</h3>

            <div class="summary-items">
              <div v-for="item in cartStore.items" :key="item.id" class="summary-item">
                <div class="si-left">
                  <span class="si-name">{{ item.productName }}</span>
                  <span class="si-qty">
                    Qty: {{ item.quantity }}
                    <span v-if="item.prescriptionRequired" class="si-rx-badge">Rx</span>
                  </span>
                </div>
                <span class="si-price">{{ formatPrice(item.subtotal) }}</span>
              </div>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-row">
              <span>Subtotal ({{ preview.itemCount }} items)</span>
              <span>{{ formatPrice(preview.subtotalAmount) }}</span>
            </div>

            <div v-if="hasDiscount" class="summary-row summary-discount">
              <span>Discount ({{ preview.discountCode }})</span>
              <span>-{{ formatPrice(preview.discountAmount) }}</span>
            </div>

            <div class="summary-row">
              <span>Shipping</span>
              <span v-if="previewLoading">...</span>
              <span v-else-if="preview.shippingAmount > 0">{{ formatPrice(preview.shippingAmount) }}</span>
              <span v-else class="text-muted">Free</span>
            </div>

            <div class="summary-row">
              <span>Tax (8%)</span>
              <span>{{ formatPrice(preview.taxAmount) }}</span>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-total-row">
              <span>Total</span>
              <span class="total-amount">{{ formatPrice(preview.totalAmount) }}</span>
            </div>

            <button class="btn-place-order"
              :disabled="submitting || validatingStock || cartStore.isEmpty"
              @click="placeOrder">
              <span v-if="validatingStock">Validating stock...</span>
              <span v-else-if="submitting">Placing Order...</span>
              <span v-else>Place Order — {{ formatPrice(preview.totalAmount) }}</span>
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
  grid-template-columns: 1fr 380px;
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
  margin-bottom: 14px;
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

/* Shipping options */
.shipping-options { display: flex; flex-direction: column; gap: 8px; }
.shipping-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border 0.15s;
}
.shipping-option.selected { border-color: var(--color-primary); }
.shipping-option input[type="radio"] { accent-color: var(--color-primary); }
.shipping-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.shipping-label { font-size: 14px; font-weight: 600; color: var(--color-text); }
.shipping-desc { font-size: 12px; color: var(--color-text-muted); }
.shipping-price { font-size: 14px; font-weight: 600; color: var(--color-text); }

/* Discount */
.discount-row { display: flex; gap: 8px; }
.discount-input {
  flex: 1;
  padding: 10px 14px;
  font-size: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text);
  outline: none;
  transition: border 0.15s;
}
.discount-input:focus { border-color: var(--color-primary); }
.discount-input:disabled { opacity: 0.5; }
.btn-apply-discount, .btn-remove-discount {
  padding: 10px 18px;
  font-size: 13px;
  font-weight: 600;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}
.btn-apply-discount {
  background: var(--color-primary);
  color: #fff;
}
.btn-apply-discount:hover:not(:disabled) { background: #1d4ed8; }
.btn-apply-discount:disabled { opacity: 0.5; cursor: default; }
.btn-remove-discount {
  background: #fef2f2;
  color: #dc2626;
}
.btn-remove-discount:hover { background: #fee2e2; }
.discount-success {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 13px;
  color: #16a34a;
}
.discount-error {
  margin-top: 8px;
  font-size: 13px;
  color: #dc2626;
}

/* Prescription */
.rx-confirm { margin-top: 20px; }
.rx-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: var(--radius-md);
  font-size: 13px;
  color: #92400e;
  margin-bottom: 10px;
}
.rx-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text);
  cursor: pointer;
  padding: 6px 0;
}
.rx-checkbox input[type="checkbox"] { accent-color: var(--color-primary); width: 16px; height: 16px; }

/* Payment */
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
.summary-items { display: flex; flex-direction: column; gap: 10px; }
.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.si-left { display: flex; flex-direction: column; gap: 2px; }
.si-name { font-size: 14px; color: var(--color-text); }
.si-qty { font-size: 12px; color: var(--color-text-muted); }
.si-rx-badge {
  display: inline-block;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 700;
  color: #b91c1c;
  background: #fef2f2;
  border-radius: 3px;
  margin-left: 4px;
}
.si-price { font-size: 14px; font-weight: 600; color: var(--color-text); }
.summary-divider { height: 1px; background: var(--color-border); margin: 14px 0; }
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.summary-discount { color: #16a34a; }
.summary-total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}
.total-amount { font-size: 24px; font-weight: 700; color: var(--color-primary); }
.text-muted { color: var(--color-text-muted); font-size: 13px; }
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
.inv-error-item { margin-bottom: 4px; }
.inv-error-item:last-child { margin-bottom: 0; }

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