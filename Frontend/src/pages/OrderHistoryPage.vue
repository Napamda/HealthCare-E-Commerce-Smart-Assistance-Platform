<script setup>
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '../stores/order.js'

const router = useRouter()
const store = useOrderStore()

const orders = computed(() => store.orders)
const pagination = computed(() => store.pagination)

function formatPrice(price) {
  if (price == null) return '$0.00'
  return '$' + Number(price).toFixed(2)
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

function statusClass(status) {
  const map = {
    PENDING: 'status-pending',
    CONFIRMED: 'status-confirmed',
    PROCESSING: 'status-processing',
    SHIPPED: 'status-shipped',
    DELIVERED: 'status-delivered',
    CANCELLED: 'status-cancelled',
  }
  return map[status] || ''
}

function goToDetail(id) {
  router.push(`/orders/${id}`)
}

function goToProducts() {
  router.push('/products')
}

onMounted(() => {
  store.fetchOrders()
})
</script>

<template>
  <div class="orders-page">
    <div class="orders-header">
      <h1>My Orders</h1>
    </div>

    <div v-if="store.loading" class="orders-loading">
      <p>Loading orders...</p>
    </div>

    <div v-else-if="orders.length === 0" class="orders-empty">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
        <polyline points="14 2 14 8 20 8" />
        <line x1="16" y1="13" x2="8" y2="13" />
        <line x1="16" y1="17" x2="8" y2="17" />
      </svg>
      <h2>No orders yet</h2>
      <p>Start shopping to place your first order.</p>
      <button class="btn-shop" @click="goToProducts">Browse Products</button>
    </div>

    <div v-else class="orders-list">
      <div v-for="order in orders" :key="order.id" class="order-card" @click="goToDetail(order.id)">
        <div class="order-card-header">
          <div>
            <span class="order-id">Order #{{ order.id }}</span>
            <span class="order-date">{{ formatDate(order.createdAt) }}</span>
          </div>
          <span class="order-status" :class="statusClass(order.status)">{{ order.status }}</span>
        </div>
        <div class="order-card-body">
          <div class="order-preview-items">
            <div v-for="item in order.items.slice(0, 3)" :key="item.id" class="preview-item">
              <div class="preview-img">
                <img v-if="item.productImage" :src="item.productImage" :alt="item.productName" />
                <div v-else class="preview-placeholder">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" /></svg>
                </div>
              </div>
              <span class="preview-name">{{ item.productName }}</span>
              <span class="preview-qty">×{{ item.quantity }}</span>
            </div>
            <span v-if="order.items.length > 3" class="more-items">+{{ order.items.length - 3 }} more</span>
          </div>
          <div class="order-card-footer">
            <span class="order-item-count">{{ order.itemCount }} item{{ order.itemCount !== 1 ? 's' : '' }}</span>
            <span class="order-total">{{ formatPrice(order.totalAmount) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 80vh;
}
.orders-header { margin-bottom: 24px; }
.orders-header h1 { font-size: 28px; font-weight: 700; color: var(--color-text); }

.orders-loading, .orders-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  color: var(--color-text-secondary);
}
.orders-empty svg { color: var(--color-text-muted); margin-bottom: 16px; }
.orders-empty h2 { font-size: 20px; color: var(--color-text); margin-bottom: 8px; }
.orders-empty p { margin-bottom: 24px; }
.btn-shop {
  padding: 12px 28px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.orders-list { display: flex; flex-direction: column; gap: 16px; }

.order-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.15s;
}
.order-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.06); }

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
}
.order-id { font-size: 15px; font-weight: 600; color: var(--color-text); }
.order-date { font-size: 13px; color: var(--color-text-muted); margin-left: 12px; }

.order-status {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
}
.status-pending { background: #fef3c7; color: #92400e; }
.status-confirmed { background: #dbeafe; color: #1e40af; }
.status-processing { background: #e0e7ff; color: #4338ca; }
.status-shipped { background: #dcfce7; color: #166534; }
.status-delivered { background: #d1fae5; color: #065f46; }
.status-cancelled { background: #fee2e2; color: #991b1b; }

.order-card-body { padding: 16px 20px; }

.order-preview-items { display: flex; flex-direction: column; gap: 8px; }
.preview-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.preview-img {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}
.preview-img img { width: 100%; height: 100%; object-fit: cover; }
.preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  opacity: 0.5;
}
.preview-name { font-size: 13px; color: var(--color-text); flex: 1; }
.preview-qty { font-size: 12px; color: var(--color-text-muted); }
.more-items { font-size: 12px; color: var(--color-text-muted); font-style: italic; padding-left: 46px; }

.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}
.order-item-count { font-size: 13px; color: var(--color-text-secondary); }
.order-total { font-size: 18px; font-weight: 700; color: var(--color-primary); }
</style>
