<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useInventoryStore } from '../stores/inventory.js'
import { useProductStore } from '../stores/product.js'
import ProductForm from '../components/product/ProductForm.vue'

const route = useRoute()
const router = useRouter()
const store = useInventoryStore()
const productStore = useProductStore()
const {
  stock,
  loading,
  reservationsLoading,
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
  reservedCount,
  activeReservations,
  categoryBreakdown,
} = storeToRefs(store)

const { categories: productCategories } = storeToRefs(productStore)

const searchQuery = ref('')
const stockFilter = ref('all')
const categoryFilter = ref('all')
const sortKey = ref('name')
const actionMenuId = ref(null)
const selectedIds = ref([])
const focusedId = ref(null)
const showProductForm = ref(false)
const editingProduct = ref(null)
const now = ref(Date.now())
let tickTimer = null

const modal = reactive({
  visible: false,
  item: null,
  mode: 'increment',
  quantity: 0,
  note: '',
})

const thresholdModal = reactive({
  visible: false,
  item: null,
  threshold: 0,
})

const MODE_TITLES = {
  increment: 'Add stock',
  decrement: 'Remove stock',
  set: 'Set stock level',
}

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

const QUICK_AMOUNTS = [5, 10, 25, 50]

onMounted(async () => {
  applyRouteState()
  await Promise.all([store.refreshAll(), productStore.fetchCategories()])
  tickTimer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
  await nextTick()
  scrollToFocused()
})

onUnmounted(() => {
  if (tickTimer) clearInterval(tickTimer)
})

watch(
  () => [route.query.filter, route.query.focus],
  async () => {
    applyRouteState()
    await nextTick()
    scrollToFocused()
  },
)

function applyRouteState() {
  const allowed = ['all', 'low', 'out', 'reserved']
  const next = String(route.query.filter || 'all')
  stockFilter.value = allowed.includes(next) ? next : 'all'
  focusedId.value = route.query.focus ? Number(route.query.focus) : null
}

function setFilter(filter) {
  stockFilter.value = filter
  actionMenuId.value = null
  router.replace({
    path: '/inventory',
    query: {
      ...(filter === 'all' ? {} : { filter }),
      ...(focusedId.value ? { focus: focusedId.value } : {}),
    },
  })
}

const categories = computed(() =>
  categoryBreakdown.value.map((row) => row.category),
)

const filteredStock = computed(() => {
  let rows = stock.value.slice()
  if (stockFilter.value === 'low') rows = rows.filter((i) => i.lowStock && !i.outOfStock)
  if (stockFilter.value === 'out') rows = rows.filter((i) => i.outOfStock)
  if (stockFilter.value === 'reserved') rows = rows.filter((i) => i.reservedQuantity > 0)
  if (categoryFilter.value !== 'all') {
    rows = rows.filter((i) => i.category === categoryFilter.value)
  }
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    rows = rows.filter(
      (i) =>
        (i.productName || '').toLowerCase().includes(q) ||
        (categoryLabel(i.category) || '').toLowerCase().includes(q),
    )
  }

  rows.sort((a, b) => {
    if (sortKey.value === 'stock') return (b.stockQuantity || 0) - (a.stockQuantity || 0)
    if (sortKey.value === 'available') return (b.availableQuantity || 0) - (a.availableQuantity || 0)
    if (sortKey.value === 'status') {
      const rank = (item) => (item.outOfStock ? 0 : item.lowStock ? 1 : item.reservedQuantity > 0 ? 2 : 3)
      return rank(a) - rank(b) || (a.productName || '').localeCompare(b.productName || '')
    }
    return (a.productName || '').localeCompare(b.productName || '')
  })
  return rows
})

const allVisibleSelected = computed(
  () =>
    filteredStock.value.length > 0 &&
    filteredStock.value.every((item) => selectedIds.value.includes(item.productId)),
)

