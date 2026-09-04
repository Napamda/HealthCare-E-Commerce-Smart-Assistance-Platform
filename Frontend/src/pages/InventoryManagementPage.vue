<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useInventoryStore } from '../stores/inventory.js'

const store = useInventoryStore()
const {
  stock,
  loading,
  updating,
  error,
  success,
  historyVisible,
  historyLoading,
  historyProduct,
  history,
  totalProducts,
  totalUnits,
  lowStockCount,
  outOfStockCount,
} = storeToRefs(store)

const searchQuery = ref('')
const stockFilter = ref('all') // all | low | out | reserved
const viewMode = ref('table') // table | grid
const modal = reactive({
  visible: false,
  item: null,
  mode: 'increment', // increment | decrement | set
  quantity: 0,
  note: '',
})
const thresholdModal = reactive({
  visible: false,
  item: null,
  threshold: 0,
})

onMounted(() => {
  store.fetchStock()
})

// ---------------- Filtering ----------------

const filteredStock = computed(() => {
  let rows = stock.value
  if (stockFilter.value === 'low') rows = rows.filter((i) => i.lowStock && !i.outOfStock)
  if (stockFilter.value === 'out') rows = rows.filter((i) => i.outOfStock)
  if (stockFilter.value === 'reserved') rows = rows.filter((i) => i.reservedQuantity > 0)
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    rows = rows.filter(
      (i) =>
        (i.productName || '').toLowerCase().includes(q) ||
        (categoryLabel(i.category) || '').toLowerCase().includes(q),
    )
  }
  return rows
})

const emptyMessage = computed(() => {
  if (stockFilter.value === 'low') return 'No low-stock products — everything looks healthy.'
  if (stockFilter.value === 'out') return 'No out-of-stock products.'
  if (stockFilter.value === 'reserved') return 'No products with reserved stock.'
  return searchQuery.value ? 'No products match your search.' : 'No products in inventory yet.'
})

// ---------------- Status helpers ----------------

function statusOf(item) {
  if (item.outOfStock) return 'out'
  if (item.lowStock) return 'low'
  if (item.reservedQuantity > 0) return 'reserved'
  return 'ok'
}

function statusLabel(item) {
  if (item.outOfStock) return 'Out of stock'
  if (item.lowStock) return 'Low stock'
  if (item.reservedQuantity > 0) return 'Reserved'
  return 'In stock'
}

function statusIcon(item) {
  if (item.outOfStock) return '🚨'
  if (item.lowStock) return '⚠️'
  if (item.reservedQuantity > 0) return '🔒'
  return '✓'
}

function rowClass(item) {
  const s = statusOf(item)
  return s === 'out' ? 'row-out' : s === 'low' ? 'row-low' : s === 'reserved' ? 'row-reserved' : ''
}

