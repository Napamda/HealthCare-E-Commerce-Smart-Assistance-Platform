<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { getRecommendations } from '../services/recommendation.js'

const authStore = useAuthStore()

const CACHE_TTL_MS = 24 * 60 * 60 * 1000

const query = ref('')
const maxResults = ref(5)
const loading = ref(false)
const error = ref('')
const result = ref(null)
const resultSource = ref('')
const resultCachedAt = ref(null)
const showContext = ref(false)

const context = ref({
  symptoms: '',
  currentConditions: '',
  previousConditions: '',
  preferredCategory: '',
  allergies: '',
  recentlyBrowsedCategories: '',
})

const presets = [
  { label: 'Headache & fever', query: 'I have a headache and slight fever. What over-the-counter products should I use?' },
  { label: 'Dry skin', query: 'I have dry skin and need soothing moisturizing products.' },
  { label: 'Cold remedies', query: 'What are the common cold remedies and which products help with a cold?' },
  { label: 'Better sleep', query: 'I want products and tips to help me sleep better.' },
  { label: 'Digestion support', query: 'I often feel bloated after meals. What digestive health products can help?' },
  { label: 'Allergy relief', query: 'Seasonal allergies are bothering me. What allergy relief products do you recommend?' },
]

const countOptions = [3, 5, 8]

const contextFields = [
  { key: 'symptoms', label: 'Symptoms', placeholder: 'e.g. headache, fever, dry skin' },
  { key: 'currentConditions', label: 'Current health conditions', placeholder: 'e.g. seasonal allergies' },
  { key: 'previousConditions', label: 'Previous health conditions', placeholder: 'e.g. asthma in childhood' },
  { key: 'preferredCategory', label: 'Preferred category', placeholder: 'e.g. Pain Relief' },
  { key: 'allergies', label: 'Known allergies', placeholder: 'e.g. peanuts, latex' },
  { key: 'recentlyBrowsedCategories', label: 'Recently browsed categories', placeholder: 'e.g. Vitamins, First Aid' },
]

function cacheStorageKey() {
  return `healthcare:recommendations:${authStore.currentUser?.id || 'guest'}`
}

