<script setup>
import { ref, onMounted, watch } from 'vue'
import {
  getVendors,
  decideOnVendor,
  getProductsForModeration,
  decideOnProduct,
  getEventsForModeration,
  decideOnEvent,
} from '../../services/admin.js'
import '../../styles/components/admin-moderation.css'

const activeTab = ref('vendors')
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const messageType = ref('success')

const vendors = ref([])
const products = ref([])
const events = ref([])

const vendorFilter = ref('')
const productFilter = ref('')
const eventFilter = ref('')

function flash(type, text) {
  messageType.value = type
  message.value = text
  setTimeout(() => { message.value = '' }, 4000)
}

function errText(e) {
  return e.response?.data?.error || e.message || 'Something went wrong'
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

// ---- Vendors (Task 3.2) ----

async function fetchVendors() {
  loading.value = true
  try {
    vendors.value = await getVendors(vendorFilter.value || null)
  } catch (e) { flash('error', errText(e)) }
  finally { loading.value = false }
}

async function approveVendor(vendor) {
  if (!confirm(`Approve vendor "${vendor.businessName}"?`)) return
  saving.value = true
  try {
    const updated = await decideOnVendor(vendor.profileId, 'APPROVED')
    mergeVendor(updated)
    flash('success', `${updated.businessName} has been approved.`)
  } catch (e) { flash('error', errText(e)) }
  finally { saving.value = false }
}

async function rejectVendor(vendor) {
  const reason = prompt(`Reject vendor "${vendor.businessName}"?\nReason (required):`)
  if (!reason) return
  saving.value = true
  try {
    const updated = await decideOnVendor(vendor.profileId, 'REJECTED', reason)
    mergeVendor(updated)
    flash('success', `${updated.businessName} has been rejected.`)
  } catch (e) { flash('error', errText(e)) }
  finally { saving.value = false }
}

function mergeVendor(updated) {
  const idx = vendors.value.findIndex((v) => v.profileId === updated.profileId)
  if (idx !== -1) vendors.value.splice(idx, 1, updated)
}

// ---- Products (Task 3.3) ----

async function fetchProducts() {
  loading.value = true
  try {
    products.value = await getProductsForModeration(productFilter.value || null)
  } catch (e) { flash('error', errText(e)) }
  finally { loading.value = false }
}

async function approveProduct(product) {
  if (!confirm(`Approve product "${product.name}"?`)) return
  saving.value = true
  try {
    const updated = await decideOnProduct(product.id, 'APPROVED')
    mergeProduct(updated)
    flash('success', `Product "${updated.name}" has been approved.`)
  } catch (e) { flash('error', errText(e)) }
  finally { saving.value = false }
}

async function rejectProduct(product) {
  const reason = prompt(`Reject product "${product.name}"?\nReason (required):`)
  if (!reason) return
  saving.value = true
  try {
    const updated = await decideOnProduct(product.id, 'REJECTED', reason)
    mergeProduct(updated)
    flash('success', `Product "${updated.name}" has been rejected.`)
  } catch (e) { flash('error', errText(e)) }
  finally { saving.value = false }
}

function mergeProduct(updated) {
  const idx = products.value.findIndex((p) => p.id === updated.id)
  if (idx !== -1) products.value.splice(idx, 1, updated)
}

// ---- Events (Task 3.4) ----

async function fetchEvents() {
  loading.value = true
  try {
    events.value = await getEventsForModeration(eventFilter.value || null)
  } catch (e) { flash('error', errText(e)) }
  finally { loading.value = false }
}

async function approveEvent(event) {
  if (!confirm(`Approve event "${event.title}"?`)) return
  saving.value = true
  try {
    const updated = await decideOnEvent(event.id, 'APPROVED')
    mergeEvent(updated)
    flash('success', `Event "${updated.title}" has been published.`)
  } catch (e) { flash('error', errText(e)) }
  finally { saving.value = false }
}

async function rejectEvent(event) {
  const reason = prompt(`Reject event "${event.title}"?\nReason (required):`)
  if (!reason) return
  saving.value = true
  try {
    const updated = await decideOnEvent(event.id, 'REJECTED', reason)
    mergeEvent(updated)
    flash('success', `Event "${updated.title}" has been rejected.`)
  } catch (e) { flash('error', errText(e)) }
  finally { saving.value = false }
}

function mergeEvent(updated) {
  const idx = events.value.findIndex((e) => e.id === updated.id)
  if (idx !== -1) events.value.splice(idx, 1, updated)
}

// ---- Tab switching ----

function switchTab(tab) {
  activeTab.value = tab
  loadActiveTab()
}

function loadActiveTab() {
  if (activeTab.value === 'vendors') fetchVendors()
  else if (activeTab.value === 'products') fetchProducts()
  else if (activeTab.value === 'events') fetchEvents()
}

watch([vendorFilter], () => fetchVendors())
watch([productFilter], () => fetchProducts())
watch([eventFilter], () => fetchEvents())

onMounted(() => fetchVendors())
</script>

<template>
  <div class="mod-page">
    <div class="mod-container">
      <header class="mod-header">
        <h1>Moderation</h1>
        <p>Review and approve vendor applications, products, and events.</p>
      </header>

      <div v-if="message" class="mod-message" :class="messageType">{{ message }}</div>

      <nav class="mod-tabs">
        <button class="mod-tab" :class="{ active: activeTab === 'vendors' }" @click="switchTab('vendors')">
          Vendors
          <span v-if="vendors.length" class="mod-tab-count">{{ vendors.filter(v => v.approvalStatus === 'PENDING').length }}</span>
        </button>
        <button class="mod-tab" :class="{ active: activeTab === 'products' }" @click="switchTab('products')">
          Products
          <span v-if="products.length" class="mod-tab-count">{{ products.filter(p => p.status === 'PENDING').length }}</span>
        </button>
        <button class="mod-tab" :class="{ active: activeTab === 'events' }" @click="switchTab('events')">
          Events
          <span v-if="events.length" class="mod-tab-count">{{ events.filter(e => e.status === 'PENDING').length }}</span>
        </button>
      </nav>

      <div v-if="loading" class="mod-loading">Loading…</div>

      <template v-else>
        <!-- ===================== Vendors ===================== -->
        <section v-show="activeTab === 'vendors'" class="mod-section">
          <div class="mod-filters">
            <select v-model="vendorFilter" class="mod-select">
              <option value="">All statuses</option>
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>

          <div class="mod-card-list">
            <div v-for="vendor in vendors" :key="vendor.profileId" class="mod-card">
              <div class="mod-card-main">
                <div class="mod-card-title">{{ vendor.businessName }}</div>
                <div class="mod-card-sub">
                  {{ vendor.firstName }} {{ vendor.lastName }} · {{ vendor.email }}
                </div>
                <div v-if="vendor.businessLicense" class="mod-card-meta">
                  License: {{ vendor.businessLicense }}
                </div>
                <div v-if="vendor.rejectionReason" class="mod-card-meta mod-reject-reason">
                  Rejection reason: {{ vendor.rejectionReason }}
                </div>
              </div>
              <div class="mod-card-side">
                <span class="mod-status-badge" :class="vendor.approvalStatus.toLowerCase()">
                  {{ vendor.approvalStatus }}
                </span>
                <div v-if="vendor.approvalStatus === 'PENDING'" class="mod-card-actions">
                  <button class="mod-btn mod-btn-success" :disabled="saving" @click="approveVendor(vendor)">Approve</button>
                  <button class="mod-btn mod-btn-danger" :disabled="saving" @click="rejectVendor(vendor)">Reject</button>
                </div>
                <span class="mod-card-date">Applied: {{ formatDate(vendor.createdAt) }}</span>
              </div>
            </div>
            <div v-if="vendors.length === 0" class="mod-empty">No vendor applications to review.</div>
          </div>
        </section>

        <!-- ===================== Products ===================== -->
        <section v-show="activeTab === 'products'" class="mod-section">
          <div class="mod-filters">
            <select v-model="productFilter" class="mod-select">
              <option value="">Moderation queue (pending + rejected)</option>
              <option value="PENDING">Pending</option>
              <option value="REJECTED">Rejected</option>
              <option value="APPROVED">Approved</option>
            </select>
          </div>

          <div class="mod-card-list">
            <div v-for="product in products" :key="product.id" class="mod-card">
              <div class="mod-card-main">
                <div class="mod-card-title">{{ product.name }}</div>
                <div class="mod-card-sub">
                  {{ product.category }} · ${{ Number(product.price).toFixed(2) }}
                  <span v-if="product.manufacturer">· {{ product.manufacturer }}</span>
                </div>
                <div v-if="product.prescriptionRequired" class="mod-card-meta">
                  ⚠ Prescription required
                </div>
                <div v-if="product.moderationReason" class="mod-card-meta mod-reject-reason">
                  Rejection reason: {{ product.moderationReason }}
                </div>
              </div>
              <div class="mod-card-side">
                <span class="mod-status-badge" :class="product.status.toLowerCase()">{{ product.status }}</span>
                <div v-if="product.status === 'PENDING'" class="mod-card-actions">
                  <button class="mod-btn mod-btn-success" :disabled="saving" @click="approveProduct(product)">Approve</button>
                  <button class="mod-btn mod-btn-danger" :disabled="saving" @click="rejectProduct(product)">Reject</button>
                </div>
                <span class="mod-card-date">Created: {{ formatDate(product.createdAt) }}</span>
              </div>
            </div>
            <div v-if="products.length === 0" class="mod-empty">No products in the moderation queue.</div>
          </div>
        </section>

        <!-- ===================== Events ===================== -->
        <section v-show="activeTab === 'events'" class="mod-section">
          <div class="mod-filters">
            <select v-model="eventFilter" class="mod-select">
              <option value="">Pending</option>
              <option value="PENDING">Pending</option>
              <option value="PUBLISHED">Published</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>

          <div class="mod-card-list">
            <div v-for="event in events" :key="event.id" class="mod-card">
              <div class="mod-card-main">
                <div class="mod-card-title">{{ event.title }}</div>
                <div class="mod-card-sub">
                  {{ event.category }}
                  <span v-if="event.organizer">· {{ event.organizer }}</span>
                  <span v-if="event.city">· {{ event.city }}</span>
                </div>
                <div v-if="event.moderationReason" class="mod-card-meta mod-reject-reason">
                  Rejection reason: {{ event.moderationReason }}
                </div>
              </div>
              <div class="mod-card-side">
                <span class="mod-status-badge" :class="event.status.toLowerCase()">{{ event.status }}</span>
                <div v-if="event.status === 'PENDING'" class="mod-card-actions">
                  <button class="mod-btn mod-btn-success" :disabled="saving" @click="approveEvent(event)">Approve</button>
                  <button class="mod-btn mod-btn-danger" :disabled="saving" @click="rejectEvent(event)">Reject</button>
                </div>
                <span class="mod-card-date">Starts: {{ formatDate(event.startDateTime) }}</span>
              </div>
            </div>
            <div v-if="events.length === 0" class="mod-empty">No events awaiting moderation.</div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>
