<template>
  <div class="page-wrap">
    <div class="page-header">
      <h1 class="page-title">Health Events</h1>
    </div>

    <p v-if="error" class="alert alert-error">{{ error }}</p>

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
      <div v-for="event in events" :key="event.id" class="event-card">
        <div class="event-card-top">
          <span class="category-badge">{{ event.category }}</span>
          <span v-if="event.capacity" class="capacity-text">{{ event.capacity }} spots</span>
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
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { listEvents, listEventCategories } from '../services/event.js'

const events = ref([])
const categories = ref([])
const categoryFilter = ref('All')
const loading = ref(false)
const error = ref('')

const categoryOptions = computed(() => ['All', ...categories.value])

async function loadEvents() {
  loading.value = true
  error.value = ''
  try {
    const params = { status: 'PUBLISHED' }
    if (categoryFilter.value !== 'All') {
      params.category = categoryFilter.value
    }
    events.value = await listEvents(params)
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load events'
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await listEventCategories()
  } catch (_) {
    categories.value = []
  }
}

function setCategory(cat) {
  categoryFilter.value = cat
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
})
</script>
