<script setup>
import { onMounted, computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '../../stores/order.js'

const route = useRoute()
const router = useRouter()
const store = useOrderStore()

const orderId = computed(() => Number(route.params.id))
const confirmingDelivery = ref(false)
const deliverySignature = ref('')

function formatPrice(price) {
  if (price == null) return '$0.00'
  return '$' + Number(price).toFixed(2)
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString([], { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function statusClass(status) {
  const map = { PENDING: 's-pending', CONFIRMED: 's-confirmed', PROCESSING: 's-processing', SHIPPED: 's-shipped', DELIVERED: 's-delivered', CANCELLED: 's-cancelled' }
  return map[status] || ''
}

function formatStatus(status) {
  const map = { PENDING: 'Pending', CONFIRMED: 'Confirmed', PROCESSING: 'Processing', SHIPPED: 'Shipped', DELIVERED: 'Delivered', CANCELLED: 'Cancelled' }
  return map[status] || status
}

function formatPaymentMethod(method) {
  if (!method) return 'N/A'
  const map = { CARD: 'Credit/Debit Card', PAYPAL: 'PayPal', BANK_TRANSFER: 'Bank Transfer', CASH_ON_DELIVERY: 'Cash on Delivery' }
  return map[method] || method
}

function isActive(status) {
  const currentStatus = order.value?.status
  if (!currentStatus) return false
  const statusFlow = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED']
  const currentIndex = statusFlow.indexOf(currentStatus)
  const targetIndex = statusFlow.indexOf(status)
  return currentIndex >= targetIndex
}

async function handleReorder() {
  try {
    await store.reorderExistingOrder(orderId.value)
    router.push('/cart')
  } catch (error) {
    console.error('Reorder failed:', error)
  }
}

function goBack() {
  router.push('/orders')
}

function goToProduct(id) {
  router.push(`/products/${id}`)
}

async function handleCancel() {
  if (!confirm('Are you sure you want to cancel this order?')) return
  try {
    await store.cancelExistingOrder(orderId.value)
  } catch (_) { /* error shown in store */ }
}

/* ---------- Delivery confirmation with frontend verification ---------- */
const canConfirmDelivery = computed(() => order.value?.status === 'SHIPPED')

function promptDeliveryVerify() {
  confirmingDelivery.value = true
}

function cancelDeliveryVerify() {
  confirmingDelivery.value = false
}

async function submitDeliveryConfirm() {
  if (!deliverySignature.value.trim()) {
    alert('Please provide your signature to confirm delivery')
    return
  }
  try {
    await store.confirmDelivery(orderId.value, deliverySignature.value)
    confirmingDelivery.value = false
    deliverySignature.value = ''
  } catch (_) { /* error shown in store */ }
}

const order = computed(() => store.selectedOrder)
const canCancel = computed(() => {
  const s = order.value?.status
  return s === 'PENDING' || s === 'CONFIRMED' || s === 'PROCESSING'
})

onMounted(async () => {
  await store.fetchOrderById(orderId.value)
})
</script>

<template>
  <div class="detail-page">
    <button class="back-btn" @click="goBack">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
      </svg>
      Back to Orders
    </button>

    <div v-if="store.loading && !order" class="loading">
      <div class="loading-spinner">
        <svg class="spinner" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
      </div>
      <p>Loading order details...</p>
    </div>

    <div v-else-if="store.error && !order" class="error">
      <div class="error-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
      </div>
      <p>{{ store.error }}</p>
      <button class="btn-back" @click="goBack">Go Back</button>
    </div>

    <template v-else-if="order">
      <div class="order-header">
        <div class="order-info">
          <h1>Order #{{ order.orderNumber || order.id }}</h1>
          <span class="order-date">{{ formatDate(order.createdAt) }}</span>
        </div>
        <div class="order-actions">
          <span class="order-status" :class="statusClass(order.status)">
            <span class="status-dot"></span>
            {{ formatStatus(order.status) }}
          </span>
        </div>
      </div>

      <!-- Order Progress -->
      <div class="order-progress">
        <div class="progress-step" :class="{ active: isActive('PENDING') }">
          <div class="step-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          </div>
          <span class="step-label">Pending</span>
        </div>
        <div class="progress-line" :class="{ active: isActive('CONFIRMED') }"></div>
        <div class="progress-step" :class="{ active: isActive('CONFIRMED') }">
          <div class="step-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </div>
          <span class="step-label">Confirmed</span>
        </div>
        <div class="progress-line" :class="{ active: isActive('PROCESSING') }"></div>
        <div class="progress-step" :class="{ active: isActive('PROCESSING') }">
          <div class="step-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
              <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
              <line x1="12" y1="22.08" x2="12" y2="12" />
            </svg>
          </div>
          <span class="step-label">Processing</span>
        </div>
        <div class="progress-line" :class="{ active: isActive('SHIPPED') }"></div>
        <div class="progress-step" :class="{ active: isActive('SHIPPED') }">
          <div class="step-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="1" y="3" width="15" height="13" />
              <polygon points="16 8 20 8 23 11 23 16 16 16 16 8" />
              <circle cx="5.5" cy="18.5" r="2.5" />
              <circle cx="18.5" cy="18.5" r="2.5" />
            </svg>
          </div>
          <span class="step-label">Shipped</span>
        </div>
        <div class="progress-line" :class="{ active: isActive('DELIVERED') }"></div>
        <div class="progress-step" :class="{ active: isActive('DELIVERED') }">
          <div class="step-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </div>
          <span class="step-label">Delivered</span>
        </div>
      </div>

      <div class="order-content">
        <!-- Items -->
        <div class="items-section">
          <h2>Order Items</h2>
          <div class="items-list">
            <div v-for="item in order.items" :key="item.id" class="item-row" @click="goToProduct(item.productId)">
              <div class="item-img">
                <img v-if="item.productImage" :src="item.productImage" :alt="item.productName" />
                <div v-else class="img-placeholder">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" /></svg>
                </div>
              </div>
              <div class="item-details">
                <span class="item-name">{{ item.productName }}</span>
                <span class="item-meta">{{ formatPrice(item.unitPrice) }} × {{ item.quantity }}</span>
              </div>
              <span class="item-subtotal">{{ formatPrice(item.subtotal) }}</span>
            </div>
          </div>
        </div>

        <!-- Shipping -->
        <div class="shipping-section">
          <h2>Shipping Information</h2>
          <div class="shipping-grid">
            <div class="shipping-field">
              <span class="sf-label">Address</span>
              <span class="sf-value">{{ order.shippingAddress || 'N/A' }}</span>
            </div>
            <div class="shipping-field">
              <span class="sf-label">City</span>
              <span class="sf-value">{{ order.shippingCity || 'N/A' }}</span>
            </div>
            <div class="shipping-field">
              <span class="sf-label">Phone</span>
              <span class="sf-value">{{ order.shippingPhone || 'N/A' }}</span>
            </div>
            <div class="shipping-field">
              <span class="sf-label">Payment Method</span>
              <span class="sf-value">{{ formatPaymentMethod(order.paymentMethod) }}</span>
            </div>
          </div>
          <div v-if="order.notes" class="order-notes">
            <span class="sf-label">Order Notes</span>
            <p>{{ order.notes }}</p>
          </div>
        </div>

        <!-- Pricing Summary -->
        <div class="pricing-section">
          <h2>Order Summary</h2>
          <div class="pricing-details">
            <div class="pricing-row">
              <span>Subtotal</span>
              <span>{{ formatPrice(order.subtotalAmount) }}</span>
            </div>
            <div class="pricing-row">
              <span>Shipping</span>
              <span>{{ formatPrice(order.shippingAmount) }}</span>
            </div>
            <div class="pricing-row">
              <span>Tax</span>
              <span>{{ formatPrice(order.taxAmount) }}</span>
            </div>
            <div v-if="order.discountAmount > 0" class="pricing-row discount">
              <span>Discount</span>
              <span>-{{ formatPrice(order.discountAmount) }}</span>
            </div>
            <div class="pricing-divider"></div>
            <div class="pricing-row total">
              <span>Total</span>
              <span>{{ formatPrice(order.totalAmount) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Action Bar -->
      <div class="order-action-bar">
        <div class="action-info">
          <span class="item-count">{{ order.itemCount }} item{{ order.itemCount !== 1 ? 's' : '' }}</span>
          <span class="order-total-display">{{ formatPrice(order.totalAmount) }}</span>
        </div>
        <div class="action-buttons">
          <button v-if="canCancel" class="btn-cancel" @click="handleCancel">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="15" y1="9" x2="9" y2="15" />
              <line x1="9" y1="9" x2="15" y2="15" />
            </svg>
            Cancel Order
          </button>

          <!-- Delivery confirmation with inline verification -->
          <template v-if="canConfirmDelivery">
            <template v-if="!confirmingDelivery">
              <button class="btn-confirm-delivery" @click="promptDeliveryVerify">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                Confirm Delivery
              </button>
            </template>
            <template v-else>
              <div class="delivery-verify-box">
                <span class="verify-text">Have you received this order?</span>
                <div class="signature-input-group">
                  <label class="signature-label">Please sign to confirm delivery:</label>
                  <textarea 
                    v-model="deliverySignature"
                    class="signature-input"
                    placeholder="Type your full name as signature"
                    rows="2"
                  ></textarea>
                </div>
                <button class="btn-back" @click="cancelDeliveryVerify">No, go back</button>
                <button
                  class="btn-confirm-delivery"
                  :disabled="store.loading || !deliverySignature.trim()"
                  @click="submitDeliveryConfirm"
                >
                  {{ store.loading ? 'Confirming…' : 'Yes, mark as delivered' }}
                </button>
              </div>
            </template>
          </template>

          <button class="btn-reorder" @click="handleReorder">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 4 23 10 17 10" />
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
            </svg>
            Reorder
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 80vh;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  font-size: 14px;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  border: 1px solid var(--color-border);
  background: transparent;
  cursor: pointer;
  transition: all 0.15s;
}
.back-btn:hover {
  background: var(--color-bg);
  color: var(--color-text);
}
.loading, .error {
  text-align: center;
  padding: 80px 20px;
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
.error-icon {
  color: #dc2626;
  margin-bottom: 16px;
}
.btn-back {
  margin-top: 16px;
  padding: 10px 20px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  gap: 20px;
}
.order-info h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}
.order-date {
  font-size: 14px;
  color: var(--color-text-muted);
}
.order-actions {
  display: flex;
  gap: 12px;
}
.order-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 600;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}
.s-pending { background: #fef3c7; color: #92400e; }
.s-confirmed { background: #dbeafe; color: #1e40af; }
.s-processing { background: #e0e7ff; color: #4338ca; }
.s-shipped { background: #dcfce7; color: #166534; }
.s-delivered { background: #d1fae5; color: #065f46; }
.s-cancelled { background: #fee2e2; color: #991b1b; }

.order-progress {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
  padding: 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
}
.step-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-bg);
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}
.progress-step.active .step-icon {
  background: var(--color-primary);
  color: #fff;
}
.step-label {
  font-size: 12px;
  color: var(--color-text-muted);
  font-weight: 500;
}
.progress-step.active .step-label {
  color: var(--color-text);
  font-weight: 600;
}
.progress-line {
  flex: 1;
  height: 2px;
  background: var(--color-border);
  margin: 0 8px;
  transition: background 0.3s;
}
.progress-line.active {
  background: var(--color-primary);
}

.order-content {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
  margin-bottom: 24px;
}

@media (max-width: 1024px) {
  .order-content { grid-template-columns: 1fr; }
}

.items-section, .shipping-section, .pricing-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}
.items-section h2, .shipping-section h2, .pricing-section h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}
.items-list { display: flex; flex-direction: column; gap: 12px; }
.item-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.1s;
}
.item-row:hover { background: var(--color-bg); }
.item-img {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-primary-bg) 0%, #dbeafe 100%);
}
.item-img img { width: 100%; height: 100%; object-fit: cover; }
.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  opacity: 0.5;
}
.item-details { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.item-name { font-size: 15px; font-weight: 600; color: var(--color-text); }
.item-meta { font-size: 13px; color: var(--color-text-muted); }
.item-subtotal { font-size: 16px; font-weight: 700; color: var(--color-text); }

.shipping-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.shipping-field { display: flex; flex-direction: column; gap: 6px; }
.sf-label { font-size: 12px; font-weight: 600; text-transform: uppercase; color: var(--color-text-muted); letter-spacing: 0.5px; }
.sf-value { font-size: 14px; color: var(--color-text); }
.order-notes { margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--color-border); }
.order-notes p { font-size: 14px; color: var(--color-text-secondary); margin-top: 6px; }

.pricing-details { display: flex; flex-direction: column; gap: 12px; }
.pricing-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.pricing-row.discount { color: #16a34a; }
.pricing-divider { height: 1px; background: var(--color-border); margin: 4px 0; }
.pricing-row.total {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  padding-top: 8px;
}
.pricing-row.total span:last-child {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
}

.order-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.action-info { display: flex; flex-direction: column; gap: 4px; }
.item-count { font-size: 14px; color: var(--color-text-muted); }
.order-total-display { font-size: 24px; font-weight: 700; color: var(--color-primary); }
.action-buttons { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.btn-cancel {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: transparent;
  color: #dc2626;
  border: 1px solid #dc2626;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-cancel:hover { background: #fef2f2; }

.btn-confirm-delivery {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: #16a34a;
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-confirm-delivery:hover { background: #15803d; }
.btn-confirm-delivery:disabled { opacity: 0.6; cursor: not-allowed; }

.delivery-verify-box {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
  min-width: 300px;
}
.verify-text {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: 500;
}
.signature-input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.signature-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}
.signature-input {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  background: var(--color-bg);
  color: var(--color-text);
}
.signature-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.signature-input::placeholder {
  color: var(--color-text-muted);
}

.btn-reorder {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-reorder:hover { background: var(--color-primary-dark); }

@media (max-width: 640px) {
  .order-header {
    flex-direction: column;
    align-items: stretch;
  }
  .order-progress {
    overflow-x: auto;
    padding: 16px;
  }
  .progress-step {
    min-width: 60px;
  }
  .order-action-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  .action-buttons {
    flex-direction: column;
    align-items: stretch;
  }

  .shipping-grid {
    grid-template-columns: 1fr;
  }
}
</style>