function categoryLabel(cat) {
  if (!cat) return ''
  return cat
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

// ---------------- Adjust modal ----------------

const MODE_TITLES = { increment: 'Add stock', decrement: 'Remove stock', set: 'Set stock level' }

const modalTitle = computed(() => MODE_TITLES[modal.mode])

const currentStock = computed(() => Number(modal.item?.stockQuantity) || 0)

const maxQuantity = computed(() => {
  if (modal.mode === 'decrement') return currentStock.value
  if (modal.mode === 'increment') return undefined
  return undefined
})

const previewStock = computed(() => {
  const q = Number(modal.quantity) || 0
  if (modal.mode === 'increment') return currentStock.value + q
  if (modal.mode === 'decrement') return Math.max(0, currentStock.value - q)
  return q
})

const hintText = computed(() => {
  const q = Number(modal.quantity) || 0
  if (modal.mode === 'decrement' && q > currentStock.value) {
    return 'Cannot remove more than the current stock.'
  }
  if (modal.mode === 'increment' && q <= 0) return 'Enter a quantity greater than 0.'
  if (modal.mode === 'decrement' && q <= 0) return 'Enter a quantity greater than 0.'
  return ''
})

const canSubmit = computed(() => {
  const q = Number(modal.quantity)
  if (!Number.isInteger(q)) return false
  if (modal.mode === 'set') return q >= 0
  if (q < 1) return false
  if (modal.mode === 'decrement') return q <= currentStock.value
  return true
})

function openAdjust(item, mode) {
  modal.visible = true
  modal.item = item
  setMode(mode)
}

function setMode(mode) {
  modal.mode = mode
  modal.quantity = 0
  modal.note = ''
}

function closeAdjust() {
  modal.visible = false
  modal.item = null
}

async function submitAdjust() {
  const ok = await store.adjust({
    productId: modal.item.productId,
    mode: modal.mode,
    quantity: Number(modal.quantity),
    note: modal.note.trim() || undefined,
  })
  if (ok) closeAdjust()
}

// ---------------- Threshold modal ----------------

function openThreshold(item) {
  thresholdModal.visible = true
  thresholdModal.item = item
  thresholdModal.threshold = item.lowStockThreshold
}

function closeThreshold() {
  thresholdModal.visible = false
  thresholdModal.item = null
}

async function submitThreshold() {
  const ok = await store.setThreshold({
    productId: thresholdModal.item.productId,
    threshold: Number(thresholdModal.threshold),
  })
  if (ok) closeThreshold()
}

function refresh() {
  store.fetchStock()
}

// ---------------- History helpers ----------------

const TYPE_LABELS = {
  INIT: 'Initial stock',
  PURCHASE: 'Restocked',
  SALE: 'Sale',
  RESERVED: 'Reserved',
  RESERVATION_RELEASED: 'Reservation released',
  RESERVATION_EXPIRED: 'Reservation expired',
  ADJUSTMENT: 'Adjustment',
  CANCELLATION: 'Cancellation',
  REFUND: 'Refund',
}

function typeLabel(t) {
  if (TYPE_LABELS[t]) return TYPE_LABELS[t]
  return String(t || '')
    .replace(/_/g, ' ')
    .toLowerCase()
}

function typeTone(t) {
  if (t === 'PURCHASE' || t === 'INIT') return 'up'
  if (t === 'SALE' || t === 'RESERVED' || t === 'CANCELLATION' || t === 'REFUND') return 'down'
  return 'neutral'
}

function deltaTone(delta) {
  if (delta > 0) return 'delta-up'
  if (delta < 0) return 'delta-down'
  return 'delta-zero'
}

function signedDelta(delta) {
  return (delta > 0 ? '+' : '') + delta
}

function formatDate(iso) {
  if (!iso) return ''
  try {
    return new Date(iso).toLocaleString()
  } catch {
    return iso
  }
}

// ---------------- Stock health indicator ----------------

function stockHealthPercentage(item) {
  if (item.lowStockThreshold === 0) return 100
  const percentage = (item.stockQuantity / item.lowStockThreshold) * 100
  return Math.min(percentage, 100)
}

function stockHealthColor(item) {
  if (item.outOfStock) return 'danger'
  if (item.lowStock) return 'warning'
  return 'success'
}
</script>

<template>
  <div class="inv-page">
    <!-- Header -->
    <div class="inv-header">
      <div class="header-content">
        <h1 class="inv-title">Inventory Management</h1>
        <p class="inv-subtitle">
          Monitor stock levels, manage inventory, track reservations, and review stock movement history.
        </p>
      </div>
      <div class="header-actions">
        <button class="btn btn-secondary" :disabled="loading" @click="refresh">
          <span class="btn-icon">🔄</span>
          {{ loading ? 'Loading…' : 'Refresh' }}
        </button>
      </div>
    </div>

    <!-- Alert banners -->
    <div v-if="error" class="banner banner-error">
      <span class="banner-icon">❌</span>
      <span>{{ error }}</span>
      <button class="banner-close" title="Dismiss" @click="store.clearMessages()">×</button>
    </div>
    <div v-if="success" class="banner banner-success">
      <span class="banner-icon">✓</span>
      <span>{{ success }}</span>
      <button class="banner-close" title="Dismiss" @click="store.clearMessages()">×</button>
    </div>

    <!-- Stats cards -->
    <div class="stat-grid">
      <div class="stat-card stat-primary">
        <div class="stat-icon">📦</div>
        <div class="stat-content">
          <div class="stat-label">Total Products</div>
          <div class="stat-value">{{ totalProducts }}</div>
          <div class="stat-trend">Active inventory items</div>
        </div>
      </div>
      <div class="stat-card stat-success">
        <div class="stat-icon">📊</div>
        <div class="stat-content">
          <div class="stat-label">Units in Stock</div>
          <div class="stat-value">{{ totalUnits }}</div>
          <div class="stat-trend">Total available units</div>
        </div>
      </div>
      <div class="stat-card stat-warning">
        <div class="stat-icon">⚠️</div>
        <div class="stat-content">
          <div class="stat-label">Low Stock</div>
          <div class="stat-value">{{ lowStockCount }}</div>
          <div class="stat-trend">Products below threshold</div>
        </div>
      </div>
      <div class="stat-card stat-danger">
        <div class="stat-icon">🚨</div>
        <div class="stat-content">
          <div class="stat-label">Out of Stock</div>
          <div class="stat-value">{{ outOfStockCount }}</div>
          <div class="stat-trend">Products unavailable</div>
        </div>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="inv-toolbar">
      <div class="toolbar-left">
        <input
          v-model="searchQuery"
          class="inv-search"
          type="search"
          placeholder="Search products or categories…"
        />
        <div class="inv-filters">
          <button
            class="chip"
            :class="{ active: stockFilter === 'all' }"
            @click="stockFilter = 'all'"
          >
            All
          </button>
          <button
            class="chip chip-warn"
            :class="{ active: stockFilter === 'low' }"
            @click="stockFilter = 'low'"
          >
            Low stock
          </button>
          <button
            class="chip chip-danger"
            :class="{ active: stockFilter === 'out' }"
            @click="stockFilter = 'out'"
          >
            Out of stock
          </button>
          <button
            class="chip chip-info"
            :class="{ active: stockFilter === 'reserved' }"
            @click="stockFilter = 'reserved'"
          >
            Reserved
          </button>
        </div>
      </div>
      <div class="toolbar-right">
        <button
          class="view-toggle"
          :class="{ active: viewMode === 'table' }"
          @click="viewMode = 'table'"
          title="Table view"
        >
          <span>📋</span>
        </button>
        <button
          class="view-toggle"
          :class="{ active: viewMode === 'grid' }"
          @click="viewMode = 'grid'"
          title="Grid view"
        >
          <span>⊞</span>
        </button>
      </div>
    </div>

    <!-- Table view -->
    <div v-if="viewMode === 'table'" class="panel">
      <div class="inv-table-wrap">
        <table class="inv-table">
          <thead>
            <tr>
              <th>Product</th>
              <th class="num">Stock</th>
              <th class="num">Threshold</th>
              <th class="num">Reserved</th>
              <th class="num">Available</th>
              <th>Status</th>
              <th class="actions-col">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="inv-message">Loading inventory…</td>
            </tr>
            <tr v-else-if="filteredStock.length === 0">
              <td colspan="7" class="inv-message">{{ emptyMessage }}</td>
            </tr>
            <tr
              v-for="item in filteredStock"
              :key="item.productId"
              :class="rowClass(item)"
            >
              <td>
                <div class="product-info">
                  <span class="status-icon">{{ statusIcon(item) }}</span>
                  <div class="product-details">
                    <div class="product-name">{{ item.productName }}</div>
                    <div class="product-meta">{{ categoryLabel(item.category) }}</div>
                  </div>
                </div>
              </td>
              <td class="num">
                <strong>{{ item.stockQuantity }}</strong>
              </td>
              <td class="num">{{ item.lowStockThreshold }}</td>
              <td class="num">{{ item.reservedQuantity }}</td>
              <td class="num">{{ item.availableQuantity }}</td>
              <td>
                <span class="status-badge" :class="`status-${statusOf(item)}`">
                  {{ statusLabel(item) }}
                </span>
              </td>
              <td class="actions-col">
                <div class="action-row">
                  <button class="act act-inc" title="Add stock" @click="openAdjust(item, 'increment')">
                    + Add
                  </button>
                  <button
                    class="act act-dec"
                    title="Remove stock"
                    :disabled="item.stockQuantity <= 0"
                    @click="openAdjust(item, 'decrement')"
                  >
                    − Remove
                  </button>
                  <button class="act act-set" title="Set stock level" @click="openAdjust(item, 'set')">
                    Set
                  </button>
                  <button class="act act-threshold" title="Set threshold" @click="openThreshold(item)">
                    ⚙️
                  </button>
                  <button class="act act-log" title="View stock history" @click="store.openHistory(item)">
                    History
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Grid view -->
    <div v-else class="grid-container">
      <div v-if="loading" class="grid-message">Loading inventory…</div>
      <div v-else-if="filteredStock.length === 0" class="grid-message">{{ emptyMessage }}</div>
      <div v-else class="grid-items">
        <div
          v-for="item in filteredStock"
          :key="item.productId"
          class="grid-item"
          :class="rowClass(item)"
        >
          <div class="grid-item-header">
            <span class="grid-status-icon">{{ statusIcon(item) }}</span>
            <span class="status-badge" :class="`status-${statusOf(item)}`">
              {{ statusLabel(item) }}
            </span>
          </div>
          <div class="grid-item-body">
            <h3 class="grid-product-name">{{ item.productName }}</h3>
            <p class="grid-product-meta">{{ categoryLabel(item.category) }}</p>
            
            <div class="grid-stats">
              <div class="grid-stat">
                <span class="grid-stat-label">Stock</span>
                <span class="grid-stat-value">{{ item.stockQuantity }}</span>
              </div>
              <div class="grid-stat">
                <span class="grid-stat-label">Threshold</span>
                <span class="grid-stat-value">{{ item.lowStockThreshold }}</span>
              </div>
              <div class="grid-stat">
                <span class="grid-stat-label">Reserved</span>
                <span class="grid-stat-value">{{ item.reservedQuantity }}</span>
              </div>
              <div class="grid-stat">
                <span class="grid-stat-label">Available</span>
                <span class="grid-stat-value">{{ item.availableQuantity }}</span>
              </div>
            </div>

            <div class="grid-health">
              <div class="health-bar">
                <div
                  class="health-fill"
                  :class="`health-${stockHealthColor(item)}`"
                  :style="{ width: `${stockHealthPercentage(item)}%` }"
                ></div>
              </div>
              <span class="health-label">Stock health</span>
            </div>
          </div>
          <div class="grid-item-actions">
            <button class="grid-act grid-act-primary" @click="openAdjust(item, 'increment')">
              + Add Stock
            </button>
            <button class="grid-act" @click="openAdjust(item, 'decrement')" :disabled="item.stockQuantity <= 0">
              − Remove
            </button>
            <button class="grid-act" @click="openThreshold(item)">⚙️ Threshold</button>
            <button class="grid-act" @click="store.openHistory(item)">📜 History</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Adjust stock modal -->
    <div v-if="modal.visible" class="modal-overlay" @click.self="closeAdjust">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">{{ modalTitle }}</h3>
          <button class="modal-x" title="Close" @click="closeAdjust">×</button>
        </div>
        <div class="modal-body">
          <p class="modal-product">{{ modal.item?.productName }}</p>

          <div class="mode-tabs">
            <button
              class="mode-tab"
              :class="{ active: modal.mode === 'increment' }"
              @click="setMode('increment')"
            >
              Add stock
            </button>
            <button
              class="mode-tab"
              :class="{ active: modal.mode === 'decrement' }"
              @click="setMode('decrement')"
            >
              Remove stock
            </button>
            <button class="mode-tab" :class="{ active: modal.mode === 'set' }" @click="setMode('set')">
              Set level
            </button>
          </div>

          <label class="field-label" for="adj-qty">Quantity</label>
          <input
            id="adj-qty"
            v-model.number="modal.quantity"
            class="field-input"
            type="number"
            min="0"
            :max="maxQuantity"
            placeholder="0"
          />
          <p v-if="hintText" class="field-hint">{{ hintText }}</p>

          <label class="field-label" for="adj-note">
            Note <span class="optional">(optional)</span>
          </label>
          <textarea
            id="adj-note"
            v-model="modal.note"
            class="field-input field-area"
            rows="2"
            placeholder="e.g. New shipment received"
          ></textarea>

          <div class="preview">
            <span>Current stock: <strong>{{ currentStock }}</strong></span>
            <span class="preview-arrow">→</span>
            <span>New stock: <strong>{{ previewStock }}</strong></span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="closeAdjust">Cancel</button>
          <button class="btn btn-primary" :disabled="!canSubmit || updating" @click="submitAdjust">
            {{ updating ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Threshold modal -->
    <div v-if="thresholdModal.visible" class="modal-overlay" @click.self="closeThreshold">
      <div class="modal modal-small">
        <div class="modal-header">
          <h3 class="modal-title">Set Low Stock Threshold</h3>
          <button class="modal-x" title="Close" @click="closeThreshold">×</button>
        </div>
        <div class="modal-body">
          <p class="modal-product">{{ thresholdModal.item?.productName }}</p>
          <p class="modal-hint">Products will be marked as "low stock" when quantity falls below this threshold.</p>
          
          <label class="field-label" for="threshold-qty">Threshold</label>
          <input
            id="threshold-qty"
            v-model.number="thresholdModal.threshold"
            class="field-input"
            type="number"
            min="0"
            placeholder="0"
          />
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="closeThreshold">Cancel</button>
          <button class="btn btn-primary" :disabled="updating" @click="submitThreshold">
            {{ updating ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Stock history modal -->
    <div v-if="historyVisible" class="modal-overlay" @click.self="store.closeHistory()">
      <div class="modal modal-wide">
        <div class="modal-header">
          <h3 class="modal-title">Stock history</h3>
          <button class="modal-x" title="Close" @click="store.closeHistory()">×</button>
        </div>
        <div class="modal-body">
          <p class="modal-product">
            {{ historyProduct?.productName }}
            <span class="history-stock">Current stock: {{ historyProduct?.stockQuantity }}</span>
          </p>

          <div v-if="historyLoading" class="inv-message">Loading history…</div>
          <div v-else-if="history.length === 0" class="inv-message">
            No stock changes have been recorded for this product yet.
          </div>
          <ul v-else class="history-list">
            <li v-for="entry in history" :key="entry.id" class="history-item">
              <div class="history-top">
                <span class="h-type" :class="`h-type-${typeTone(entry.changeType)}`">
                  {{ typeLabel(entry.changeType) }}
                </span>
                <span class="h-delta" :class="deltaTone(entry.quantityChange)">
                  {{ signedDelta(entry.quantityChange) }}
                </span>
              </div>
              <div class="history-meta">
                Stock after: <strong>{{ entry.stockAfter }}</strong>
                <template v-if="entry.note"> · {{ entry.note }}</template>
              </div>
              <div class="history-time">{{ formatDate(entry.createdAt) }}</div>
            </li>
          </ul>
        </div>
        <div class="modal-footer">
          <button class="btn btn-primary" @click="store.closeHistory()">Close</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.inv-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

/* Header */
.inv-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.header-content {
  flex: 1;
}

.inv-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--color-text);
}

.inv-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* Buttons */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-icon {
  font-size: 16px;
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-ghost {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
}

.btn-ghost:hover {
  border-color: var(--color-text-muted);
  color: var(--color-text);
}

.btn-secondary {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
}

.btn-secondary:hover:not(:disabled) {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
}

.btn-secondary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* Banners */
.banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
  margin-bottom: 20px;
}

.banner-icon {
  font-size: 18px;
}

.banner-error {
  background: rgba(220, 38, 38, 0.08);
  border: 1px solid rgba(220, 38, 38, 0.3);
  color: var(--color-danger);
}

.banner-success {
  background: rgba(22, 163, 74, 0.08);
  border: 1px solid rgba(22, 163, 74, 0.3);
  color: var(--color-success);
}

.banner-close {
  border: none;
  background: none;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  color: inherit;
  opacity: 0.6;
  padding: 0 4px;
  margin-left: auto;
}

.banner-close:hover {
  opacity: 1;
}

/* Stat cards */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  font-size: 32px;
  line-height: 1;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1;
  margin-bottom: 4px;
}

.stat-trend {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.stat-primary .stat-icon { filter: hue-rotate(210deg); }
.stat-success .stat-icon { filter: hue-rotate(140deg); }
.stat-warning .stat-icon { filter: hue-rotate(30deg); }
.stat-danger .stat-icon { filter: hue-rotate(0deg); }

/* Toolbar */
.inv-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.inv-search {
  flex: 1;
  min-width: 200px;
  max-width: 320px;
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-surface);
  color: var(--color-text);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.inv-search:focus {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.inv-filters {
  display: flex;
  gap: 6px;
}

.chip {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  border-radius: var(--radius-full);
  padding: 7px 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.chip:hover {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
}

.chip.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.chip-warn.active {
  background: var(--color-warning);
  border-color: var(--color-warning);
}

.chip-danger.active {
  background: var(--color-danger);
  border-color: var(--color-danger);
}

.chip-info.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.view-toggle {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  padding: 8px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
  font-size: 16px;
}

.view-toggle:hover {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
}

.view-toggle.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

/* Table */
.panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.inv-table-wrap {
  overflow-x: auto;
}

.inv-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.inv-table th {
  text-align: left;
  padding: 14px 18px;
  background: var(--color-bg);
  color: var(--color-text-muted);
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}

.inv-table td {
  padding: 14px 18px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
}

.inv-table tbody tr:last-child td {
  border-bottom: none;
}

.inv-table tbody tr {
  transition: background 0.12s;
}

.inv-table tbody tr:hover {
  background: rgba(37, 99, 235, 0.03);
}

.inv-table tbody tr.row-low {
  background: rgba(217, 119, 6, 0.05);
}

.inv-table tbody tr.row-out {
  background: rgba(220, 38, 38, 0.05);
}

.inv-table tbody tr.row-reserved {
  background: rgba(37, 99, 235, 0.05);
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-icon {
  font-size: 18px;
  line-height: 1;
}

.product-details {
  flex: 1;
}

.product-name {
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.product-meta {
  font-size: 12px;
  color: var(--color-text-muted);
  text-transform: capitalize;
}

.inv-message {
  text-align: center;
  padding: 48px 20px !important;
  color: var(--color-text-muted);
  font-size: 14px;
}

/* Status badges */
.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.status-ok {
  color: var(--color-success);
  background: rgba(22, 163, 74, 0.1);
}

.status-low {
  color: var(--color-warning);
  background: rgba(217, 119, 6, 0.12);
}

.status-out {
  color: var(--color-danger);
  background: rgba(220, 38, 38, 0.1);
}

.status-reserved {
  color: var(--color-primary);
  background: rgba(37, 99, 235, 0.1);
}

/* Row actions */
.actions-col {
  white-space: nowrap;
}

.action-row {
  display: flex;
  gap: 6px;
  align-items: center;
}

.act {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 12px;
  font-weight: 600;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.15s;
}

.act:hover:not(:disabled) {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
}

.act:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.act-inc:hover:not(:disabled) {
  border-color: var(--color-success);
  color: var(--color-success);
  background: rgba(22, 163, 74, 0.06);
}

.act-dec:hover:not(:disabled) {
  border-color: var(--color-danger);
  color: var(--color-danger);
  background: rgba(220, 38, 38, 0.06);
}

.act-threshold:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(37, 99, 235, 0.06);
}

/* Grid view */
.grid-container {
  display: grid;
  gap: 20px;
}

.grid-message {
  text-align: center;
  padding: 48px 20px;
  color: var(--color-text-muted);
  font-size: 14px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.grid-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.grid-item {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: all 0.2s;
  box-shadow: var(--shadow-sm);
}

.grid-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.grid-item.row-low {
  border-color: var(--color-warning);
  background: rgba(217, 119, 6, 0.03);
}

.grid-item.row-out {
  border-color: var(--color-danger);
  background: rgba(220, 38, 38, 0.03);
}

.grid-item.row-reserved {
  border-color: var(--color-primary);
  background: rgba(37, 99, 235, 0.03);
}

.grid-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.grid-status-icon {
  font-size: 20px;
}

.grid-item-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.grid-product-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--color-text);
}

.grid-product-meta {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
  text-transform: capitalize;
}

.grid-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.grid-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.grid-stat-label {
  font-size: 11px;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.grid-stat-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.grid-health {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.health-bar {
  height: 6px;
  background: var(--color-bg);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.health-fill {
  height: 100%;
  transition: width 0.3s ease;
}

.health-success {
  background: var(--color-success);
}

.health-warning {
  background: var(--color-warning);
}

.health-danger {
  background: var(--color-danger);
}

.health-label {
  font-size: 11px;
  color: var(--color-text-muted);
}

.grid-item-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.grid-act {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 12px;
  font-weight: 600;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
}

.grid-act:hover:not(:disabled) {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
}

.grid-act:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.grid-act-primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.grid-act-primary:hover:not(:disabled) {
  background: var(--color-primary-dark);
}

/* Modals */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 100;
}

.modal {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  width: 480px;
  max-width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 16px 48px rgba(15, 23, 42, 0.2);
  animation: inv-pop 0.2s ease-out;
}

.modal-small {
  width: 400px;
}

.modal-wide {
  width: 640px;
}

@keyframes inv-pop {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid var(--color-border);
}

.modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
}

.modal-x {
  border: none;
  background: none;
  font-size: 22px;
  line-height: 1;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0;
}

.modal-x:hover {
  color: var(--color-text);
}

.modal-body {
  padding: 20px 24px;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--color-border);
}

.modal-product {
  margin: 0 0 16px;
  font-weight: 600;
  color: var(--color-text);
  font-size: 15px;
}

.modal-hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.history-stock {
  display: block;
  font-size: 13px;
  font-weight: 400;
  color: var(--color-text-muted);
  margin-top: 4px;
}

/* Mode tabs */
.mode-tabs {
  display: flex;
  gap: 4px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 4px;
  margin-bottom: 16px;
}

.mode-tab {
  flex: 1;
  border: none;
  background: transparent;
  padding: 8px 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.15s;
}

.mode-tab.active {
  background: var(--color-surface);
  color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

/* Form fields */
.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 14px 0 8px;
}

.field-label .optional {
  font-weight: 400;
  color: var(--color-text-muted);
}

.field-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  background: var(--color-surface);
  color: var(--color-text);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.field-input:focus {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.field-area {
  resize: vertical;
  min-height: 60px;
}

.field-hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--color-danger);
}

.preview {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  padding: 12px 16px;
  background: var(--color-primary-bg);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-secondary);
  flex-wrap: wrap;
}

.preview-arrow {
  color: var(--color-text-muted);
}

/* History list */
.history-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 12px 16px;
}

.history-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.h-type {
  font-size: 13px;
  font-weight: 700;
  padding: 3px 12px;
  border-radius: var(--radius-full);
}

.h-type-up {
  color: var(--color-success);
  background: rgba(22, 163, 74, 0.1);
}

.h-type-down {
  color: var(--color-danger);
  background: rgba(220, 38, 38, 0.1);
}

.h-type-neutral {
  color: var(--color-text-secondary);
  background: var(--color-bg);
}

.h-delta {
  font-size: 15px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.delta-up {
  color: var(--color-success);
}

.delta-down {
  color: var(--color-danger);
}

.delta-zero {
  color: var(--color-text-muted);
}

.history-meta {
  margin-top: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.history-time {
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-muted);
}

/* Responsive */
@media (max-width: 1024px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .grid-items {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .inv-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .inv-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    flex-direction: column;
    align-items: stretch;
  }

  .inv-search {
    max-width: none;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .grid-items {
    grid-template-columns: 1fr;
  }

  .grid-item-actions {
    grid-template-columns: 1fr;
  }
}
</style>
