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
const stockFilter = ref('all') // all | low | out
const modal = reactive({
  visible: false,
  item: null,
  mode: 'increment', // increment | decrement | set
  quantity: 0,
  note: '',
})

onMounted(() => {
  store.fetchStock()
})

// ---------------- Filtering ----------------

const filteredStock = computed(() => {
  let rows = stock.value
  if (stockFilter.value === 'low') rows = rows.filter((i) => i.lowStock && !i.outOfStock)
  if (stockFilter.value === 'out') rows = rows.filter((i) => i.outOfStock)
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
  return searchQuery.value ? 'No products match your search.' : 'No products in inventory yet.'
})

// ---------------- Status helpers ----------------

function statusOf(item) {
  if (item.outOfStock) return 'out'
  if (item.lowStock) return 'low'
  return 'ok'
}

function statusLabel(item) {
  if (item.outOfStock) return 'Out of stock'
  if (item.lowStock) return 'Low stock'
  return 'In stock'
}

function rowClass(item) {
  const s = statusOf(item)
  return s === 'out' ? 'row-out' : s === 'low' ? 'row-low' : ''
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
</script>

<template>
  <div class="inv-page">
    <div class="inv-header">
      <div>
        <h1 class="inv-title">Vendor Inventory</h1>
        <p class="inv-subtitle">
          Monitor stock levels, top up products, and review stock movement history.
        </p>
      </div>
      <button class="btn btn-secondary" :disabled="loading" @click="refresh">
        {{ loading ? 'Loading…' : 'Refresh' }}
      </button>
    </div>

    <div v-if="error" class="banner banner-error">
      <span>{{ error }}</span>
      <button class="banner-close" title="Dismiss" @click="store.clearMessages()">×</button>
    </div>
    <div v-if="success" class="banner banner-success">
      <span>{{ success }}</span>
      <button class="banner-close" title="Dismiss" @click="store.clearMessages()">×</button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <span class="stat-label">Total products</span>
        <span class="stat-value">{{ totalProducts }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">Units in stock</span>
        <span class="stat-value">{{ totalUnits }}</span>
      </div>
      <div class="stat-card stat-warn">
        <span class="stat-label">Low stock</span>
        <span class="stat-value">{{ lowStockCount }}</span>
      </div>
      <div class="stat-card stat-danger">
        <span class="stat-label">Out of stock</span>
        <span class="stat-value">{{ outOfStockCount }}</span>
      </div>
    </div>

    <div class="inv-toolbar">
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
      </div>
    </div>

    <div class="panel">
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
                <div class="product-name">{{ item.productName }}</div>
                <div class="product-meta">{{ categoryLabel(item.category) }}</div>
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
  max-width: 1120px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

.inv-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.inv-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--color-text);
}

.inv-subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* ---------------- Banners ---------------- */
.banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  margin-bottom: 16px;
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
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  color: inherit;
  opacity: 0.6;
  padding: 0 2px;
}

.banner-close:hover {
  opacity: 1;
}

/* ---------------- Stat cards ---------------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: var(--shadow-sm);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1;
}

.stat-card.stat-warn .stat-value {
  color: var(--color-warning);
}

.stat-card.stat-danger .stat-value {
  color: var(--color-danger);
}

/* ---------------- Toolbar ---------------- */
.inv-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.inv-search {
  flex: 1;
  min-width: 220px;
  padding: 9px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
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
  font-size: 12px;
  font-weight: 500;
  border-radius: var(--radius-full);
  padding: 6px 14px;
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

/* ---------------- Table ---------------- */
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
  font-size: 13px;
}

.inv-table th {
  text-align: left;
  padding: 12px 16px;
  background: var(--color-bg);
  color: var(--color-text-muted);
  font-weight: 600;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}

.inv-table td {
  padding: 12px 16px;
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

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.product-name {
  font-weight: 600;
  color: var(--color-text);
}

.product-meta {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
  text-transform: capitalize;
}

.inv-message {
  text-align: center;
  padding: 36px 16px !important;
  color: var(--color-text-muted);
  font-size: 13px;
}

/* Status badges */
.status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11px;
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
  font-size: 11px;
  font-weight: 600;
  padding: 5px 9px;
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

/* ---------------- Modals ---------------- */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 100;
}

.modal {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  width: 440px;
  max-width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.18);
  animation: inv-pop 0.16s ease-out;
}

.modal-wide {
  width: 620px;
}

@keyframes inv-pop {
  from {
    transform: scale(0.97);
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
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}

.modal-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
}

.modal-x {
  border: none;
  background: none;
  font-size: 20px;
  line-height: 1;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0;
}

.modal-x:hover {
  color: var(--color-text);
}

.modal-body {
  padding: 18px 20px;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--color-border);
}

.modal-product {
  margin: 0 0 14px;
  font-weight: 600;
  color: var(--color-text);
}

.history-stock {
  display: block;
  font-size: 12px;
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
  margin-bottom: 14px;
}

.mode-tab {
  flex: 1;
  border: none;
  background: transparent;
  padding: 7px 6px;
  font-size: 12px;
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
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 12px 0 6px;
}

.field-label .optional {
  font-weight: 400;
  color: var(--color-text-muted);
}

.field-input {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
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
  min-height: 52px;
}

.field-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--color-danger);
}

.preview {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 10px 12px;
  background: var(--color-primary-bg);
  border-radius: var(--radius-md);
  font-size: 13px;
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
  gap: 8px;
}

.history-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}

.history-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.h-type {
  font-size: 12px;
  font-weight: 700;
  padding: 2px 10px;
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
  font-size: 14px;
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
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.history-time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--color-text-muted);
}

/* ---------------- Buttons ---------------- */
.btn {
  border: none;
  border-radius: var(--radius-md);
  padding: 9px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-dark);
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

/* ---------------- Responsive ---------------- */
@media (max-width: 760px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .inv-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
