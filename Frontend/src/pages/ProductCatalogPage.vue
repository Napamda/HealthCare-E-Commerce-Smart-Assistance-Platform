<script setup>
import { ref, onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useProductStore } from '../stores/product.js'
import ProductCard from '../components/product/ProductCard.vue'
import ProductFilters from '../components/product/ProductFilters.vue'

const store = useProductStore()
const {
  products,
  categories,
  categoryCounts,
  isLoading,
  error,
  pagination,
  filters,
  isLastPage,
} = storeToRefs(store)

const searchInput = ref('')
const showFilters = ref(false)

onMounted(async () => {
  await Promise.all([
    store.fetchCategories(),
    store.fetchCategoryCounts(),
    store.fetchProducts(),
  ])
})

function onSearch() {
  store.applyFilters({ keyword: searchInput.value })
}

function onFilterChange(newFilters) {
  store.applyFilters(newFilters)
}

function onClearFilters() {
  searchInput.value = ''
  store.clearFilters()
}

function onLoadMore() {
  store.loadMore()
}

function toggleFilters() {
  showFilters.value = !showFilters.value
}

// Category card — select/deselect
function selectCategory(cat) {
  const newCat = filters.value.category === cat ? '' : cat
  store.applyFilters({ category: newCat })
}

// Icons per category (inline SVG)
const categoryMeta = {
  VITAMINS:          { label: 'Vitamins',       icon: 'pill-vitamin',  color: '#f59e0b' },
  PAIN_RELIEF:       { label: 'Pain Relief',     icon: 'pain',         color: '#ef4444' },
  SKIN_CARE:         { label: 'Skin Care',       icon: 'skin',         color: '#ec4899' },
  DIGESTIVE_HEALTH:  { label: 'Digestive Health',icon: 'digestive',    color: '#10b981' },
  RESPIRATORY:       { label: 'Respiratory',     icon: 'lungs',        color: '#06b6d4' },
  HEART_HEALTH:      { label: 'Heart Health',    icon: 'heart',        color: '#dc2626' },
  DIABETES_CARE:     { label: 'Diabetes Care',   icon: 'blood',        color: '#7c3aed' },
  FIRST_AID:         { label: 'First Aid',       icon: 'cross',        color: '#0891b2' },
  MEDICAL_DEVICES:   { label: 'Devices',         icon: 'device',       color: '#6366f1' },
  PERSONAL_CARE:     { label: 'Personal Care',   icon: 'hands',        color: '#14b8a6' },
  WELLNESS:          { label: 'Wellness',        icon: 'leaf',         color: '#22c55e' },
  BABY_CARE:         { label: 'Baby Care',       icon: 'baby',         color: '#f472b6' },
  ELDERLY_CARE:      { label: 'Elderly Care',    icon: 'senior',       color: '#8b5cf6' },
  OTHER:             { label: 'Other',           icon: 'package',      color: '#6b7280' },
}

function getCategoryCount(name) {
  const found = categoryCounts.value.find(c => c.name === name)
  return found ? found.count : 0
}
</script>

