<template>
  <div class="page-wrap">
    <div class="page-header">
      <h1 class="page-title">Health Events</h1>
    </div>

    <p v-if="error" class="alert alert-error">{{ error }}</p>

    <div class="event-toolbar">
      <div class="event-search-group">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
        </svg>
        <input
          v-model.trim="keyword"
          class="event-search-input"
          type="text"
          placeholder="Search events by title, location, organizer..."
          @keyup.enter="applySearch"
        />
      </div>
      <select v-model="cityFilter" class="form-input event-filter-select" @change="applySearch">
        <option value="">All locations</option>
        <option v-for="city in cities" :key="city" :value="city">{{ city }}</option>
      </select>
      <input v-model="dateFrom" class="form-input event-filter-select" type="date" @change="applySearch" />
      <input v-model="dateTo" class="form-input event-filter-select" type="date" @change="applySearch" />
    </div>

    <div class="filter-row">
      <button
        v-for="cat in categoryOptions"
        :key="cat"
        class="filter-chip"
        :class="{ active: categoryFilter === cat }"
        @click="setCategory(cat)"
      >
        {{ cat }}
      </button>
    </div>

    <div v-if="loading" class="loading-state">Loading events...</div>

    <div v-else-if="events.length === 0" class="empty-state">
      No events found.
    </div>

    <div v-else class="events-grid">
      <router-link
        v-for="event in events"
        :key="event.id"
        :to="`/events/${event.id}`"
        class="event-card"
      >
        <div class="event-card-top">
          <span class="category-badge">{{ event.category }}</span>
          <span
            v-if="event.capacity"
            class="capacity-text"
            :class="{ 'capacity-full': spotsLeft(event) === 0 }"
          >
            {{ spotsLeft(event) === 0 ? 'Event full' : spotsLeft(event) + ' spots left' }}
          </span>
        </div>
        <h3 class="event-card-title">{{ event.title }}</h3>
        <p v-if="event.description" class="event-card-desc">{{ event.description }}</p>
        <div class="event-meta">
          <span v-if="event.startDateTime" class="event-meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
            </svg>
            {{ formatDateTime(event.startDateTime) }}
          </span>
          <span v-if="event.venue || event.city" class="event-meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" /><circle cx="12" cy="10" r="3" />
            </svg>
            {{ event.venue }}{{ event.venue && event.city ? ', ' : '' }}{{ event.city }}
          </span>
          <span v-if="event.organizer" class="event-meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" />
            </svg>
            {{ event.organizer }}
          </span>
        </div>
        <div v-if="event.tags && event.tags.length" class="tag-list">
          <span v-for="tag in event.tags" :key="tag" class="tag-chip">{{ tag }}</span>
        </div>
      </router-link>
    </div>

    <div v-if="totalPages > 1" class="event-pagination">
      <button class="btn-secondary" :disabled="page === 0" @click="goToPage(page - 1)">Prev</button>
      <span class="event-pagination-info">Page {{ page + 1 }} of {{ totalPages }}</span>
      <button class="btn-secondary" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">Next</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { searchEvents, listEventCategories, listEventCities } from '../services/event.js'

let pollTimer = null

const events = ref([])
const categories = ref([])
const cities = ref([])
const categoryFilter = ref('All')
const keyword = ref('')
const cityFilter = ref('')
const dateFrom = ref('')
const dateTo = ref('')
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = 9
const totalPages = ref(0)

const categoryOptions = computed(() => ['All', ...categories.value])

async function loadEvents(silent = false) {
  if (!silent) loading.value = true
  if (!silent) error.value = ''
  try {
    const params = {
      status: 'PUBLISHED',
      page: page.value,
      size,
    }
    if (categoryFilter.value !== 'All') params.category = categoryFilter.value
    if (keyword.value) params.keyword = keyword.value
    if (cityFilter.value) params.city = cityFilter.value
    if (dateFrom.value) params.dateFrom = dateFrom.value
    if (dateTo.value) params.dateTo = dateTo.value
    const result = await searchEvents(params)
    events.value = result.content || []
    totalPages.value = result.totalPages || 0
  } catch (e) {
    if (!silent) error.value = e.response?.data?.error || 'Failed to load events'
  } finally {
    if (!silent) loading.value = false
  }
}

function spotsLeft(event) {
  const capacity = event.capacity
  if (!capacity) return null
  return Math.max(capacity - (event.registeredCount || 0), 0)
}

async function loadCategories() {
  try {
    categories.value = await listEventCategories()
  } catch (_) {
    categories.value = []
  }
}

async function loadCities() {
  try {
    cities.value = await listEventCities()
  } catch (_) {
    cities.value = []
  }
}

function setCategory(cat) {
  categoryFilter.value = cat
  page.value = 0
  loadEvents()
}

function applySearch() {
  page.value = 0
  loadEvents()
}

function goToPage(target) {
  page.value = target
  loadEvents()
}

function formatDateTime(dt) {
  if (!dt) return '—'
  const date = new Date(dt)
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  loadEvents()
  loadCategories()
  loadCities()
  pollTimer = setInterval(() => loadEvents(true), 20000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>