const selectedItems = computed(() =>
  stock.value.filter((item) => selectedIds.value.includes(item.productId)),
)

const emptyMessage = computed(() => {
  if (stockFilter.value === 'low') return 'No low-stock products — everything looks healthy.'
  if (stockFilter.value === 'out') return 'No out-of-stock products.'
  if (stockFilter.value === 'reserved') return 'No products with reserved stock.'
  return searchQuery.value ? 'No products match your search.' : 'No products in inventory yet.'
})

const productNameById = computed(() => {
  const map = {}
  for (const item of stock.value) map[item.productId] = item.productName
  return map
})

const suggestedQty = computed(() =>
  modal.item ? store.suggestedRestockQty(modal.item) : 10,
)

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

function rowClass(item) {
  const classes = []
  const s = statusOf(item)
  if (s === 'out') classes.push('row-out')
  if (s === 'low') classes.push('row-low')
  if (s === 'reserved') classes.push('row-reserved')
  if (focusedId.value === item.productId) classes.push('row-focused')
  if (selectedIds.value.includes(item.productId)) classes.push('row-selected')
  return classes
}

function categoryLabel(cat) {
  if (!cat) return ''
  return cat
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

const modalTitle = computed(() => MODE_TITLES[modal.mode])
const currentStock = computed(() => Number(modal.item?.stockQuantity) || 0)
const maxQuantity = computed(() => (modal.mode === 'decrement' ? currentStock.value : undefined))

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
  if ((modal.mode === 'increment' || modal.mode === 'decrement') && q <= 0) {
    return 'Enter a quantity greater than 0.'
  }
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
  actionMenuId.value = null
  modal.visible = true
  modal.item = item
  setMode(mode)
  if (mode === 'increment') {
    modal.quantity = store.suggestedRestockQty(item)
    modal.note = 'Restock'
  }
}

function setMode(mode) {
  modal.mode = mode
  modal.quantity = mode === 'increment' && modal.item ? store.suggestedRestockQty(modal.item) : 0
  modal.note = mode === 'increment' ? 'Restock' : ''
}