<template>
  <div class="catalog-page">
    <!-- Header -->
    <div class="page-header">
      <h1 class="page-title">HealthCare Products</h1>
      <p class="page-subtitle">Browse our catalog of {{ pagination.totalElements || 0 }} trusted healthcare products</p>
    </div>

    <!-- ===================== CATEGORY BROWSER ===================== -->
    <div class="category-section">
      <div class="category-header">
        <h2 class="section-title">Browse by Category</h2>
        <button
          v-if="filters.category"
          class="btn-clear-cat"
          @click="selectCategory(filters.category)"
        >
          Clear category &times;
        </button>
      </div>
      <div class="category-scroll">
        <button
          v-for="cat in categories"
          :key="cat"
          class="category-card"
          :class="{ active: filters.category === cat }"
          :style="filters.category === cat
            ? { background: (categoryMeta[cat]?.color || '#2563eb') + '18', borderColor: categoryMeta[cat]?.color || '#2563eb' }
            : {}"
          @click="selectCategory(cat)"
        >
          <div class="cat-icon" :class="'icon-' + (categoryMeta[cat]?.icon || 'package')">
            <!-- VITAMINS — pill/capsule -->
            <svg v-if="categoryMeta[cat]?.icon === 'pill-vitamin'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="6" y="6" width="12" height="12" rx="6" />
              <line x1="12" y1="6" x2="12" y2="18" />
              <line x1="6" y1="12" x2="18" y2="12" />
            </svg>
            <!-- PAIN_RELIEF — lightning/bolt -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'pain'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
            <!-- SKIN_CARE — droplet -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'skin'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />
            </svg>
            <!-- DIGESTIVE_HEALTH — stomach/smile -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'digestive'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10" />
              <path d="M8 14s1.5 2 4 2 4-2 4-2" />
              <line x1="9" y1="9" x2="9.01" y2="9" />
              <line x1="15" y1="9" x2="15.01" y2="9" />
            </svg>
            <!-- RESPIRATORY — lungs -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'lungs'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M7 7C5.5 5 2 5.5 2 10c0 4 3 6 5 7" />
              <path d="M17 7c1.5-2 5-1.5 5 3 0 4-3 6-5 7" />
              <path d="M8 17v4M16 17v4M7 12h10" />
            </svg>
            <!-- HEART_HEALTH — heart -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'heart'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
            </svg>
            <!-- DIABETES_CARE — blood drop -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'blood'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />
              <path d="M12 8v8" />
              <circle cx="12" cy="12" r="2" />
            </svg>
            <!-- FIRST_AID — medical cross -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'cross'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="3" />
              <line x1="12" y1="8" x2="12" y2="16" />
              <line x1="8" y1="12" x2="16" y2="12" />
            </svg>
            <!-- MEDICAL_DEVICES — monitor/device -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'device'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="3" width="20" height="14" rx="2" />
              <line x1="8" y1="21" x2="16" y2="21" />
              <line x1="12" y1="17" x2="12" y2="21" />
            </svg>
            <!-- PERSONAL_CARE — hands washing -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'hands'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 11V6a2 2 0 0 0-4 0v1" />
              <path d="M8 11V6a2 2 0 0 1 4 0v5" />
              <path d="M4 9v8a4 4 0 0 0 4 4h8a4 4 0 0 0 4-4V9" />
              <path d="M4 9h16" />
            </svg>
            <!-- WELLNESS — leaf -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'leaf'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M11 20A7 7 0 0 1 9.8 6.9C15.5 4.9 17 3.5 19 2c1 2 2 4.5 2 8 0 5.5-4.78 10-10 10z" />
              <path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12" />
            </svg>
            <!-- BABY_CARE — pacifier/bib -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'baby'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="8" r="6" />
              <path d="M12 14v8M8 18h8" />
            </svg>
            <!-- ELDERLY_CARE — walking stick / senior -->
            <svg v-else-if="categoryMeta[cat]?.icon === 'senior'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="6" r="4" />
              <path d="M12 10v12M8 22l4-4 4 4" />
              <line x1="9" y1="16" x2="15" y2="16" />
            </svg>
            <!-- OTHER / default — package -->
            <svg v-else width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <line x1="16.5" y1="9.4" x2="7.5" y2="4.21" />
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
              <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
              <line x1="12" y1="22.08" x2="12" y2="12" />
            </svg>
          </div>
          <span class="cat-label">{{ categoryMeta[cat]?.label || cat }}</span>
          <span class="cat-count">{{ getCategoryCount(cat) }}</span>
        </button>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="search-bar">
      <div class="search-wrapper">
        <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          v-model="searchInput"
          type="text"
          class="search-input"
          placeholder="Search products by name, manufacturer, or ingredients..."
          @keyup.enter="onSearch"
        />
        <button v-if="searchInput" class="btn-search-clear" @click="searchInput = ''; onSearch()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
        <button class="btn-search" @click="onSearch">Search</button>
      </div>
      <button class="btn-toggle-filters" @click="toggleFilters">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="4" y1="21" x2="4" y2="14" /><line x1="4" y1="10" x2="4" y2="3" />
          <line x1="12" y1="21" x2="12" y2="12" /><line x1="12" y1="8" x2="12" y2="3" />
          <line x1="20" y1="21" x2="20" y2="16" /><line x1="20" y1="12" x2="20" y2="3" />
          <line x1="1" y1="14" x2="7" y2="14" /><line x1="9" y1="8" x2="15" y2="8" />
          <line x1="17" y1="16" x2="23" y2="16" />
        </svg>
        Filters
        <span v-if="filters.category || filters.minPrice !== null || filters.maxPrice !== null" class="filter-dot" />
      </button>
    </div>

    <!-- Active filter tags -->
    <div v-if="filters.category || filters.minPrice !== null || filters.maxPrice !== null" class="active-filters">
      <span class="filter-tag-label">Active filters:</span>
      <span v-if="filters.category" class="filter-tag">
        {{ categoryMeta[filters.category]?.label || filters.category }}
        <button @click="selectCategory(filters.category)">&times;</button>
      </span>
      <span v-if="filters.minPrice !== null || filters.maxPrice !== null" class="filter-tag">
        ${{ filters.minPrice || 0 }} — ${{ filters.maxPrice || '∞' }}
        <button @click="store.applyFilters({ minPrice: null, maxPrice: null })">&times;</button>
      </span>
      <button class="btn-clear-all-tags" @click="onClearFilters">Clear all</button>
    </div>

    <!-- Main content -->
    <div class="catalog-body">
      <!-- Filters sidebar -->
      <ProductFilters
        v-if="showFilters"
        v-model="filters"
        :categories="categories"
        @search="onFilterChange"
        @clear="onClearFilters"
      />

      <!-- Product grid -->
      <div class="products-area">
        <!-- Error -->
        <div v-if="error" class="error-banner">
          <span>{{ error }}</span>
          <button class="btn-dismiss" @click="store.clearError()">Dismiss</button>
        </div>

        <!-- Loading -->
        <div v-if="isLoading && products.length === 0" class="loading-state">
          <div class="dot-typing"><span></span><span></span><span></span></div>
          <p>Loading products...</p>
        </div>

        <!-- Empty -->
        <div v-if="!isLoading && products.length === 0 && !error" class="empty-state">
          <div class="empty-icon">
            <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </div>
          <h2>No products found</h2>
          <p>Try adjusting your search or filter criteria</p>
          <button class="btn-clear-link" @click="onClearFilters">Clear all filters</button>
        </div>

        <!-- Grid -->
        <div v-if="products.length > 0" class="product-grid">
          <ProductCard
            v-for="product in products"
            :key="product.id"
            :product="product"
          />
        </div>

        <!-- Load more -->
        <div v-if="products.length > 0 && !isLastPage" class="load-more">
          <button class="btn-load-more" :disabled="isLoading" @click="onLoadMore">
            <span v-if="isLoading" class="spinner" />
            <span v-else>Load More Products</span>
          </button>
        </div>

        <!-- Count -->
        <div v-if="products.length > 0" class="results-count">
          Showing {{ products.length }} of {{ pagination.totalElements }} products
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.catalog-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 32px 24px 64px;
}

