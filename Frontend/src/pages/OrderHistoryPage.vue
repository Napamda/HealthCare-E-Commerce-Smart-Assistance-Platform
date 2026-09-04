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

function formatStatus(status) {
  const map = {
    PENDING: 'Pending',
    CONFIRMED: 'Confirmed',
    PROCESSING: 'Processing',
    SHIPPED: 'Shipped',
    DELIVERED: 'Delivered',
    CANCELLED: 'Cancelled',
  }
  return map[status] || status
}

function calculateTotalSpent() {
  return orders.value.reduce((total, order) => {
    if (order.status !== 'CANCELLED') {
      return total + (order.totalAmount || 0)
    }
    return total
  }, 0)
}

function calculateActiveOrders() {
  return orders.value.filter(order =>
    ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED'].includes(order.status)
  ).length
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
      <div class="header-content">
        <h1>My Orders</h1>
        <p class="header-subtitle">Track and manage your healthcare orders</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" @click="store.fetchOrders()" title="Refresh orders">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M23 4v6h-6" />
            <path d="M1 20v-6h6" />
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
          </svg>
          Refresh
        </button>
      </div>
    </div>

    <div v-if="store.loading" class="orders-loading">
      <div class="loading-spinner">
        <svg class="spinner" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
      </div>
      <p>Loading your orders...</p>
    </div>

    <div v-else-if="orders.length === 0" class="orders-empty">
      <div class="empty-illustration">
        <svg width="120" height="120" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="1" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <polyline points="14 2 14 8 20 8" />
          <line x1="16" y1="13" x2="8" y2="13" />
          <line x1="16" y1="17" x2="8" y2="17" />
        </svg>
      </div>
      <h2>No orders yet</h2>
      <p>Start shopping to place your first healthcare order.</p>
      <div class="empty-suggestions">
        <div class="suggestion-item">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="21" r="1" />
            <circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
          </svg>
          <span>Browse products</span>
        </div>
        <div class="suggestion-item">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.42 4.58a5.4 5.4 0 0 0-7.65 0l-.77.78-.77-.78a5.4 5.4 0 0 0-7.65 0C1.46 6.7 1.33 10.28 4 13l8 8 8-8c2.67-2.72 2.54-6.3.42-8.42z" />
          </svg>
          <span>View recommendations</span>
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

    <div v-else class="orders-content">
      <div class="orders-stats">
        <div class="stat-card">
          <span class="stat-value">{{ orders.length }}</span>
          <span class="stat-label">Total Orders</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ formatPrice(calculateTotalSpent()) }}</span>
          <span class="stat-label">Total Spent</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ calculateActiveOrders() }}</span>
          <span class="stat-label">Active Orders</span>
        </div>
      </div>

      <div class="orders-list">
        <div v-for="order in orders" :key="order.id" class="order-card" @click="goToDetail(order.id)">
          <div class="order-card-header">
            <div class="order-info">
              <span class="order-number">#{{ order.orderNumber || order.id }}</span>
              <span class="order-date">{{ formatDate(order.createdAt) }}</span>
            </div>
            <span class="order-status" :class="statusClass(order.status)">
              <span class="status-dot"></span>
              {{ formatStatus(order.status) }}
            </span>
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
              <div class="order-meta">
                <span class="order-item-count">{{ order.itemCount }} item{{ order.itemCount !== 1 ? 's' : '' }}</span>
                <span class="order-payment">{{ order.paymentMethod || 'N/A' }}</span>
              </div>
              <span class="order-total">{{ formatPrice(order.totalAmount) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 80vh;
}

.orders-header {
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
.header-subtitle {
  font-size: 15px;
  color: var(--color-text-muted);
}
.header-actions {
  display: flex;
  gap: 12px;
}
.btn-refresh {
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
.btn-refresh:hover {
  color: var(--color-text);
  border-color: var(--color-text);
  background: var(--color-bg);
}

.orders-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
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

.orders-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  color: var(--color-text-secondary);
}
.empty-illustration {
  color: var(--color-text-muted);
  margin-bottom: 24px;
  opacity: 0.6;
}
.orders-empty h2 {
  font-size: 24px;
  color: var(--color-text);
  margin-bottom: 8px;
}
.orders-empty p {
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

.orders-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.orders-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  text-align: center;
}
.stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 4px;
}
.stat-label {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.15s;
}
.order-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
}
.order-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.order-number {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}
.order-date {
  font-size: 13px;
  color: var(--color-text-muted);
}

.order-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
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
  gap: 12px;
}
.preview-img {
  width: 40px;
  height: 40px;
  border-radius: 8px;
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
.preview-name { font-size: 14px; color: var(--color-text); flex: 1; }
.preview-qty { font-size: 12px; color: var(--color-text-muted); }
.more-items { font-size: 12px; color: var(--color-text-muted); font-style: italic; padding-left: 52px; }

.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--color-border);
}
.order-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.order-item-count { font-size: 13px; color: var(--color-text-secondary); }
.order-payment { font-size: 12px; color: var(--color-text-muted); }
.order-total { font-size: 20px; font-weight: 700; color: var(--color-primary); }

@media (max-width: 768px) {
  .orders-header {
    flex-direction: column;
    align-items: stretch;
  }
  .orders-stats {
    grid-template-columns: 1fr;
  }
  .order-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .order-card-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .empty-suggestions {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
