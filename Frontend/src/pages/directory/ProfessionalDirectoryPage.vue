<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { listProfessionals, listSpecialties, searchNearby } from '../../services/professional.js'
import { geocodeAddress, reverseGeocode } from '../../services/geocoding.js'

const professionals = ref([])
const specialties = ref([])
const isLoading = ref(false)
const error = ref('')

const specialtyFilter = ref('')
const radiusKm = ref(25)
const userLocation = ref(null)
const locating = ref(false)
const addressQuery = ref('')
const geocoding = ref(false)
const locationLabel = ref('')

const mapEl = ref(null)
let map = null
let markers = []
let userMarker = null

const DEFAULT_CENTER = { lat: 5.6037, lng: -0.187 }

const specialtyLabels = {
  CARDIOLOGY: 'Cardiology',
  DERMATOLOGY: 'Dermatology',
  GENERAL_PRACTICE: 'General Practice',
  PEDIATRICS: 'Pediatrics',
  ORTHOPEDICS: 'Orthopedics',
  GYNECOLOGY: 'Gynecology',
  NEUROLOGY: 'Neurology',
}

const specialtyColors = {
  CARDIOLOGY: '#dc2626',
  DERMATOLOGY: '#ec4899',
  GENERAL_PRACTICE: 'var(--color-primary)',
  PEDIATRICS: '#f59e0b',
  ORTHOPEDICS: '#8b5cf6',
  GYNECOLOGY: '#14b8a6',
  NEUROLOGY: '#7c3aed',
}

function specialtyLabel(key) {
  return specialtyLabels[key] || key
}

function specialtyColor(key) {
  return specialtyColors[key] || 'var(--color-primary)'
}

function makeMarkerIcon(specialty, selected = false) {
  return L.divIcon({
    className: 'prof-marker-wrap',
    html: `<div class="prof-marker" style="background:${specialtyColor(specialty)}${selected ? ';transform:scale(1.25)' : ''}">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
      </svg>
    </div>`,
    iconSize: [28, 28],
    iconAnchor: [14, 28],
    popupAnchor: [0, -26],
  })
}

function makeUserIcon() {
  return L.divIcon({
    className: 'prof-marker-wrap',
    html: `<div class="prof-marker prof-marker-user">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10" /><circle cx="12" cy="10" r="3" /><path d="M6.5 18.5a6 6 0 0 1 11 0" />
      </svg>
    </div>`,
    iconSize: [28, 28],
    iconAnchor: [14, 28],
    popupAnchor: [0, -26],
  })
}

function clearMarkers() {
  markers.forEach((m) => map.removeLayer(m))
  markers = []
}

function renderMarkers(list, origin) {
  clearMarkers()
  if (userMarker && map) {
    map.removeLayer(userMarker)
    userMarker = null
  }

  if (origin) {
    userMarker = L.marker([origin.lat, origin.lng], { icon: makeUserIcon() })
      .addTo(map)
      .bindPopup('<b>You</b>')
    userMarker.addTo(map)
  }

  list.forEach((entry) => {
    const p = entry.profile || entry
    const marker = L.marker([p.latitude, p.longitude], { icon: makeMarkerIcon(p.specialty) })
      .addTo(map)
      .bindPopup(
        `<b>${p.title} ${p.fullName}</b><br/>
         ${specialtyLabel(p.specialty)}<br/>
         ${p.city || ''}<br/>
         ${entry.distanceKm !== undefined ? `<b>${formatDistance(entry.distanceKm)}</b> away` : ''}`
      )
    markers.push(marker)
  })

  if (list.length > 0) {
    const points = list.map((e) => {
      const p = e.profile || e
      return [p.latitude, p.longitude]
    })
    if (origin) points.push([origin.lat, origin.lng])
    map.fitBounds(points, { padding: [40, 40], maxZoom: 14 })
  }
}

function formatDistance(km) {
  if (km < 1) return `${Math.round(km * 1000)} m`
  return `${km.toFixed(1)} km`
}

function useMyLocation() {
  if (!navigator.geolocation) {
    error.value = 'Geolocation is not supported by your browser'
    return
  }
  locating.value = true
  error.value = ''
  navigator.geolocation.getCurrentPosition(
    async (pos) => {
      userLocation.value = {
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
      }
      locating.value = false
      locationLabel.value = ''
      try {
        const result = await reverseGeocode(pos.coords.latitude, pos.coords.longitude)
        locationLabel.value = result.displayName || ''
      } catch (_) {
        locationLabel.value = ''
      }
      await loadNearby()
    },
    (err) => {
      locating.value = false
      error.value = 'Unable to detect your location: ' + err.message
    },
    { enableHighAccuracy: true, timeout: 10000 }
  )
}

async function searchByAddress() {
  const query = addressQuery.value.trim()
  if (!query || geocoding.value) return
  geocoding.value = true
  error.value = ''
  try {
    const result = await geocodeAddress(query)
    userLocation.value = { lat: result.latitude, lng: result.longitude }
    locationLabel.value = result.displayName || query
    addressQuery.value = ''
    await loadNearby()
  } catch (e) {
    error.value = e.response?.data?.error || 'Could not find that address'
  } finally {
    geocoding.value = false
  }
}

function clearLocation() {
  userLocation.value = null
  locationLabel.value = ''
  addressQuery.value = ''
  loadAll()
}

async function loadAll() {
  isLoading.value = true
  error.value = ''
  try {
    const data = await listProfessionals(specialtyFilter.value || undefined)
    professionals.value = data
    await nextTick()
    renderMarkers(data, userLocation.value)
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load professionals'
  } finally {
    isLoading.value = false
  }
}