.page-header {
  max-width: 1200px;
  margin: 0 auto 20px;
}
.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}
.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
}

/* ==================== CATEGORY SECTION ==================== */
.category-section {
  max-width: 1200px;
  margin: 0 auto 24px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
}

.category-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
}

.btn-clear-cat {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
  padding: 4px 12px;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  transition: all 0.15s;
}
.btn-clear-cat:hover {
  color: var(--color-danger);
  border-color: var(--color-danger);
  background: #fef2f2;
}

.category-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
  scroll-snap-type: x proximity;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.category-scroll::-webkit-scrollbar {
  display: none;
}

.category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  min-width: 100px;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  border: 2px solid transparent;
  background: var(--color-bg);
  cursor: pointer;
  transition: all 0.2s ease;
  scroll-snap-align: start;
  position: relative;
}
.category-card:hover {
  border-color: var(--color-primary-light);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}
.category-card.active {
  border-width: 2px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.cat-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-primary);
  transition: all 0.2s;
}
.category-card.active .cat-icon {
  color: currentColor;
}

.cat-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text);
  text-align: center;
  line-height: 1.3;
  white-space: nowrap;
}

.cat-count {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-text-muted);
  background: var(--color-bg);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.category-card.active .cat-count {
  background: rgba(255,255,255,0.3);
  color: inherit;
}

