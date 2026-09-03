<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardStats, getVendorOrders, updateOrderStatus, shipOrder } from '../services/vendor.js'

const router = useRouter()

// ============ State ============
const activeTab = ref('overview') // overview | orders | shipments
const stats = ref(null)
const statsLoading = ref(false)
const statsError = ref('')

const orders = ref([])
const ordersLoading = ref(false)
const ordersError = ref('')
const ordersPage = ref(0)
const ordersTotalPages = ref(0)
const ordersTotalElements = ref(0)
const orderStatusFilter = ref('')

const actionLoading = ref(null) // orderId currently being acted upon
const shipModal = ref({ visible: false, order: null, trackingNumber: '' })

// ============ Computed ============
const ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED']

const statusColor = (status) => {
  const map = {
    PENDING: 's-pending', CONFIRMED: 's-confirmed', PROCESSING: 's-processing',
    SHIPPED: 's-shipped', DELIVERED: 's-delivered', CANCELLED: 's-cancelled',
  }
  return map[status] || ''
}

const statusLabel = (status) => status.charAt(0) + status.slice(1).toLowerCase()

const nextStatus = (current) => {
  const flow = { PENDING: 'CONFIRMED', CONFIRMED: 'PROCESSING', PROCESSING: 'SHIPPED', SHIPPED: 'DELIVERED' }
  return flow[current] || null
}

const nextStatusLabel = (current) => {
  const map = { PENDING: 'Confirm', CONFIRMED: 'Process', PROCESSING: 'Ship', SHIPPED: 'Deliver' }
  return map[current] || ''
}

