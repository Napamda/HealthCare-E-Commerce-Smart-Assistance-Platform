<template>
  <div class="page-wrap">
    <div class="page-header">
      <h1 class="page-title">Event Management</h1>
      <button class="btn-primary" @click="openCreate">
        + New Event
      </button>
    </div>

    <p v-if="error" class="alert alert-error">{{ error }}</p>
    <p v-if="success" class="alert alert-success">{{ success }}</p>

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

    <div v-else class="event-table-wrap">
      <table class="event-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Category</th>
            <th>Date</th>
            <th>Venue</th>
            <th>Status</th>
            <th class="col-actions">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="event in events" :key="event.id">
            <td class="col-title">
              <span class="event-title">{{ event.title }}</span>
              <span v-if="event.tags && event.tags.length" class="tag-list">
                <span v-for="tag in event.tags" :key="tag" class="tag-chip">{{ tag }}</span>
              </span>
            </td>
            <td><span class="category-badge">{{ event.category }}</span></td>
            <td class="col-date">{{ formatDateTime(event.startDateTime) }}</td>
            <td>
              <span class="venue-text">{{ event.venue }}</span>
              <span v-if="event.city" class="city-text">{{ event.city }}</span>
            </td>
            <td><span class="status-badge" :class="`status-${event.status.toLowerCase()}`">{{ event.status }}</span></td>
            <td class="col-actions">
              <button class="btn-link" @click="openEdit(event)">Edit</button>
              <button class="btn-link btn-link-danger" :disabled="deletingId === event.id" @click="removeEvent(event)">
                {{ deletingId === event.id ? 'Deleting...' : 'Delete' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showForm" class="modal-overlay" @click.self="closeForm">
      <div class="event-form-card">
        <div class="form-header">
          <h2>{{ editingEvent ? 'Edit Event' : 'New Event' }}</h2>
          <button class="btn-icon" @click="closeForm">&times;</button>
        </div>

        <form class="event-form" @submit.prevent="submitForm">
          <div class="form-grid">
            <label class="form-field form-field-wide">
              <span class="form-label">Title *</span>
              <input v-model.trim="form.title" type="text" class="form-input" placeholder="Event title" />
            </label>

            <label class="form-field">
              <span class="form-label">Category *</span>
              <input v-model.trim="form.category" list="category-options" type="text" class="form-input" placeholder="e.g. Health Talk" />
              <datalist id="category-options">
                <option v-for="cat in categoryOptions" :key="cat" :value="cat" />
              </datalist>
            </label>

            <label class="form-field">
              <span class="form-label">Status</span>
              <select v-model="form.status" class="form-input">
                <option value="PUBLISHED">Published</option>
                <option value="DRAFT">Draft</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </label>

            <label class="form-field">
              <span class="form-label">Start Date &amp; Time *</span>
              <input v-model="form.startDateTime" type="datetime-local" class="form-input" />
            </label>

            <label class="form-field">
              <span class="form-label">End Date &amp; Time</span>
              <input v-model="form.endDateTime" type="datetime-local" class="form-input" />
            </label>

            <label class="form-field">
              <span class="form-label">Venue</span>
              <input v-model.trim="form.venue" type="text" class="form-input" placeholder="Venue name" />
            </label>

            <label class="form-field">
              <span class="form-label">City</span>
              <input v-model.trim="form.city" type="text" class="form-input" placeholder="City" />
            </label>

            <label class="form-field">
              <span class="form-label">Capacity</span>
              <input v-model="form.capacity" type="number" min="1" class="form-input" placeholder="Max attendees" />
            </label>

            <label class="form-field">
              <span class="form-label">Organizer</span>
              <input v-model.trim="form.organizer" type="text" class="form-input" placeholder="Organizer name" />
            </label>

            <label class="form-field form-field-wide">
              <span class="form-label">Tags</span>
              <input v-model="form.tagsText" type="text" class="form-input" placeholder="Comma separated, e.g. screening, free, blood-pressure" />
            </label>

            <label class="form-field form-field-wide">
              <span class="form-label">Description</span>
              <textarea v-model="form.description" class="form-input form-textarea" rows="4" placeholder="Event description"></textarea>
            </label>
          </div>

          <p v-if="formError" class="alert alert-error">{{ formError }}</p>

          <div class="form-actions">
            <button type="button" class="btn-secondary" @click="closeForm">Cancel</button>
            <button type="submit" class="btn-primary" :disabled="saving">
              {{ saving ? 'Saving...' : (editingEvent ? 'Save Changes' : 'Create Event') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { listEvents, listEventCategories, createEvent, updateEvent, deleteEvent } from '../services/event.js'

const events = ref([])
const categories = ref([])
const categoryFilter = ref('All')
const loading = ref(false)
const error = ref('')
const success = ref('')
const showForm = ref(false)
const editingEvent = ref(null)
const saving = ref(false)
const deletingId = ref(null)
const formError = ref('')

const form = ref(emptyForm())

const categoryOptions = computed(() => ['All', ...categories.value])

function emptyForm() {
  return {
    title: '',
    description: '',
    category: '',
    tagsText: '',
    startDateTime: '',
    endDateTime: '',
    venue: '',
    city: '',
    capacity: '',
    organizer: '',
    status: 'PUBLISHED',
  }
}

async function loadEvents() {
  loading.value = true
  error.value = ''
  try {
    const params = {}
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

function openCreate() {
  editingEvent.value = null
  form.value = emptyForm()
  formError.value = ''
  showForm.value = true
}

function openEdit(event) {
  editingEvent.value = event
  form.value = {
    title: event.title || '',
    description: event.description || '',
    category: event.category || '',
    tagsText: (event.tags || []).join(', '),
    startDateTime: toInputValue(event.startDateTime),
    endDateTime: toInputValue(event.endDateTime),
    venue: event.venue || '',
    city: event.city || '',
    capacity: event.capacity ?? '',
    organizer: event.organizer || '',
    status: event.status || 'PUBLISHED',
  }
  formError.value = ''
  showForm.value = true
}

function toInputValue(dt) {
  if (!dt) return ''
  return dt.slice(0, 16)
}

function buildPayload() {
  return {
    title: form.value.title,
    description: form.value.description,
    category: form.value.category,
    tags: form.value.tagsText.split(',').map((t) => t.trim()).filter(Boolean),
    startDateTime: form.value.startDateTime,
    endDateTime: form.value.endDateTime || null,
    venue: form.value.venue,
    city: form.value.city,
    capacity: form.value.capacity ? Number(form.value.capacity) : null,
    organizer: form.value.organizer,
    status: form.value.status,
  }
}

async function submitForm() {
  formError.value = ''
  if (!form.value.title || !form.value.category || !form.value.startDateTime) {
    formError.value = 'Title, category and start date are required'
    return
  }
  saving.value = true
  try {
    if (editingEvent.value) {
      await updateEvent(editingEvent.value.id, buildPayload())
      success.value = 'Event updated'
    } else {
      await createEvent(buildPayload())
      success.value = 'Event created'
    }
    closeForm()
    await loadEvents()
    await loadCategories()
  } catch (e) {
    formError.value = e.response?.data?.error || 'Failed to save event'
  } finally {
    saving.value = false
  }
}

async function removeEvent(event) {
  if (!window.confirm(`Delete event "${event.title}"?`)) return
  deletingId.value = event.id
  error.value = ''
  try {
    await deleteEvent(event.id)
    success.value = 'Event deleted'
    await loadEvents()
    await loadCategories()
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to delete event'
  } finally {
    deletingId.value = null
  }
}

function closeForm() {
  showForm.value = false
  editingEvent.value = null
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