/* ==================== ACTIVE FILTER TAGS ==================== */
.active-filters {
  max-width: 1200px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tag-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  background: var(--color-primary-bg);
  color: var(--color-primary);
  border: 1px solid var(--color-primary-light);
}
.filter-tag button {
  font-size: 14px;
  color: inherit;
  opacity: 0.6;
  transition: opacity 0.1s;
  line-height: 1;
}
.filter-tag button:hover {
  opacity: 1;
}

.btn-clear-all-tags {
  font-size: 12px;
  color: var(--color-text-muted);
  text-decoration: underline;
  transition: color 0.15s;
}
.btn-clear-all-tags:hover {
  color: var(--color-danger);
}

/* ==================== SEARCH ==================== */
.search-bar {
  max-width: 1200px;
  margin: 0 auto 16px;
  display: flex;
  gap: 12px;
}
.search-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 0 16px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search-wrapper:focus-within {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
.search-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 14px 12px;
  font-size: 14px;
  background: transparent;
}
.btn-search-clear {
  padding: 4px;
  color: var(--color-text-muted);
  border-radius: var(--radius-full);
  transition: color 0.15s;
}
.btn-search-clear:hover {
  color: var(--color-text);
}
.btn-search {
  padding: 8px 20px;
  margin: 6px 0;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.15s;
}
.btn-search:hover {
  background: var(--color-primary-dark);
}
.btn-toggle-filters {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 20px;
  border-radius: var(--radius-lg);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  transition: all 0.15s;
  position: relative;
}
.btn-toggle-filters:hover {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
}
.filter-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  position: absolute;
  top: -3px;
  right: -3px;
}

/* ==================== BODY ==================== */
.catalog-body {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.products-area {
  flex: 1;
  min-width: 0;
}

/* Error */
.error-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fef2f2;
  color: var(--color-danger);
  border: 1px solid #fecaca;
  border-radius: var(--radius-md);
  font-size: 13px;
  margin-bottom: 16px;
}
.btn-dismiss {
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  background: var(--color-danger);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}

/* Loading / Empty */
.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--color-text-secondary);
}
.empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 16px;
}
.empty-state h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}
.btn-clear-link {
  margin-top: 12px;
  padding: 8px 18px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary);
  background: var(--color-primary-bg);
  transition: background 0.15s;
}
.btn-clear-link:hover {
  background: #dbeafe;
}

/* Grid */
.product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
@media (max-width: 900px) {
  .product-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .product-grid { grid-template-columns: 1fr; }
  .catalog-body { flex-direction: column; }
}

/* Load more */
.load-more {
  display: flex;
  justify-content: center;
  padding: 24px 0 16px;
}
.btn-load-more {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary-light);
  transition: all 0.15s;
}
.btn-load-more:hover:not(:disabled) {
  background: #dbeafe;
}
.btn-load-more:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.results-count {
  text-align: center;
  font-size: 12px;
  color: var(--color-text-muted);
  padding-bottom: 16px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(37, 99, 235, 0.3);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Loading dots */
.dot-typing {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}
.dot-typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: dotPulse 1.4s infinite ease-in-out both;
}
.dot-typing span:nth-child(1) { animation-delay: -0.32s; }
.dot-typing span:nth-child(2) { animation-delay: -0.16s; }
@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}
</style>
