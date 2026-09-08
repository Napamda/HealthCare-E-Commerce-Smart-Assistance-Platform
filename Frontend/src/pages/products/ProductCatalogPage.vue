<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useProductStore } from '../../stores/product.js'
import { useCategoryStore } from '../../stores/category.js'
import { categoryToFilterValue, flattenCategoryTree } from '../../services/category.js'
import ProductCard from '../../components/product/ProductCard.vue'
import ProductFilters from '../../components/product/ProductFilters.vue'
import CategoryTree from '../../components/category/CategoryTree.vue'
import { generateCategorySvg } from '../../utils/categoryIllustrations.js'

const store = useProductStore()
const categoryStore = useCategoryStore()
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
const sortValue = ref('newest')
const heroVisible = ref(false)
const heroIndex = ref(0)
const categoryScrollEl = ref(null)
let heroTimer = null

const heroImages = [
  '/images/hero/hero-1.jpg',
  '/images/hero/hero-2.jpg',
  '/images/hero/hero-3.jpg',
]

function startHeroCarousel() {
  heroTimer = setInterval(() => {
    heroIndex.value = (heroIndex.value + 1) % heroImages.length
  }, 5000)
}

function stopHeroCarousel() {
  if (heroTimer) { clearInterval(heroTimer); heroTimer = null }
}

// Keep the local sort select in sync with the store (e.g. after clearing filters)
watch(
  () => filters.value.sort,
  (val) => {
    sortValue.value = val || 'newest'
  },
)

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

const categoryIcons = {
  VITAMINS: '💊', PAIN_RELIEF: '💉', SKIN_CARE: '🧴',
  DIGESTIVE_HEALTH: '🫄', RESPIRATORY: '🫁', HEART_HEALTH: '❤️',
  DIABETES_CARE: '🩸', FIRST_AID: '🩹', MEDICAL_DEVICES: '🔬',
  PERSONAL_CARE: '🧼', WELLNESS: '🧘', BABY_CARE: '🍼',
  ELDERLY_CARE: '👴', OTHER: '📦',
}

const categoryColors = {
  VITAMINS: '#f59e0b', PAIN_RELIEF: '#ef4444', SKIN_CARE: '#ec4899',
  DIGESTIVE_HEALTH: '#10b981', RESPIRATORY: '#06b6d4', HEART_HEALTH: '#dc2626',
  DIABETES_CARE: '#7c3aed', FIRST_AID: '#0891b2', MEDICAL_DEVICES: '#6366f1',
  PERSONAL_CARE: '#14b8a6', WELLNESS: '#22c55e', BABY_CARE: '#f472b6',
  ELDERLY_CARE: '#8b5cf6', OTHER: '#6b7280',
}

function categoryLabel(cat) {
  const labels = {
    VITAMINS: 'Vitamins', PAIN_RELIEF: 'Pain Relief', SKIN_CARE: 'Skin Care',
    DIGESTIVE_HEALTH: 'Digestive Health', RESPIRATORY: 'Respiratory', HEART_HEALTH: 'Heart Health',
    DIABETES_CARE: 'Diabetes Care', FIRST_AID: 'First Aid', MEDICAL_DEVICES: 'Medical Devices',
    PERSONAL_CARE: 'Personal Care', WELLNESS: 'Wellness', BABY_CARE: 'Baby Care',
    ELDERLY_CARE: 'Elderly Care', OTHER: 'Other',
  }
  return labels[cat] || cat
}

function getCategorySvg(cat) {
  return generateCategorySvg(cat)
}

const hasTreeCategories = computed(() => categoryStore.tree.length > 0)

const treeIcons = computed(() =>
  Object.fromEntries(
    Object.entries(categoryMeta).map(([key, meta]) => [
      key,
      { label: meta.label, color: meta.color },
    ]),
  ),
)

// name of the currently selected category (from tree, else legacy meta)
const selectedCategoryLabel = computed(() => {
  const flat = flattenCategoryTree(categoryStore.tree)
  const match = flat.find((n) => categoryToFilterValue(n.name) === filters.value.category)
  if (match) return match.name
  return categoryMeta[filters.value.category]?.label || filters.value.category
})

onMounted(async () => {
  await Promise.all([
    store.fetchCategories(),
    store.fetchCategoryCounts(),
    categoryStore.fetchTree(),
    store.fetchProducts(),
  ])
  setTimeout(() => { heroVisible.value = true }, 100)
  startHeroCarousel()
})