async function loadNearby() {
  if (!userLocation.value) {
    await loadAll()
    return
  }
  isLoading.value = true
  error.value = ''
  try {
    const data = await searchNearby({
      lat: userLocation.value.lat,
      lng: userLocation.value.lng,
      radius: radiusKm.value,
      specialty: specialtyFilter.value || undefined,
    })
    professionals.value = data
    await nextTick()
    renderMarkers(data, userLocation.value)
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to search nearby professionals'
  } finally {
    isLoading.value = false
  }
}

function onFilterChange() {
  if (userLocation.value) {
    loadNearby()
  } else {
    loadAll()
  }
}

onMounted(async () => {
  try {
    const specs = await listSpecialties()
    specialties.value = specs
  } catch (_) {
    specialties.value = []
  }

  map = L.map(mapEl.value, { center: [DEFAULT_CENTER.lat, DEFAULT_CENTER.lng], zoom: 11 })
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)

  await loadAll()
})

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<template>
  <div class="directory-page">
    <div class="page-header">
      <h1 class="page-title">Find Healthcare Professionals</h1>
      <p class="page-subtitle">Search nearby doctors and specialists by location, specialty, and distance</p>
    </div>

    <div class="controls-card">
      <div class="control-row">
        <form class="address-search" @submit.prevent="searchByAddress">
          <input
            v-model="addressQuery"
            type="text"
            class="address-input"
            placeholder="Search by address, city or landmark"
            :disabled="geocoding"
          />
          <button type="submit" class="btn-locate" :disabled="geocoding || !addressQuery.trim()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="7" /><path d="m21 21-4.3-4.3" />
            </svg>
            <span v-if="geocoding">Searching...</span>
            <span v-else>Search</span>
          </button>
        </form>

        <button class="btn-locate btn-locate-secondary" :disabled="locating" @click="useMyLocation">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" /><circle cx="12" cy="12" r="3" />
          </svg>
          <span v-if="locating">Locating...</span>
          <span v-else>Use My Location</span>
        </button>

        <button v-if="userLocation" class="btn-clear" @click="clearLocation">
          Clear location
        </button>
      </div>

      <div v-if="userLocation" class="coord-display">
        <span v-if="locationLabel" class="location-label">{{ locationLabel }}</span>
        <span class="location-coords">{{ userLocation.lat.toFixed(4) }}, {{ userLocation.lng.toFixed(4) }}</span>
      </div>
      <div v-else class="coord-display muted">
        Showing all professionals — search an address or enable location to sort by distance
      </div>

      <div class="filter-row">
        <label class="filter-field">
          <span class="filter-label">Specialty</span>
          <select v-model="specialtyFilter" class="filter-select" @change="onFilterChange">
            <option value="">All specialties</option>
            <option v-for="s in specialties" :key="s" :value="s">{{ specialtyLabel(s) }}</option>
          </select>
        </label>

        <label v-if="userLocation" class="filter-field">
          <span class="filter-label">Max distance</span>
          <select v-model="radiusKm" class="filter-select" @change="onFilterChange">
            <option :value="5">5 km</option>
            <option :value="10">10 km</option>
            <option :value="25">25 km</option>
            <option :value="50">50 km</option>
            <option :value="100">100 km</option>
          </select>
        </label>
      </div>
    </div>

    <div v-if="error" class="error-banner">
      <span>{{ error }}</span>
      <button class="btn-dismiss" @click="error = ''">Dismiss</button>
    </div>

    <div class="directory-body">
      <div ref="mapEl" class="map-container"></div>

      <div class="results-panel">
        <div class="results-header">
          <h2>{{ userLocation ? 'Nearby Professionals' : 'All Professionals' }}</h2>
          <span class="results-count">{{ professionals.length }} found</span>
        </div>

        <div v-if="isLoading && professionals.length === 0" class="loading-state">
          <div class="dot-typing"><span></span><span></span><span></span></div>
          <p>Loading professionals...</p>
        </div>

        <div v-if="!isLoading && professionals.length === 0 && !error" class="empty-state">
          <div class="empty-icon">
            <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10" />
              <path d="M8 14s1.5 2 4 2 4-2 4-2" />
              <line x1="9" y1="9" x2="9.01" y2="9" />
              <line x1="15" y1="9" x2="15.01" y2="9" />
            </svg>
          </div>
          <h2>No professionals found</h2>
          <p>Try widening your distance or clearing the specialty filter</p>
        </div>

        <div v-if="professionals.length > 0" class="prof-list">
          <div
            v-for="entry in professionals"
            :key="entry.id || entry.profile?.id"
            class="prof-card"
          >
            <div class="prof-avatar" :style="{ background: specialtyColor(entry.profile ? entry.profile.specialty : entry.specialty) }">
              {{ (entry.profile ? entry.profile.firstName : entry.firstName)?.charAt(0) }}
              {{ (entry.profile ? entry.profile.lastName : entry.lastName)?.charAt(0) }}
            </div>
            <div class="prof-info">
              <div class="prof-name">
                {{ entry.profile ? entry.profile.title : entry.title }}
                {{ entry.profile ? entry.profile.fullName : entry.fullName }}
              </div>
              <div class="prof-meta">
                <span class="prof-specialty" :style="{ color: specialtyColor(entry.profile ? entry.profile.specialty : entry.specialty) }">
                  {{ specialtyLabel(entry.profile ? entry.profile.specialty : entry.specialty) }}
                </span>
                <span v-if="entry.profile?.city || entry.city" class="prof-city">
                  {{ entry.profile ? entry.profile.city : entry.city }}
                </span>
              </div>
              <div v-if="entry.profile?.address || entry.address" class="prof-address">
                {{ entry.profile ? entry.profile.address : entry.address }}
              </div>
              <div v-if="entry.distanceKm !== undefined" class="prof-distance">
                {{ formatDistance(entry.distanceKm) }} away
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