function applyQuickAmount(amount) {
  if (modal.mode === 'set') {
    modal.quantity = amount
    return
  }
  modal.quantity = amount
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

function openThreshold(item) {
  actionMenuId.value = null
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

function toggleMenu(id) {
  actionMenuId.value = actionMenuId.value === id ? null : id
}

function openHistory(item) {
  actionMenuId.value = null
  store.openHistory(item)
}

function toggleSelect(id) {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter((x) => x !== id)
  } else {
    selectedIds.value = [...selectedIds.value, id]
  }
}

function toggleSelectAll() {
  if (allVisibleSelected.value) {
    const visible = new Set(filteredStock.value.map((i) => i.productId))
    selectedIds.value = selectedIds.value.filter((id) => !visible.has(id))
  } else {
    const ids = new Set([...selectedIds.value, ...filteredStock.value.map((i) => i.productId)])
    selectedIds.value = [...ids]
  }
}

function clearSelection() {
  selectedIds.value = []
}

async function bulkRestockSelected() {
  const targets = selectedItems.value
  if (!targets.length) return
  const ok = await store.bulkRestock(targets, 'Bulk restock from inventory page')
  if (ok) clearSelection()
}

async function restockAllLow() {
  const targets = stock.value.filter((i) => i.lowStock || i.outOfStock)
  if (!targets.length) return
  await store.bulkRestock(targets, 'Restock all low / out of stock')
}

async function quickRowRestock(item) {
  actionMenuId.value = null
  await store.restockToSafeLevel(item)
}

function exportCsv() {
  const headers = [
    'Product',
    'Category',
    'Stock',
    'Threshold',
    'Reserved',
    'Available',
    'Status',
  ]
  const rows = filteredStock.value.map((item) => [
    item.productName,
    categoryLabel(item.category),
    item.stockQuantity,
    item.lowStockThreshold,
    item.reservedQuantity,
    item.availableQuantity,
    statusLabel(item),
  ])
  const csv = [headers, ...rows]
    .map((row) =>
      row
        .map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`)
        .join(','),
    )
    .join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `inventory-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function scrollToFocused() {
  if (!focusedId.value) return
  const el = document.getElementById(`inv-row-${focusedId.value}`)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function typeLabel(t) {
  if (TYPE_LABELS[t]) return TYPE_LABELS[t]
  return String(t || '')
    .replace(/_/g, ' ')
    .toLowerCase()
}

function typeTone(t) {
  if (t === 'PURCHASE' || t === 'INIT' || t === 'RESERVATION_RELEASED' || t === 'RESERVATION_EXPIRED') {
    return 'up'
  }
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

function reservationStatus(r) {
  return String(r.status || '').toUpperCase()
}

function countdown(expiresAt) {
  if (!expiresAt) return '—'
  const ms = new Date(expiresAt).getTime() - now.value
  if (ms <= 0) return 'Expired'
  const totalSec = Math.floor(ms / 1000)
  const m = Math.floor(totalSec / 60)
  const s = totalSec % 60
  return `${m}m ${String(s).padStart(2, '0')}s`
}

function isExpiringSoon(r) {
  if (!r.expiresAt || reservationStatus(r) !== 'ACTIVE') return false
  const ms = new Date(r.expiresAt).getTime() - now.value
  return ms > 0 && ms < 10 * 60 * 1000
}

function stockHealthPercentage(item) {
  if (!item.lowStockThreshold) return item.stockQuantity > 0 ? 100 : 0
  return Math.min(100, (item.stockQuantity / item.lowStockThreshold) * 100)
}

function openNewProduct() {
  editingProduct.value = null
  showProductForm.value = true
}

function closeProductForm() {
  showProductForm.value = false
  editingProduct.value = null
}

async function onProductSave(payload) {
  try {
    store.clearMessages()
    if (editingProduct.value?.id) {
      await productStore.editProduct(editingProduct.value.id, payload)
      success.value = `Updated "${payload.name}"`
      closeProductForm()
    } else {
      const created = await productStore.addProduct(payload)
      success.value = `Created "${created.name}". You can upload images now.`
      editingProduct.value = created
    }
    await store.fetchStock()
  } catch (e) {
    error.value =
      e?.response?.data?.message ||
      e?.response?.data?.error ||
      productStore.error ||
      'Failed to save product'
  }
}

async function onProductImagesChanged() {
  await store.fetchStock()
}

function stockHealthColor(item) {
  if (item.outOfStock) return 'danger'
  if (item.lowStock) return 'warning'
  return 'success'
}
</script>

<template>
  <div class="page-wrap inv-page" @click="actionMenuId = null">
    <header class="inv-header">
      <div>
        <button class="btn-link inv-back" type="button" @click="$router.push('/vendor')">
          ← Vendor workspace
        </button>
        <h1 class="page-title">Inventory</h1>
        <p class="inv-subtitle">
          Search, sort, restock, and track reserved units across your catalog.
        </p>
      </div>
      <div class="inv-header-actions">
        <button class="btn btn-primary" type="button" @click="openNewProduct">
          + Add product
        </button>
        <button
          class="btn btn-secondary"
          type="button"
          :disabled="updating || !(lowStockCount + outOfStockCount)"
          @click="restockAllLow"
        >
          Restock all low
        </button>
        <button class="btn btn-secondary" type="button" @click="exportCsv">Export CSV</button>
        <button class="btn btn-secondary" type="button" :disabled="loading" @click="store.refreshAll()">
          {{ loading ? 'Refreshing…' : 'Refresh' }}
        </button>
      </div>
    </header>

    <div v-if="error" class="alert alert-error inv-alert">
      <span>{{ error }}</span>
      <button class="banner-close" type="button" title="Dismiss" @click="store.clearMessages()">×</button>
    </div>
    <div v-if="success" class="alert alert-success inv-alert">
      <span>{{ success }}</span>
      <button class="banner-close" type="button" title="Dismiss" @click="store.clearMessages()">×</button>
    </div>

    <section class="inv-stats" aria-label="Inventory summary">
      <button class="inv-stat" type="button" :class="{ active: stockFilter === 'all' }" @click="setFilter('all')">
        <span class="inv-stat-label">Products</span>
        <span class="inv-stat-value">{{ totalProducts }}</span>
      </button>
      <button class="inv-stat" type="button" @click="setFilter('all')">
        <span class="inv-stat-label">Units</span>
        <span class="inv-stat-value">{{ totalUnits }}</span>
      </button>
      <button
        class="inv-stat tone-warn"
        type="button"
        :class="{ active: stockFilter === 'low' }"
        @click="setFilter('low')"
      >
        <span class="inv-stat-label">Low stock</span>
        <span class="inv-stat-value">{{ lowStockCount }}</span>
      </button>
      <button
        class="inv-stat tone-danger"
        type="button"
        :class="{ active: stockFilter === 'out' }"
        @click="setFilter('out')"
      >
        <span class="inv-stat-label">Out of stock</span>
        <span class="inv-stat-value">{{ outOfStockCount }}</span>
      </button>
      <button
        class="inv-stat tone-info"
        type="button"
        :class="{ active: stockFilter === 'reserved' }"
        @click="setFilter('reserved')"
      >
        <span class="inv-stat-label">Reserved SKUs</span>
        <span class="inv-stat-value">{{ reservedCount }}</span>
      </button>
    </section>

    <div class="inv-toolbar">
      <div class="inv-search-wrap">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
        </svg>
        <input
          v-model="searchQuery"
          class="inv-search"
          type="search"
          placeholder="Search products or categories…"
        />
      </div>
      <div class="inv-toolbar-controls">
        <select v-model="categoryFilter" class="inv-select">
          <option value="all">All categories</option>
          <option v-for="cat in categories" :key="cat" :value="cat">
            {{ categoryLabel(cat) }}
          </option>
        </select>
        <select v-model="sortKey" class="inv-select">
          <option value="name">Sort: Name</option>
          <option value="stock">Sort: Stock</option>
          <option value="available">Sort: Available</option>
          <option value="status">Sort: Status</option>
        </select>
      </div>
    </div>

    <div class="filter-row inv-filters">
      <button
        v-for="chip in [
          { id: 'all', label: 'All' },
          { id: 'low', label: 'Low stock' },
          { id: 'out', label: 'Out of stock' },
          { id: 'reserved', label: 'Reserved' },
        ]"
        :key="chip.id"
        type="button"
        class="filter-chip"
        :class="{ active: stockFilter === chip.id }"
        @click="setFilter(chip.id)"
      >
        {{ chip.label }}
      </button>
      <span class="inv-result-count">{{ filteredStock.length }} shown</span>
    </div>

    <div v-if="selectedIds.length" class="inv-bulk-bar">
      <span>{{ selectedIds.length }} selected</span>
      <div class="inv-bulk-actions">
        <button class="btn btn-primary" type="button" :disabled="updating" @click="bulkRestockSelected">
          {{ updating ? 'Restocking…' : 'Restock selected' }}
        </button>
        <button class="btn-link" type="button" @click="clearSelection">Clear</button>
      </div>
    </div>

    <section class="inv-panel">
      <div class="inv-table-wrap">
        <table class="inv-table">
          <thead>
            <tr>
              <th class="check-col">
                <input
                  type="checkbox"
                  :checked="allVisibleSelected"
                  :disabled="!filteredStock.length"
                  @change="toggleSelectAll"
                />
              </th>
              <th>Product</th>
              <th class="num">Stock</th>
              <th class="num">Threshold</th>
              <th class="num">Reserved</th>
              <th class="num">Available</th>
              <th>Health</th>
              <th>Status</th>
              <th class="actions-col">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="9" class="inv-message">Loading inventory…</td>
            </tr>
            <tr v-else-if="filteredStock.length === 0">
              <td colspan="9" class="inv-message">{{ emptyMessage }}</td>
            </tr>
            <tr
              v-for="item in filteredStock"
              :id="`inv-row-${item.productId}`"
              :key="item.productId"
              :class="rowClass(item)"
            >
              <td class="check-col" @click.stop>
                <input
                  type="checkbox"
                  :checked="selectedIds.includes(item.productId)"
                  @change="toggleSelect(item.productId)"
                />
              </td>
              <td>
                <div class="inv-product">
                  <div class="inv-product-name">{{ item.productName }}</div>
                  <div class="inv-product-meta">{{ categoryLabel(item.category) }}</div>
                </div>
              </td>
              <td class="num"><strong>{{ item.stockQuantity }}</strong></td>
              <td class="num">{{ item.lowStockThreshold }}</td>
              <td class="num">{{ item.reservedQuantity }}</td>
              <td class="num">{{ item.availableQuantity }}</td>
              <td>
                <div class="inv-health">
                  <div class="inv-health-bar">
                    <div
                      class="inv-health-fill"
                      :class="`health-${stockHealthColor(item)}`"
                      :style="{ width: `${stockHealthPercentage(item)}%` }"
                    ></div>
                  </div>
                </div>
              </td>
              <td>
                <span class="inv-status" :class="`status-${statusOf(item)}`">
                  {{ statusLabel(item) }}
                </span>
              </td>
              <td class="actions-col" @click.stop>
                <div class="inv-actions">
                  <button class="act act-inc" type="button" :disabled="updating" @click="quickRowRestock(item)">
                    +{{ store.suggestedRestockQty(item) }}
                  </button>
                  <button class="act act-inc" type="button" @click="openAdjust(item, 'increment')">
                    Add
                  </button>
                  <button
                    class="act act-dec"
                    type="button"
                    :disabled="item.stockQuantity <= 0"
                    @click="openAdjust(item, 'decrement')"
                  >
                    Remove
                  </button>
                  <div class="inv-menu">
                    <button
                      class="act act-more"
                      type="button"
                      :aria-expanded="actionMenuId === item.productId"
                      @click="toggleMenu(item.productId)"
                    >
                      More
                    </button>
                    <div v-if="actionMenuId === item.productId" class="inv-menu-panel">
                      <button type="button" @click="openAdjust(item, 'set')">Set level</button>
                      <button type="button" @click="openThreshold(item)">Set threshold</button>
                      <button type="button" @click="openHistory(item)">View history</button>
                    </div>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="inv-reservations">
      <div class="vendor-section-head">
        <div>
          <h2>Active reservations</h2>
          <p>Live countdown until checkout holds expire and stock is released.</p>
        </div>
        <span class="inv-count-pill">{{ activeReservations.length }} active</span>
      </div>

      <div v-if="reservationsLoading" class="loading-state">Loading reservations…</div>
      <div v-else-if="activeReservations.length === 0" class="empty-state vendor-empty">
        No active checkout reservations right now.
      </div>
      <div v-else class="inv-table-wrap">
        <table class="inv-table inv-reservation-table">
          <thead>
            <tr>
              <th>Product</th>
              <th class="num">Qty</th>
              <th>Order</th>
              <th>Status</th>
              <th>Countdown</th>
              <th>Expires</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="r in activeReservations"
              :key="r.id"
              :class="{ 'row-expiring': isExpiringSoon(r) }"
            >
              <td>{{ productNameById[r.productId] || `Product #${r.productId}` }}</td>
              <td class="num"><strong>{{ r.quantity }}</strong></td>
              <td>{{ r.orderId ? `#${r.orderId}` : '—' }}</td>
              <td>
                <span class="inv-status status-reserved">
                  {{ reservationStatus(r) }}
                </span>
              </td>
              <td>
                <span class="vendor-countdown" :class="{ urgent: isExpiringSoon(r) }">
                  {{ countdown(r.expiresAt) }}
                </span>
              </td>
              <td>{{ formatDate(r.expiresAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Adjust modal -->
    <div v-if="modal.visible" class="modal-overlay" @click.self="closeAdjust">
      <div class="modal" role="dialog" aria-modal="true">
        <div class="modal-header">
          <h3 class="modal-title">{{ modalTitle }}</h3>
          <button class="modal-x" type="button" title="Close" @click="closeAdjust">×</button>
        </div>
        <div class="modal-body">
          <p class="modal-product">{{ modal.item?.productName }}</p>

          <div class="mode-tabs">
            <button
              type="button"
              class="mode-tab"
              :class="{ active: modal.mode === 'increment' }"
              @click="setMode('increment')"
            >
              Add
            </button>
            <button
              type="button"
              class="mode-tab"
              :class="{ active: modal.mode === 'decrement' }"
              @click="setMode('decrement')"
            >
              Remove
            </button>
            <button
              type="button"
              class="mode-tab"
              :class="{ active: modal.mode === 'set' }"
              @click="setMode('set')"
            >
              Set level
            </button>
          </div>

          <div v-if="modal.mode !== 'decrement'" class="quick-amounts">
            <button
              v-for="amount in QUICK_AMOUNTS"
              :key="amount"
              type="button"
              class="quick-chip"
              @click="applyQuickAmount(amount)"
            >
              {{ modal.mode === 'set' ? amount : `+${amount}` }}
            </button>
            <button
              v-if="modal.mode === 'increment'"
              type="button"
              class="quick-chip quick-chip-accent"
              @click="applyQuickAmount(suggestedQty)"
            >
              Safe +{{ suggestedQty }}
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
            <span>Current: <strong>{{ currentStock }}</strong></span>
            <span class="preview-arrow">→</span>
            <span>New: <strong>{{ previewStock }}</strong></span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" type="button" @click="closeAdjust">Cancel</button>
          <button
            class="btn btn-primary"
            type="button"
            :disabled="!canSubmit || updating"
            @click="submitAdjust"
          >
            {{ updating ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Threshold modal -->
    <div v-if="thresholdModal.visible" class="modal-overlay" @click.self="closeThreshold">
      <div class="modal modal-small" role="dialog" aria-modal="true">
        <div class="modal-header">
          <h3 class="modal-title">Low stock threshold</h3>
          <button class="modal-x" type="button" title="Close" @click="closeThreshold">×</button>
        </div>
        <div class="modal-body">
          <p class="modal-product">{{ thresholdModal.item?.productName }}</p>
          <p class="modal-hint">
            Products are marked low stock when quantity falls to this level or below.
          </p>
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
          <button class="btn btn-secondary" type="button" @click="closeThreshold">Cancel</button>
          <button class="btn btn-primary" type="button" :disabled="updating" @click="submitThreshold">
            {{ updating ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </div>
    </div>

    <!-- History modal -->
    <div v-if="historyVisible" class="modal-overlay" @click.self="store.closeHistory()">
      <div class="modal modal-wide" role="dialog" aria-modal="true">
        <div class="modal-header">
          <h3 class="modal-title">Stock history</h3>
          <button class="modal-x" type="button" title="Close" @click="store.closeHistory()">×</button>
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
          <button class="btn btn-primary" type="button" @click="store.closeHistory()">Close</button>
        </div>
      </div>
    </div>

    <ProductForm
      v-if="showProductForm"
      :product="editingProduct"
      :categories="productCategories"
      @save="onProductSave"
      @close="closeProductForm"
      @images-changed="onProductImagesChanged"
    />
  </div>
</template>
