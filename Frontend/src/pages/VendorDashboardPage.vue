<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useInventoryStore } from '../stores/inventory.js'

const router = useRouter()
const inventoryStore = useInventoryStore()
const {
  loading,
  updating,
  error,
  success,
  totalProducts,
  totalUnits,
  totalAvailable,
  totalReservedUnits,
  lowStockCount,
  outOfStockCount,
  reservedCount,
  stockHealthScore,
  activeReservations,
  attentionItems,
  categoryBreakdown,
  recentMovements,
  stock,
} = storeToRefs(inventoryStore)

const ready = ref(false)
const restockingId = ref(null)
const searchQuery = ref('')
const now = ref(Date.now())
let tickTimer = null

onMounted(async () => {
  await inventoryStore.refreshAll()
  ready.value = true
  tickTimer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (tickTimer) clearInterval(tickTimer)
})

const hasAlerts = computed(() => lowStockCount.value > 0 || outOfStockCount.value > 0)

const healthTone = computed(() => {
  if (stockHealthScore.value >= 85) return 'good'
  if (stockHealthScore.value >= 60) return 'warn'
  return 'bad'
})

const searchResults = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return []
  return stock.value
    .filter(
      (i) =>
        (i.productName || '').toLowerCase().includes(q) ||
        categoryLabel(i.category).toLowerCase().includes(q),
    )
    .slice(0, 6)
})

const expiringReservations = computed(() =>
  activeReservations.value
    .slice()
    .sort((a, b) => new Date(a.expiresAt || 0) - new Date(b.expiresAt || 0))
    .slice(0, 5),
)

function goInventory(filter = 'all') {
  router.push({ path: '/inventory', query: filter === 'all' ? {} : { filter } })
}

function openProduct(item) {
  router.push({
    path: '/inventory',
    query: {
      focus: item.productId,
      ...(item.outOfStock ? { filter: 'out' } : item.lowStock ? { filter: 'low' } : {}),
    },
  })
}