function readCache() {
  try {
    const raw = localStorage.getItem(cacheStorageKey())
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function writeCache(entry) {
  try {
    localStorage.setItem(cacheStorageKey(), JSON.stringify(entry))
  } catch {
    return
  }
}

function isExpired(entry) {
  return !entry.cachedAt || Date.now() - entry.cachedAt > CACHE_TTL_MS
}

function hasResults(data) {
  return !!data && ((data.products && data.products.length) || (data.healthTips && data.healthTips.length))
}

function parseList(value) {
  return String(value || '')
    .split(/[\n,]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function buildRequest() {
  return {
    query: query.value.trim(),
    symptoms: parseList(context.value.symptoms),
    currentConditions: parseList(context.value.currentConditions),
    previousConditions: parseList(context.value.previousConditions),
    preferredCategory: context.value.preferredCategory.trim(),
    allergies: parseList(context.value.allergies),
    recentlyBrowsedCategories: parseList(context.value.recentlyBrowsedCategories),
    maxResults: maxResults.value,
  }
}

function sameRequest(a, b) {
  return JSON.stringify(a) === JSON.stringify(b)
}

function restoreCachedSearch() {
  const cached = readCache()
  if (!cached || isExpired(cached) || !hasResults(cached.result)) return
  query.value = cached.request.query || ''
  maxResults.value = cached.request.maxResults || 5
  context.value.symptoms = (cached.request.symptoms || []).join(', ')
  context.value.currentConditions = (cached.request.currentConditions || []).join(', ')
  context.value.previousConditions = (cached.request.previousConditions || []).join(', ')
  context.value.preferredCategory = cached.request.preferredCategory || ''
  context.value.allergies = (cached.request.allergies || []).join(', ')
  context.value.recentlyBrowsedCategories = (cached.request.recentlyBrowsedCategories || []).join(', ')
  result.value = cached.result
  resultSource.value = 'cache'
  resultCachedAt.value = cached.cachedAt
}

function selectPreset(preset) {
  query.value = preset.query
  submit()
}

async function submit() {
  const request = buildRequest()
  if (!request.query) {
    error.value = 'Please describe what you are looking for first.'
    return
  }
  error.value = ''

  const cached = readCache()
  if (cached && !isExpired(cached) && sameRequest(cached.request, request)) {
    result.value = cached.result
    resultSource.value = 'cache'
    resultCachedAt.value = cached.cachedAt
    return
  }

  loading.value = true
  result.value = null
  resultSource.value = 'live'
  resultCachedAt.value = null
  try {
    const data = await getRecommendations(request)
    result.value = data
    if (hasResults(data)) {
      writeCache({ request, result: data, cachedAt: Date.now() })
    }
  } catch (e) {
    error.value = e.response?.data?.error || e.message || 'Failed to generate recommendations. Please try again.'
  } finally {
    loading.value = false
  }
}

function formatCachedAt(ts) {
  if (!ts) return ''
  try {
    return new Date(ts).toLocaleString()
  } catch {
    return ''
  }
}

function confidencePct(score) {
  return Math.round((score || 0) * 100)
}

function confidenceClass(score) {
  const pct = confidencePct(score)
  if (pct >= 70) return 'confidence-high'
  if (pct >= 40) return 'confidence-medium'
  return 'confidence-low'
}

function dismissError() {
  error.value = ''
}

onMounted(restoreCachedSearch)
</script>

<template>
  <div class="recommendations-page">
    <div class="page-header">
      <h1 class="page-title">AI Product Recommendations</h1>
      <p class="page-subtitle">
        Describe what you need and the AI recommends products from the HealthCare catalog plus health tips.
      </p>
    </div>

    <div class="rec-form-card">
      <div class="rec-form-row">
        <input
          v-model="query"
          type="text"
          class="rec-input"
          placeholder="e.g. I have a headache and slight fever"
          @keyup.enter="submit"
        />
        <div class="rec-count">
          <span class="rec-count-label">Results</span>
          <select v-model.number="maxResults" class="rec-select">
            <option v-for="n in countOptions" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <button class="btn btn-primary rec-submit" :disabled="loading" @click="submit">
          {{ loading ? 'Analyzing…' : 'Get recommendations' }}
        </button>
      </div>
      <div class="rec-presets">
        <span class="rec-presets-label">Try:</span>
        <button
          v-for="p in presets"
          :key="p.label"
          class="rec-chip"
          :disabled="loading"
          @click="selectPreset(p)"
        >
          {{ p.label }}
        </button>
      </div>
    </div>

    <button class="rec-context-toggle" @click="showContext = !showContext">
      <span>Health context (optional)</span>
      <span class="rec-context-arrow" :class="{ open: showContext }">&#9662;</span>
    </button>

    <div v-if="showContext" class="rec-context-card">
      <p class="rec-context-hint">
        Extra background the AI can use when recommending. These will be filled from your profile automatically once patient profiles exist.
      </p>
      <div class="rec-context-grid">
        <label v-for="f in contextFields" :key="f.key" class="rec-field">
          <span class="rec-field-label">{{ f.label }}</span>
          <input
            v-if="f.key === 'preferredCategory'"
            v-model="context[f.key]"
            type="text"
            class="rec-field-input"
            :placeholder="f.placeholder"
          />
          <textarea
            v-else
            v-model="context[f.key]"
            rows="2"
            class="rec-textarea"
            :placeholder="f.placeholder"
          ></textarea>
        </label>
      </div>
    </div>

    <div v-if="error" class="error-banner">
      <span>{{ error }}</span>
      <button class="btn-dismiss" @click="dismissError">Dismiss</button>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Analyzing your request and browsing the catalog…</p>
    </div>

    <div v-else-if="result" class="rec-results">
      <div v-if="resultSource === 'cache'" class="rec-cached-banner">
        Showing saved results from {{ formatCachedAt(resultCachedAt) }}. Run a new search to get fresh recommendations.
      </div>

      <div v-if="result.allergyWarnings && result.allergyWarnings.length" class="rec-warning-list">
        <div v-for="warning in result.allergyWarnings" :key="warning" class="rec-warning">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <span>{{ warning }}</span>
        </div>
      </div>

      <div v-if="result.reasoning" class="rec-reason">
        {{ result.reasoning }}
      </div>

      <div v-if="result.products && result.products.length" class="rec-section">
        <h2 class="rec-section-title">Recommended products</h2>
        <div class="rec-grid">
          <div v-for="p in result.products" :key="p.productId || p.productName" class="rec-card">
            <div class="rec-card-top">
              <span class="rec-card-category">{{ p.category || 'Healthcare' }}</span>
              <span class="rec-confidence" :class="confidenceClass(p.confidenceScore)">
                {{ confidencePct(p.confidenceScore) }}% match
              </span>
            </div>
            <h3 class="rec-card-name">
              <router-link v-if="p.productId" :to="'/products/' + p.productId" class="rec-card-link">
                {{ p.productName }}
              </router-link>
              <template v-else>{{ p.productName }}</template>
            </h3>
            <p class="rec-card-reason">{{ p.reason }}</p>
            <p v-if="p.description" class="rec-card-desc">{{ p.description }}</p>
            <div class="rec-card-footer">
              <div class="rec-conf-bar">
                <span class="rec-conf-fill" :style="{ width: confidencePct(p.confidenceScore) + '%' }"></span>
              </div>
              <router-link v-if="p.productId" :to="'/products/' + p.productId" class="link">
                View in Shop &rarr;
              </router-link>
              <span v-else class="rec-card-unavailable">Check catalog manually</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="result.healthTips && result.healthTips.length" class="rec-section">
        <h2 class="rec-section-title">Health tips</h2>
        <div class="rec-tips">
          <div v-for="tip in result.healthTips" :key="tip.title" class="rec-tip">
            <div class="rec-tip-title">{{ tip.title }}</div>
            <div class="rec-tip-content">{{ tip.content }}</div>
          </div>
        </div>
      </div>

      <div
        v-if="!result.products || result.products.length === 0"
        class="empty-state"
      >
        <h3>No matching products found</h3>
        <p>Try describing your need with more detail, or browse the catalog directly.</p>
        <router-link to="/products" class="btn btn-primary rec-empty-cta">Browse catalog</router-link>
      </div>
    </div>
  </div>
</template>