function scrollToSection(refName) {
  const el = document.querySelector(refName)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// Browse by Category — make the chip row scrollable in both directions
function scrollCategories(direction) {
  const el = categoryScrollEl.value
  if (!el) return
  const amount = direction * Math.max(280, Math.round(el.clientWidth * 0.6))
  el.scrollBy({ left: amount, behavior: 'smooth' })
}

function onCategoryWheel(event) {
  const el = categoryScrollEl.value
  if (!el || el.scrollWidth <= el.clientWidth + 1) return
  // Let a vertical wheel scroll the category row horizontally (mouse-friendly).
  if (Math.abs(event.deltaY) > Math.abs(event.deltaX)) {
    el.scrollLeft += event.deltaY
    event.preventDefault()
  }
}

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

// Category tree — select ('' clears the filter)
function onTreeSelect(value) {
  store.applyFilters({ category: value })
}

function getCategoryCount(name) {
  const found = categoryCounts.value.find(c => c.name === name)
  return found ? found.count : 0
}

function onSortChange() {
  store.applyFilters({ sort: sortValue.value })
}
</script>

<template>
  <div class="catalog-page">
    <!-- Page background mesh decoration -->
    <div class="page-bg">
      <span class="bg-blob bg-blob-1" />
      <span class="bg-blob bg-blob-2" />
      <span class="bg-blob bg-blob-3" />
    </div>

    <!-- Hero Banner with Image Carousel -->
    <div
      class="hero-banner"
      :class="{ visible: heroVisible }"
      @mouseenter="stopHeroCarousel"
      @mouseleave="startHeroCarousel"
    >
      <div class="hero-bg-layers">
        <TransitionGroup name="hero-fade">
          <div
            v-for="(img, i) in heroImages"
            :key="img"
            class="hero-bg"
            :class="{ active: i === heroIndex }"
            :style="{ backgroundImage: `url(${img})` }"
          />
        </TransitionGroup>
      </div>
      <div class="hero-overlay" />
      <div class="hero-shapes">
        <span class="shape shape-1">💊</span>
        <span class="shape shape-2">❤️</span>
        <span class="shape shape-3">🩹</span>
        <span class="shape shape-4">🧴</span>
        <span class="shape shape-5">🩺</span>
      </div>
      <div class="hero-content">
        <span class="hero-tag">🏥 Trusted Healthcare</span>
        <h1 class="hero-title">Your Health,<br/>Our Priority</h1>
        <p class="hero-subtitle">
          Browse {{ pagination.totalElements || 0 }}+ quality healthcare products across {{ categories.length }} categories. Fast delivery, AI-powered recommendations, and 24/7 support.
        </p>
        <div class="hero-actions">
          <button class="btn-hero-primary" @click="scrollToSection('.products-area')">
            Shop Now →
          </button>
          <button class="btn-hero-secondary" @click="scrollToSection('.category-section')">
            Browse Categories
          </button>
        </div>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="stat-number">{{ pagination.totalElements || 0 }}+</span>
            <span class="stat-label">Products</span>
          </div>
          <div class="hero-stat">
            <span class="stat-number">{{ categories.length }}</span>
            <span class="stat-label">Categories</span>
          </div>
          <div class="hero-stat">
            <span class="stat-number">24/7</span>
            <span class="stat-label">Support</span>
          </div>
        </div>
        <div class="hero-trust">
          <div class="trust-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            <span>Verified Products</span>
          </div>
          <div class="trust-divider" />
          <div class="trust-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
            <span>Fast Delivery</span>
          </div>
          <div class="trust-divider" />
          <div class="trust-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
            <span>24/7 Support</span>
          </div>
          <div class="trust-divider" />
          <div class="trust-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            <span>Secure Checkout</span>
          </div>
        </div>
      </div>
      <div class="hero-dots">
        <button
          v-for="(img, i) in heroImages"
          :key="'dot-' + i"
          class="hero-dot"
          :class="{ active: i === heroIndex }"
          @click="heroIndex = i"
        />
      </div>
    </div>

    <!-- Category Browser -->
    <div class="category-section">
      <div class="section-header">
        <h2 class="section-title">Browse by Category</h2>
        <div class="category-scroll-controls">
          <button
            v-if="filters.category"
            class="btn-link"
            @click="selectCategory(filters.category)"
          >
            Clear category
          </button>
          <button
            type="button"
            class="cat-scroll-btn"
            aria-label="Scroll categories left"
            @click="scrollCategories(-1)"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </button>
          <button
            type="button"
            class="cat-scroll-btn"
            aria-label="Scroll categories right"
            @click="scrollCategories(1)"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </button>
        </div>
      </div>
      <div
        ref="categoryScrollEl"
        class="category-scroll"
        @wheel="onCategoryWheel"
      >
        <button
          v-for="cat in categories"
          :key="cat"
          class="category-chip"
          :class="{ active: filters.category === cat }"
          :style="{ '--cat-color': categoryColors[cat] || '#6b7280' }"
          @click="selectCategory(cat)"
        >
          <span class="cat-icon">{{ categoryIcons[cat] || '📦' }}</span>
          <span class="cat-label">{{ categoryLabel(cat) }}</span>
          <span v-if="categoryCounts.find(c => c.name === cat)" class="cat-count">
            {{ categoryCounts.find(c => c.name === cat).count }}
          </span>
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
        {{ selectedCategoryLabel }}
        <button @click="onTreeSelect('')">&times;</button>
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

        <!-- Results toolbar -->
        <div v-if="products.length > 0" class="results-toolbar">
          <div class="results-count">
            {{ products.length }} of {{ pagination.totalElements }} products
          </div>
          <label class="sort-control">
            <span>Sort by</span>
            <select v-model="sortValue" class="sort-select" @change="onSortChange">
              <option value="newest">Newest</option>
              <option value="price_asc">Price: Low to High</option>
              <option value="price_desc">Price: High to Low</option>
              <option value="name_asc">Name: A to Z</option>
              <option value="name_desc">Name: Z to A</option>
              <option value="popular">Most Popular</option>
              <option value="stock_asc">Low Stock First</option>
            </select>
          </label>
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

      </div>
    </div>
  </div>
</template>