function formatPrice(val) {
  if (val == null) return '$0.00'
  return '$' + Number(val).toFixed(2)
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

// ============ Data fetching ============
async function fetchStats() {
  statsLoading.value = true
  statsError.value = ''
  try {
    stats.value = await getDashboardStats()
  } catch (e) {
    console.error('Vendor stats error:', e)
    if (e.response?.status === 401) {
      statsError.value = 'Session expired. Please log in again.'
    } else if (e.response?.status === 403) {
      statsError.value = 'Access denied. You need a vendor or admin account.'
    } else {
      statsError.value = e.response?.data?.error || e.response?.data?.message || e.message || 'Failed to load dashboard stats'
    }
  } finally {
    statsLoading.value = false
  }
}

async function fetchOrders(page = 0) {
  ordersLoading.value = true
  ordersError.value = ''
  try {
    const data = await getVendorOrders({
      page,
      size: 15,
      status: orderStatusFilter.value || undefined,
    })
    orders.value = data.content
    ordersPage.value = data.page
    ordersTotalPages.value = data.totalPages
    ordersTotalElements.value = data.totalElements
  } catch (e) {
    console.error('Vendor orders error:', e)
    ordersError.value = e.response?.data?.error || e.response?.data?.message || e.message || 'Failed to load orders'
  } finally {
    ordersLoading.value = false
  }
}

function changePage(dir) {
  const next = ordersPage.value + dir
  if (next >= 0 && next < ordersTotalPages.value) fetchOrders(next)
}

function filterByStatus(status) {
  orderStatusFilter.value = orderStatusFilter.value === status ? '' : status
  fetchOrders(0)
}

// ============ Actions ============
async function doAdvanceStatus(order) {
  const ns = nextStatus(order.status)
  if (!ns) return
  actionLoading.value = order.id
  try {
    const updated = await updateOrderStatus(order.id, ns)
    const idx = orders.value.findIndex(o => o.id === order.id)
    if (idx >= 0) orders.value[idx] = updated
  } catch (e) {
    ordersError.value = e.response?.data?.error || 'Failed to update status'
  } finally {
    actionLoading.value = null
  }
}

function openShipModal(order) {
  shipModal.value = { visible: true, order, trackingNumber: order.trackingNumber || '' }
}

function closeShipModal() {
  shipModal.value = { visible: false, order: null, trackingNumber: '' }
}

async function doShip() {
  const order = shipModal.value.order
  if (!order) return
  actionLoading.value = order.id
  try {
    const updated = await shipOrder(order.id, shipModal.value.trackingNumber || undefined)
    const idx = orders.value.findIndex(o => o.id === order.id)
    if (idx >= 0) orders.value[idx] = updated
    closeShipModal()
  } catch (e) {
    ordersError.value = e.response?.data?.error || 'Failed to ship order'
  } finally {
    actionLoading.value = null
  }
}

async function doCancelOrder(order) {
  if (!confirm(`Cancel order #${order.id}?`)) return
  actionLoading.value = order.id
  try {
    const updated = await updateOrderStatus(order.id, 'CANCELLED')
    const idx = orders.value.findIndex(o => o.id === order.id)
    if (idx >= 0) orders.value[idx] = updated
  } catch (e) {
    ordersError.value = e.response?.data?.error || 'Failed to cancel order'
  } finally {
    actionLoading.value = null
  }
}

// ============ Init ============
onMounted(() => {
  fetchStats()
  fetchOrders()
})
</script>

<template>
  <div class="vd-page">
    <header class="vd-header">
      <div>
        <h1 class="vd-title">Vendor Dashboard</h1>
        <p class="vd-subtitle">Manage orders, shipments, and inventory</p>
      </div>
      <button class="vd-btn vd-btn-secondary" @click="router.push('/inventory')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
        Open Inventory
      </button>
    </header>

    <!-- Tabs -->
    <div class="vd-tabs">
      <button class="vd-tab" :class="{ active: activeTab === 'overview' }" @click="activeTab = 'overview'">Overview</button>
      <button class="vd-tab" :class="{ active: activeTab === 'orders' }" @click="activeTab = 'orders'">Orders</button>
      <button class="vd-tab" :class="{ active: activeTab === 'shipments' }" @click="activeTab = 'shipments'; orderStatusFilter = 'PROCESSING'; fetchOrders(0)">Shipments</button>
    </div>

    <!-- ==================== OVERVIEW ==================== -->
    <template v-if="activeTab === 'overview'">
      <div v-if="statsError" class="vd-banner vd-banner-error">{{ statsError }}</div>

      <div class="vd-stats-grid">
        <div class="vd-stat">
          <div class="vd-stat-icon vd-stat-icon-blue">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
          </div>
          <div class="vd-stat-body">
            <span class="vd-stat-label">Total Products</span>
            <span class="vd-stat-value">{{ statsLoading ? '...' : stats?.totalProducts ?? 0 }}</span>
          </div>
        </div>

        <div class="vd-stat">
          <div class="vd-stat-icon vd-stat-icon-green">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
          </div>
          <div class="vd-stat-body">
            <span class="vd-stat-label">Revenue (Delivered)</span>
            <span class="vd-stat-value">{{ statsLoading ? '...' : formatPrice(stats?.totalRevenue) }}</span>
          </div>
        </div>

        <div class="vd-stat">
          <div class="vd-stat-icon vd-stat-icon-purple">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          </div>
          <div class="vd-stat-body">
            <span class="vd-stat-label">Total Orders</span>
            <span class="vd-stat-value">{{ statsLoading ? '...' : stats?.totalOrders ?? 0 }}</span>
          </div>
        </div>

        <div class="vd-stat">
          <div class="vd-stat-icon vd-stat-icon-orange">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          </div>
          <div class="vd-stat-body">
            <span class="vd-stat-label">Pending Orders</span>
            <span class="vd-stat-value">{{ statsLoading ? '...' : stats?.pendingOrders ?? 0 }}</span>
          </div>
        </div>

        <div class="vd-stat">
          <div class="vd-stat-icon vd-stat-icon-teal">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="23 7 16 12 23 17 23 7"/><rect x="8" y="21" width="8" height="3"/></svg>
          </div>
          <div class="vd-stat-body">
            <span class="vd-stat-label">Shipped Today</span>
            <span class="vd-stat-value">{{ statsLoading ? '...' : stats?.ordersToday ?? 0 }}</span>
          </div>
        </div>

        <div class="vd-stat">
          <div class="vd-stat-icon vd-stat-icon-red">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          </div>
          <div class="vd-stat-body">
            <span class="vd-stat-label">Low / Out of Stock</span>
            <span class="vd-stat-value">
              {{ statsLoading ? '...' : `${stats?.lowStockCount ?? 0} / ${stats?.outOfStockCount ?? 0}` }}
            </span>
          </div>
        </div>
      </div>

      <!-- Order status breakdown -->
      <div class="vd-section">
        <h3 class="vd-section-title">Order Status Breakdown</h3>
        <div class="vd-status-bar" v-if="stats">
          <div class="vd-status-seg s-pending" :style="{ flex: stats.pendingOrders || 0.1 }" :title="`Pending: ${stats.pendingOrders}`">{{ stats.pendingOrders }}</div>
          <div class="vd-status-seg s-confirmed" :style="{ flex: stats.processingOrders || 0.1 }" :title="`Processing: ${stats.processingOrders}`">{{ stats.processingOrders }}</div>
          <div class="vd-status-seg s-shipped" :style="{ flex: stats.shippedOrders || 0.1 }" :title="`Shipped: ${stats.shippedOrders}`">{{ stats.shippedOrders }}</div>
          <div class="vd-status-seg s-delivered" :style="{ flex: stats.deliveredOrders || 0.1 }" :title="`Delivered: ${stats.deliveredOrders}`">{{ stats.deliveredOrders }}</div>
        </div>
        <div class="vd-status-legend">
          <span class="vd-legend"><span class="vd-legend-dot s-pending-bg"></span> Pending</span>
          <span class="vd-legend"><span class="vd-legend-dot s-processing-bg"></span> Processing</span>
          <span class="vd-legend"><span class="vd-legend-dot s-shipped-bg"></span> Shipped</span>
          <span class="vd-legend"><span class="vd-legend-dot s-delivered-bg"></span> Delivered</span>
        </div>
      </div>

      <!-- Quick actions -->
      <div class="vd-section">
        <h3 class="vd-section-title">Quick Actions</h3>
        <div class="vd-actions">
          <button class="vd-action-card" @click="activeTab = 'orders'">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            <span>View All Orders</span>
          </button>
          <button class="vd-action-card" @click="activeTab = 'orders'; orderStatusFilter = 'PENDING'; fetchOrders(0)">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>Pending Orders</span>
          </button>
          <button class="vd-action-card" @click="activeTab = 'shipments'">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="23 7 16 12 23 17 23 7"/><rect x="8" y="21" width="8" height="3"/></svg>
            <span>Manage Shipments</span>
          </button>
          <button class="vd-action-card" @click="router.push('/inventory')">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
            <span>Inventory</span>
          </button>
        </div>
      </div>
    </template>

    <!-- ==================== ORDERS & SHIPMENTS ==================== -->
    <template v-if="activeTab === 'orders' || activeTab === 'shipments'">
      <!-- Status filter chips -->
      <div class="vd-filters">
        <button
          v-for="s in ORDER_STATUSES"
          :key="s"
          class="vd-chip"
          :class="{ active: orderStatusFilter === s }"
          @click="filterByStatus(s)"
        >{{ statusLabel(s) }}</button>
      </div>

      <div v-if="ordersError" class="vd-banner vd-banner-error">{{ ordersError }}</div>

      <div class="vd-panel">
        <div class="vd-table-wrap">
          <table class="vd-table">
            <thead>
              <tr>
                <th>Order #</th>
                <th>Customer</th>
                <th>Items</th>
                <th class="num">Total</th>
                <th>Status</th>
                <th>Tracking</th>
                <th>Date</th>
                <th class="actions-col">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="ordersLoading">
                <td colspan="8" class="vd-empty">Loading orders...</td>
              </tr>
              <tr v-else-if="orders.length === 0">
                <td colspan="8" class="vd-empty">No orders found.</td>
              </tr>
              <tr v-for="order in orders" :key="order.id">
                <td class="vd-order-num">{{ order.orderNumber || `#${order.id}` }}</td>
                <td>
                  <div class="vd-customer">{{ order.userName || order.userEmail }}</div>
                  <div class="vd-customer-meta">{{ order.shippingCity }}</div>
                </td>
                <td>
                  <div class="vd-items-list">
                    <span v-for="item in order.items" :key="item.id" class="vd-item-chip">
                      {{ item.productName }} ×{{ item.quantity }}
                    </span>
                  </div>
                </td>
                <td class="num vd-total">{{ formatPrice(order.totalAmount) }}</td>
                <td>
                  <span class="vd-status" :class="statusColor(order.status)">{{ statusLabel(order.status) }}</span>
                </td>
                <td>
                  <span v-if="order.trackingNumber" class="vd-tracking">{{ order.trackingNumber }}</span>
                  <span v-else class="vd-tracking-none">—</span>
                </td>
                <td class="vd-date">{{ formatDate(order.createdAt) }}</td>
                <td class="actions-col">
                  <div class="vd-actions-row">
                    <button
                      v-if="nextStatus(order.status) && order.status !== 'PROCESSING'"
                      class="vd-act vd-act-primary"
                      :disabled="actionLoading === order.id"
                      @click="doAdvanceStatus(order)"
                    >{{ actionLoading === order.id ? '...' : nextStatusLabel(order.status) }}</button>
                    <button
                      v-if="order.status === 'PROCESSING' || order.status === 'CONFIRMED'"
                      class="vd-act vd-act-ship"
                      :disabled="actionLoading === order.id"
                      @click="openShipModal(order)"
                    >{{ actionLoading === order.id ? '...' : 'Ship' }}</button>
                    <button
                      v-if="order.status !== 'CANCELLED' && order.status !== 'DELIVERED'"
                      class="vd-act vd-act-danger"
                      :disabled="actionLoading === order.id"
                      @click="doCancelOrder(order)"
                    >Cancel</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="ordersTotalPages > 1" class="vd-pagination">
          <button :disabled="ordersPage <= 0" @click="changePage(-1)">Previous</button>
          <span class="vd-page-info">Page {{ ordersPage + 1 }} of {{ ordersTotalPages }} ({{ ordersTotalElements }} total)</span>
          <button :disabled="ordersPage >= ordersTotalPages - 1" @click="changePage(1)">Next</button>
        </div>
      </div>
    </template>

    <!-- ==================== SHIP MODAL ==================== -->
    <div v-if="shipModal.visible" class="vd-modal-overlay" @click.self="closeShipModal">
      <div class="vd-modal">
        <div class="vd-modal-header">
          <h3>Ship Order</h3>
          <button class="vd-modal-x" @click="closeShipModal">&times;</button>
        </div>
        <div class="vd-modal-body">
          <p class="vd-modal-order">Order #{{ shipModal.order?.id }} — {{ shipModal.order?.userName || shipModal.order?.userEmail }}</p>
          <label class="vd-field-label">Tracking Number <span class="vd-optional">(optional)</span></label>
          <input
            v-model="shipModal.trackingNumber"
            class="vd-field-input"
            type="text"
            placeholder="e.g. 1Z999AA10123456784"
          />
        </div>
        <div class="vd-modal-footer">
          <button class="vd-btn vd-btn-ghost" @click="closeShipModal">Cancel</button>
          <button class="vd-btn vd-btn-primary" :disabled="actionLoading === shipModal.order?.id" @click="doShip">
            {{ actionLoading === shipModal.order?.id ? 'Shipping...' : 'Mark as Shipped' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vd-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}
.vd-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.vd-title { font-size: 26px; font-weight: 700; color: var(--color-text); margin: 0 0 4px; }
.vd-subtitle { margin: 0; font-size: 13px; color: var(--color-text-secondary); }

/* Tabs */
.vd-tabs {
  display: flex;
  gap: 2px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 4px;
  margin-bottom: 24px;
  width: fit-content;
}
.vd-tab {
  padding: 8px 20px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.15s;
}
.vd-tab.active { background: var(--color-surface); color: var(--color-primary); box-shadow: var(--shadow-sm); }

/* Stat cards */
.vd-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}
@media (max-width: 860px) { .vd-stats-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 520px) { .vd-stats-grid { grid-template-columns: 1fr; } }

.vd-stat {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition: box-shadow 0.15s;
}
.vd-stat:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.vd-stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.vd-stat-icon-blue { background: #eff6ff; color: #2563eb; }
.vd-stat-icon-green { background: #f0fdf4; color: #16a34a; }
.vd-stat-icon-purple { background: #faf5ff; color: #9333ea; }
.vd-stat-icon-orange { background: #fff7ed; color: #ea580c; }
.vd-stat-icon-teal { background: #f0fdfa; color: #0d9488; }
.vd-stat-icon-red { background: #fef2f2; color: #dc2626; }
.vd-stat-body { display: flex; flex-direction: column; gap: 2px; }
.vd-stat-label { font-size: 12px; color: var(--color-text-muted); }
.vd-stat-value { font-size: 22px; font-weight: 700; color: var(--color-text); }

/* Sections */
.vd-section { margin-bottom: 24px; }
.vd-section-title { font-size: 16px; font-weight: 600; color: var(--color-text); margin: 0 0 12px; }

/* Status bar */
.vd-status-bar {
  display: flex;
  height: 28px;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
}
.vd-status-seg {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  min-width: 28px;
  transition: flex 0.3s;
}
.s-pending, .s-pending-bg { background: #f59e0b; }
.s-confirmed, .s-processing-bg { background: #3b82f6; }
.s-processing, .s-processing-bg { background: #8b5cf6; }
.s-shipped, .s-shipped-bg { background: #06b6d4; }
.s-delivered, .s-delivered-bg { background: #10b981; }
.s-cancelled { background: #ef4444; }
.vd-status-legend {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.vd-legend { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--color-text-secondary); }
.vd-legend-dot { width: 10px; height: 10px; border-radius: 50%; }

/* Quick actions */
.vd-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
@media (max-width: 600px) { .vd-actions { grid-template-columns: repeat(2, 1fr); } }
.vd-action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  cursor: pointer;
  transition: all 0.15s;
}
.vd-action-card:hover { border-color: var(--color-primary); color: var(--color-primary); background: #eff6ff; }

/* Filters */
.vd-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}
.vd-chip {
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.vd-chip:hover { border-color: var(--color-primary); color: var(--color-primary); }
.vd-chip.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }

/* Table */
.vd-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.vd-table-wrap { overflow-x: auto; }
.vd-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.vd-table th {
  text-align: left;
  padding: 12px 14px;
  background: var(--color-bg);
  color: var(--color-text-muted);
  font-weight: 600;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}
.vd-table td {
  padding: 12px 14px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
}
.vd-table tbody tr:last-child td { border-bottom: none; }
.vd-table tbody tr:hover { background: rgba(37, 99, 235, 0.02); }
.vd-table .num { text-align: right; font-variant-numeric: tabular-nums; }
.vd-empty { text-align: center; padding: 40px 16px !important; color: var(--color-text-muted); }

.vd-order-num { font-weight: 600; color: var(--color-primary); white-space: nowrap; }
.vd-customer { font-weight: 600; color: var(--color-text); }
.vd-customer-meta { font-size: 11px; color: var(--color-text-muted); }
.vd-items-list { display: flex; flex-wrap: wrap; gap: 4px; }
.vd-item-chip {
  padding: 2px 8px;
  font-size: 11px;
  background: var(--color-bg);
  border-radius: 10px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}
.vd-total { font-weight: 700; color: var(--color-text); }
.vd-date { font-size: 12px; color: var(--color-text-muted); white-space: nowrap; }
.vd-tracking { font-size: 12px; font-weight: 600; color: var(--color-primary); font-family: monospace; }
.vd-tracking-none { font-size: 12px; color: var(--color-text-muted); }

/* Status badges */
.vd-status {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.s-pending { color: #d97706; background: #fef3c7; }
.s-confirmed { color: #2563eb; background: #dbeafe; }
.s-processing { color: #7c3aed; background: #ede9fe; }
.s-shipped { color: #0891b2; background: #cffafe; }
.s-delivered { color: #059669; background: #d1fae5; }
.s-cancelled { color: #dc2626; background: #fef2f2; }

/* Actions row */
.actions-col { white-space: nowrap; }
.vd-actions-row { display: flex; gap: 5px; }
.vd-act {
  padding: 5px 10px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.vd-act:hover:not(:disabled) { border-color: var(--color-primary); color: var(--color-primary); }
.vd-act:disabled { opacity: 0.5; cursor: default; }
.vd-act-primary { background: #eff6ff; border-color: #bfdbfe; color: #2563eb; }
.vd-act-primary:hover:not(:disabled) { background: #dbeafe; }
.vd-act-ship { background: #f0fdfa; border-color: #99f6e4; color: #0d9488; }
.vd-act-ship:hover:not(:disabled) { background: #ccfbf1; }
.vd-act-danger { color: #dc2626; }
.vd-act-danger:hover:not(:disabled) { background: #fef2f2; border-color: #fecaca; }

/* Pagination */
.vd-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 14px;
  border-top: 1px solid var(--color-border);
}
.vd-pagination button {
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.vd-pagination button:hover:not(:disabled) { border-color: var(--color-primary); color: var(--color-primary); }
.vd-pagination button:disabled { opacity: 0.4; cursor: default; }
.vd-page-info { font-size: 12px; color: var(--color-text-muted); }

/* Banner */
.vd-banner { padding: 10px 14px; border-radius: var(--radius-md); font-size: 13px; margin-bottom: 16px; }
.vd-banner-error { background: #fef2f2; border: 1px solid #fecaca; color: #dc2626; }

/* Buttons */
.vd-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 16px;
  font-size: 13px;
  font-weight: 600;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
}
.vd-btn-primary { background: var(--color-primary); color: #fff; }
.vd-btn-primary:hover:not(:disabled) { background: #1d4ed8; }
.vd-btn-primary:disabled { opacity: 0.5; cursor: default; }
.vd-btn-secondary { background: var(--color-surface); border: 1px solid var(--color-border); color: var(--color-text-secondary); }
.vd-btn-secondary:hover { border-color: var(--color-primary); color: var(--color-primary); }
.vd-btn-ghost { background: transparent; border: 1px solid var(--color-border); color: var(--color-text-secondary); }
.vd-btn-ghost:hover { border-color: var(--color-text-muted); color: var(--color-text); }

/* Modal */
.vd-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 100;
}
.vd-modal {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  width: 440px;
  max-width: 100%;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.18);
  animation: vd-pop 0.16s ease-out;
}
@keyframes vd-pop { from { transform: scale(0.97); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.vd-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}
.vd-modal-header h3 { margin: 0; font-size: 16px; font-weight: 700; }
.vd-modal-x { border: none; background: none; font-size: 22px; color: var(--color-text-muted); cursor: pointer; }
.vd-modal-x:hover { color: var(--color-text); }
.vd-modal-body { padding: 18px 20px; }
.vd-modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px; border-top: 1px solid var(--color-border); }
.vd-modal-order { margin: 0 0 14px; font-weight: 600; color: var(--color-text); }
.vd-field-label { display: block; font-size: 12px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 6px; }
.vd-optional { font-weight: 400; color: var(--color-text-muted); }
.vd-field-input {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  background: var(--color-surface);
  color: var(--color-text);
  outline: none;
}
.vd-field-input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12); }
</style>