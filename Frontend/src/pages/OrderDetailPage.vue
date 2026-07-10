<script setup>
import { onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '../stores/order.js'

const route = useRoute()
const router = useRouter()
const store = useOrderStore()

const orderId = computed(() => Number(route.params.id))

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

    <div v-if="store.loading" class="loading"><p>Loading order...</p></div>

    <div v-else-if="store.error" class="error">
      <p>{{ store.error }}</p>
      <button @click="goBack">Go Back</button>
    </div>

    <template v-else-if="order">
      <div class="order-header">
        <div>
          <h1>Order #{{ order.id }}</h1>
          <span class="order-date">{{ formatDate(order.createdAt) }}</span>
        </div>
        <span class="order-status" :class="statusClass(order.status)">{{ order.status }}</span>
      </div>

      <!-- Items -->
      <div class="items-section">
        <h2>Items</h2>
        <div class="items-list">
          <div v-for="item in order.items" :key="item.id" class="item-row" @click="goToProduct(item.productId)">
            <div class="item-img">
              <img v-if="item.productImage" :src="item.productImage" :alt="item.productName" />
              <div v-else class="img-placeholder">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
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
            <span class="sf-label">Payment</span>
            <span class="sf-value">{{ order.paymentMethod || 'N/A' }}</span>
          </div>
        </div>
        <div v-if="order.notes" class="order-notes">
          <span class="sf-label">Notes</span>
          <p>{{ order.notes }}</p>
        </div>
      </div>

      <!-- Summary -->
      <div class="order-summary-bar">
        <div class="summary-left">
          <span>{{ order.itemCount }} item{{ order.itemCount !== 1 ? 's' : '' }}</span>
          <span class="summary-total">{{ formatPrice(order.totalAmount) }}</span>
        </div>
        <button v-if="canCancel" class="btn-cancel" @click="handleCancel">Cancel Order</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 80vh;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  border: none;
  background: none;
  cursor: pointer;
}
.back-btn:hover { background: var(--color-bg); }
.loading, .error { text-align: center; padding: 60px 20px; color: var(--color-text-secondary); }

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.order-header h1 { font-size: 26px; font-weight: 700; color: var(--color-text); }
.order-date { font-size: 13px; color: var(--color-text-muted); display: block; margin-top: 4px; }
.order-status {
  padding: 6px 16px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
}
.s-pending { background: #fef3c7; color: #92400e; }
.s-confirmed { background: #dbeafe; color: #1e40af; }
.s-processing { background: #e0e7ff; color: #4338ca; }
.s-shipped { background: #dcfce7; color: #166534; }
.s-delivered { background: #d1fae5; color: #065f46; }
.s-cancelled { background: #fee2e2; color: #991b1b; }

.items-section, .shipping-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 16px;
}
.items-section h2, .shipping-section h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 14px;
}
.items-list { display: flex; flex-direction: column; gap: 10px; }
.item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.1s;
}
.item-row:hover { background: var(--color-bg); }
.item-img {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}
.item-img img { width: 100%; height: 100%; object-fit: cover; }
.img-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: var(--color-text-muted); opacity: 0.5; }
.item-details { flex: 1; display: flex; flex-direction: column; }
.item-name { font-size: 14px; font-weight: 600; color: var(--color-text); }
.item-meta { font-size: 12px; color: var(--color-text-muted); }
.item-subtotal { font-size: 14px; font-weight: 700; color: var(--color-text); }

.shipping-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.shipping-field { display: flex; flex-direction: column; gap: 4px; }
.sf-label { font-size: 11px; font-weight: 600; text-transform: uppercase; color: var(--color-text-muted); letter-spacing: 0.5px; }
.sf-value { font-size: 14px; color: var(--color-text); }
.order-notes { margin-top: 16px; padding-top: 14px; border-top: 1px solid var(--color-border); }
.order-notes p { font-size: 14px; color: var(--color-text-secondary); margin-top: 4px; }

.order-summary-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.summary-left { display: flex; flex-direction: column; gap: 2px; font-size: 13px; color: var(--color-text-secondary); }
.summary-total { font-size: 22px; font-weight: 700; color: var(--color-primary); }
.btn-cancel {
  padding: 10px 20px;
  background: transparent;
  color: #dc2626;
  border: 1px solid #dc2626;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-cancel:hover { background: #fef2f2; }
</style>