function categoryLabel(cat) {
  if (!cat) return ''
  return cat
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

function typeLabel(t) {
  const labels = {
    INIT: 'Initial',
    PURCHASE: 'Restocked',
    SALE: 'Sale',
    RESERVED: 'Reserved',
    RESERVATION_RELEASED: 'Released',
    RESERVATION_EXPIRED: 'Expired',
    ADJUSTMENT: 'Adjusted',
    CANCELLATION: 'Cancelled',
    REFUND: 'Refund',
  }
  return labels[t] || String(t || '').replace(/_/g, ' ').toLowerCase()
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

function countdown(expiresAt) {
  if (!expiresAt) return '—'
  const ms = new Date(expiresAt).getTime() - now.value
  if (ms <= 0) return 'Expired'
  const totalSec = Math.floor(ms / 1000)
  const m = Math.floor(totalSec / 60)
  const s = totalSec % 60
  return `${m}m ${String(s).padStart(2, '0')}s`
}

function productName(productId) {
  return stock.value.find((i) => i.productId === productId)?.productName || `Product #${productId}`
}

async function quickRestock(item) {
  restockingId.value = item.productId
  await inventoryStore.restockToSafeLevel(item)
  restockingId.value = null
}

async function restockAllLow() {
  const targets = attentionItems.value
  if (!targets.length) return
  await inventoryStore.bulkRestock(targets, 'Bulk restock from vendor dashboard')
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
  const rows = stock.value.map((item) => [
    item.productName,
    categoryLabel(item.category),
    item.stockQuantity,
    item.lowStockThreshold,
    item.reservedQuantity,
    item.availableQuantity,
    item.outOfStock ? 'Out of stock' : item.lowStock ? 'Low stock' : 'In stock',
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
</script>

<template>
  <div class="page-wrap vendor-hub">
    <header class="vendor-hero">
      <div class="vendor-hero-copy">
        <h1 class="vendor-title">Vendor workspace</h1>
        <p class="vendor-lead">
          Monitor stock health, restock low items quickly, and keep an eye on checkout holds.
        </p>
        <div class="vendor-hero-actions">
          <button class="btn btn-primary" type="button" @click="goInventory()">
            Open inventory
          </button>
          <button
            class="btn btn-secondary"
            type="button"
            :disabled="loading || !attentionItems.length || updating"
            @click="restockAllLow"
          >
            {{ updating ? 'Restocking…' : 'Restock all low' }}
          </button>
          <button class="btn btn-secondary" type="button" @click="exportCsv">
            Export CSV
          </button>
        </div>
      </div>

      <div class="vendor-hero-panel">
        <div class="vendor-panel-label">Stock health</div>
        <div class="vendor-health-ring" :class="`health-${healthTone}`">
          <span class="vendor-health-score">{{ stockHealthScore }}%</span>
          <span class="vendor-health-caption">healthy SKUs</span>
        </div>
        <div class="vendor-panel-grid vendor-panel-grid-compact">
          <div>
            <span class="vendor-panel-value">{{ totalAvailable }}</span>
            <span class="vendor-panel-meta">Available</span>
          </div>
          <div>
            <span class="vendor-panel-value">{{ totalReservedUnits }}</span>
            <span class="vendor-panel-meta">Reserved units</span>
          </div>
        </div>
        <button
          class="btn-link"
          type="button"
          :disabled="loading"
          @click="inventoryStore.refreshAll()"
        >
          {{ loading ? 'Refreshing…' : 'Refresh data' }}
        </button>
      </div>
    </header>

    <div v-if="error" class="alert alert-error inv-alert">
      <span>{{ error }}</span>
      <button class="banner-close" type="button" @click="inventoryStore.clearMessages()">×</button>
    </div>
    <div v-if="success" class="alert alert-success inv-alert">
      <span>{{ success }}</span>
      <button class="banner-close" type="button" @click="inventoryStore.clearMessages()">×</button>
    </div>

    <div v-if="hasAlerts" class="vendor-alert" role="status">
      <div>
        <strong>Attention needed.</strong>
        {{ outOfStockCount }} out of stock · {{ lowStockCount }} below threshold.
      </div>
      <button class="btn-link" type="button" @click="goInventory(outOfStockCount ? 'out' : 'low')">
        Review items
      </button>
    </div>

    <section class="vendor-metrics" aria-label="Inventory metrics">
      <button class="vendor-metric" type="button" @click="goInventory('all')">
        <span class="vendor-metric-label">Products</span>
        <span class="vendor-metric-value">{{ totalProducts }}</span>
        <span class="vendor-metric-hint">{{ totalUnits }} total units</span>
      </button>
      <button class="vendor-metric metric-warn" type="button" @click="goInventory('low')">
        <span class="vendor-metric-label">Low stock</span>
        <span class="vendor-metric-value">{{ lowStockCount }}</span>
        <span class="vendor-metric-hint">Below threshold</span>
      </button>
      <button class="vendor-metric metric-danger" type="button" @click="goInventory('out')">
        <span class="vendor-metric-label">Out of stock</span>
        <span class="vendor-metric-value">{{ outOfStockCount }}</span>
        <span class="vendor-metric-hint">Unavailable</span>
      </button>
      <button class="vendor-metric metric-info" type="button" @click="goInventory('reserved')">
        <span class="vendor-metric-label">Reserved SKUs</span>
        <span class="vendor-metric-value">{{ reservedCount }}</span>
        <span class="vendor-metric-hint">{{ activeReservations.length }} active holds</span>
      </button>
    </section>

    <section class="vendor-search-block">
      <div class="inv-search-wrap vendor-search">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
        </svg>
        <input
          v-model="searchQuery"
          class="inv-search"
          type="search"
          placeholder="Jump to a product…"
        />
      </div>
      <ul v-if="searchResults.length" class="vendor-search-results">
        <li v-for="item in searchResults" :key="item.productId">
          <button type="button" @click="openProduct(item)">
            <span>
              <strong>{{ item.productName }}</strong>
              <small>{{ categoryLabel(item.category) }} · {{ item.stockQuantity }} on hand</small>
            </span>
            <span
              class="inv-status"
              :class="item.outOfStock ? 'status-out' : item.lowStock ? 'status-low' : 'status-ok'"
            >
              {{ item.outOfStock ? 'Out' : item.lowStock ? 'Low' : 'OK' }}
            </span>
          </button>
        </li>
      </ul>
    </section>

    <div class="vendor-split">
      <section class="vendor-attention">
        <div class="vendor-section-head">
          <div>
            <h2>Needs restock</h2>
            <p>Quick-restock brings items back above a safe level.</p>
          </div>
          <button class="btn-link" type="button" @click="goInventory('low')">Open list</button>
        </div>

        <div v-if="!ready || loading" class="loading-state">Loading inventory…</div>
        <div v-else-if="attentionItems.length === 0" class="empty-state vendor-empty">
          All stock levels look healthy.
        </div>
        <ul v-else class="vendor-attention-list">
          <li v-for="item in attentionItems.slice(0, 8)" :key="item.productId">
            <div>
              <div class="vendor-item-name">{{ item.productName }}</div>
              <div class="vendor-item-meta">
                {{ categoryLabel(item.category) }} · {{ item.stockQuantity }} on hand · threshold
                {{ item.lowStockThreshold }}
              </div>
            </div>
            <span
              class="inv-status"
              :class="item.outOfStock ? 'status-out' : 'status-low'"
            >
              {{ item.outOfStock ? 'Out of stock' : 'Low stock' }}
            </span>
            <div class="vendor-item-actions">
              <button
                class="act act-inc"
                type="button"
                :disabled="updating"
                @click="quickRestock(item)"
              >
                {{ restockingId === item.productId ? '…' : `+${inventoryStore.suggestedRestockQty(item)}` }}
              </button>
              <button class="btn-link" type="button" @click="openProduct(item)">Edit</button>
            </div>
          </li>
        </ul>
      </section>

      <section class="vendor-side-stack">
        <article class="vendor-card">
          <div class="vendor-section-head">
            <div>
              <h2>By category</h2>
              <p>Where your units are concentrated.</p>
            </div>
          </div>
          <div v-if="!categoryBreakdown.length" class="empty-state vendor-empty">No categories yet.</div>
          <ul v-else class="vendor-category-list">
            <li v-for="row in categoryBreakdown.slice(0, 6)" :key="row.category">
              <div class="vendor-category-top">
                <strong>{{ categoryLabel(row.category) }}</strong>
                <span>{{ row.units }} units</span>
              </div>
              <div class="inv-health-bar">
                <div
                  class="inv-health-fill"
                  :class="row.out || row.low ? 'health-warning' : 'health-success'"
                  :style="{ width: `${Math.min(100, (row.units / Math.max(totalUnits, 1)) * 100)}%` }"
                ></div>
              </div>
              <div class="vendor-category-meta">
                {{ row.products }} products
                <template v-if="row.low"> · {{ row.low }} low</template>
                <template v-if="row.out"> · {{ row.out }} out</template>
              </div>
            </li>
          </ul>
        </article>

        <article class="vendor-card">
          <div class="vendor-section-head">
            <div>
              <h2>Checkout holds</h2>
              <p>Reserved stock with live countdown.</p>
            </div>
            <button class="btn-link" type="button" @click="goInventory('reserved')">View</button>
          </div>
          <div v-if="!expiringReservations.length" class="empty-state vendor-empty">
            No active reservations.
          </div>
          <ul v-else class="vendor-reservation-list">
            <li v-for="r in expiringReservations" :key="r.id">
              <div>
                <div class="vendor-item-name">{{ productName(r.productId) }}</div>
                <div class="vendor-item-meta">
                  {{ r.quantity }} units
                  <template v-if="r.orderId"> · order #{{ r.orderId }}</template>
                </div>
              </div>
              <span class="vendor-countdown" :class="{ urgent: countdown(r.expiresAt) === 'Expired' || countdown(r.expiresAt).startsWith('0m') }">
                {{ countdown(r.expiresAt) }}
              </span>
            </li>
          </ul>
        </article>
      </section>
    </div>

    <section class="vendor-card vendor-activity">
      <div class="vendor-section-head">
        <div>
          <h2>Recent stock activity</h2>
          <p>Latest restocks, sales, reservations, and adjustments.</p>
        </div>
      </div>
      <div v-if="!recentMovements.length" class="empty-state vendor-empty">
        No recent movements yet.
      </div>
      <ul v-else class="vendor-activity-list">
        <li v-for="entry in recentMovements" :key="`${entry.id}-${entry.productId}`">
          <div>
            <div class="vendor-item-name">{{ entry.productName }}</div>
            <div class="vendor-item-meta">{{ typeLabel(entry.changeType) }} · {{ formatDate(entry.createdAt) }}</div>
          </div>
          <span class="h-delta" :class="entry.quantityChange > 0 ? 'delta-up' : entry.quantityChange < 0 ? 'delta-down' : 'delta-zero'">
            {{ signedDelta(entry.quantityChange) }}
          </span>
        </li>
      </ul>
    </section>
  </div>
</template>
