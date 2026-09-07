<script setup>
import { ref, onMounted, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useProductStore } from '../stores/product.js'
import ProductCard from '../components/product/ProductCard.vue'
import ProductFilters from '../components/product/ProductFilters.vue'

const store = useProductStore()
const {
  products, categories, categoryCounts, isLoading, error, pagination, filters, isLastPage,
} = storeToRefs(store)

const searchInput = ref('')
const showFilters = ref(false)
const featuredProducts = ref([])
const trendingProducts = ref([])
const quickViewProduct = ref(null)
const heroVisible = ref(false)
const heroIndex = ref(0)
let heroTimer = null

const heroImages = [
  '/images/hero/hero-1.jpg',
  '/images/hero/hero-2.jpg',
  '/images/hero/hero-3.jpg',
  '/images/hero/hero-4.jpg',
  '/images/hero/hero-5.jpg',
  '/images/hero/hero-6.jpg',
]

function startHeroCarousel() {
  heroTimer = setInterval(() => {
    heroIndex.value = (heroIndex.value + 1) % heroImages.length
  }, 5000)
}
function stopHeroCarousel() {
  if (heroTimer) { clearInterval(heroTimer); heroTimer = null }
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

onMounted(async () => {
  await Promise.all([
    store.fetchCategories(),
    store.fetchCategoryCounts(),
    store.fetchProducts(),
  ])
  if (store.fetchFeaturedProducts) {
    store.fetchFeaturedProducts().then(d => { featuredProducts.value = d })
  }
  if (store.fetchTopRatedProducts) {
    store.fetchTopRatedProducts(8).then(d => { trendingProducts.value = d })
  }
  setTimeout(() => { heroVisible.value = true }, 100)
  startHeroCarousel()
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

function selectCategory(cat) {
  const newCat = filters.value.category === cat ? '' : cat
  store.applyFilters({ category: newCat })
}

function openQuickView(product) { quickViewProduct.value = product }
function closeQuickView() { quickViewProduct.value = null }

function scrollToSection(refName) {
  const el = document.querySelector(refName)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
      <!-- Background Image Layers (crossfade carousel) -->
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

      <!-- Gradient Overlay for readability -->
      <div class="hero-overlay" />

      <!-- Animated Floating Shapes -->
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
          <button class="btn-hero-primary" @click="scrollToSection('.products-section')">
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

        <!-- Trust Badges Strip -->
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

      <!-- Carousel Dots -->
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

    <!-- Featured Products -->
    <div v-if="featuredProducts.length > 0" class="section-wrap">
      <div class="section-header">
        <h2 class="section-title">⭐ Featured Products</h2>
        <span class="section-subtitle">Handpicked for you</span>
      </div>
      <div class="featured-grid">
        <ProductCard
          v-for="product in featuredProducts.slice(0, 4)"
          :key="'feat-' + product.id"
          :product="product"
          @quick-view="openQuickView"
        />
      </div>
    </div>

    <!-- Trending Products -->
    <div v-if="trendingProducts.length > 0" class="section-wrap">
      <div class="section-header">
        <h2 class="section-title">🔥 Trending Now</h2>
        <span class="section-subtitle">Most popular this week</span>
      </div>
      <div class="trending-scroll">
        <ProductCard
          v-for="product in trendingProducts"
          :key="'trend-' + product.id"
          :product="product"
          class="trending-card"
          @quick-view="openQuickView"
        />
      </div>
    </div>

    <!-- Category Browser -->
    <div class="category-section">
      <div class="section-header">
        <h2 class="section-title">Browse by Category</h2>
        <button
          v-if="filters.category"
          class="btn-link"
          @click="selectCategory(filters.category)"
        >
          Clear category
        </button>
      </div>
      <div class="category-scroll">
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
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.3-4.3" />
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
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
      <button class="btn-secondary" @click="toggleFilters">
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
      <span class="filter-tag-label">Active:</span>
      <span v-if="filters.category" class="filter-tag">
        {{ categoryLabel(filters.category) }}
        <button @click="selectCategory(filters.category)">&times;</button>
      </span>
      <span v-if="filters.minPrice !== null || filters.maxPrice !== null" class="filter-tag">
        ${{ filters.minPrice || 0 }} — ${{ filters.maxPrice || '∞' }}
        <button @click="store.applyFilters({ minPrice: null, maxPrice: null })">&times;</button>
      </span>
      <button class="btn-link" @click="onClearFilters">Clear all</button>
    </div>

    <!-- Main Content -->
    <div class="catalog-body">
      <ProductFilters
        v-if="showFilters"
        v-model="filters"
        :categories="categories"
        @search="onFilterChange"
        @clear="onClearFilters"
      />

      <div class="products-area products-section">
        <div v-if="isLoading" class="loading-state">
          <div class="dot-typing"><span></span><span></span><span></span></div>
          <p>Loading products...</p>
        </div>

        <div v-else-if="error" class="alert alert-error">
          <p>{{ error }}</p>
          <button class="btn-link" @click="store.fetchProducts()">Retry</button>
        </div>

        <div v-else-if="products.length === 0" class="empty-state">
          <p>No products found matching your criteria.</p>
          <button class="btn-link" @click="onClearFilters">Clear filters</button>
        </div>

        <div v-else class="products-grid">
          <ProductCard
            v-for="(product, idx) in products"
            :key="product.id"
            :product="product"
            :index="idx"
            @quick-view="openQuickView"
          />
        </div>

        <div v-if="!isLastPage && products.length > 0" class="load-more">
          <button class="btn-primary" :disabled="isLoading" @click="onLoadMore">
            {{ isLoading ? 'Loading...' : 'Load More Products' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Quick View Modal -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="quickViewProduct" class="modal-backdrop" @click.self="closeQuickView">
          <div class="modal-content">
            <button class="modal-close" @click="closeQuickView">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
            <div class="modal-body">
              <div class="modal-image">
                <img v-if="quickViewProduct.imageUrl" :src="quickViewProduct.imageUrl" :alt="quickViewProduct.name" />
                <div v-else class="image-placeholder">
                  <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" />
                  </svg>
                </div>
              </div>
              <div class="modal-info">
                <span class="modal-category" :style="{ color: categoryColors[quickViewProduct.category] || '#6b7280' }">
                  {{ categoryIcons[quickViewProduct.category] || '📦' }} {{ categoryLabel(quickViewProduct.category) }}
                </span>
                <h2 class="modal-name">{{ quickViewProduct.name }}</h2>
                <p v-if="quickViewProduct.manufacturer" class="modal-manufacturer">{{ quickViewProduct.manufacturer }}</p>
                <p v-if="quickViewProduct.description" class="modal-description">{{ quickViewProduct.description }}</p>
                <div class="modal-price-row">
                  <span class="modal-price">{{ formatPrice(quickViewProduct.price) }}</span>
                  <span v-if="quickViewProduct.originalPrice > quickViewProduct.price" class="modal-original-price">
                    {{ formatPrice(quickViewProduct.originalPrice) }}
                  </span>
                </div>
                <button class="btn-primary modal-btn" @click="closeQuickView(); $router.push('/products/' + quickViewProduct.id)">
                  View Full Details →
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ==================== HERO BANNER ==================== */
.hero-banner {
  border-radius: var(--radius-xl);
  padding: 56px 48px;
  margin-bottom: 32px;
  position: relative;
  overflow: hidden;
  min-height: 380px;
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.7s ease, transform 0.7s ease;
  isolation: isolate;
}
.hero-banner.visible {
  opacity: 1;
  transform: translateY(0);
}

/* Background image layers */
.hero-bg-layers {
  position: absolute;
  inset: 0;
  z-index: -2;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  opacity: 0;
  transition: opacity 1.2s ease-in-out, transform 8s ease-out;
  transform: scale(1.05);
}
.hero-bg.active {
  opacity: 1;
  transform: scale(1);
}

/* Gradient overlay for text readability */
.hero-overlay {
  position: absolute;
  inset: 0;
  z-index: -1;
  background: linear-gradient(
    135deg,
    rgba(15, 40, 120, 0.85) 0%,
    rgba(30, 80, 180, 0.75) 50%,
    rgba(6, 182, 212, 0.65) 100%
  );
}

/* Animated floating shapes */
.hero-shapes {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
.shape {
  position: absolute;
  font-size: 32px;
  opacity: 0.25;
  animation: floatShape 12s ease-in-out infinite;
}
.shape-1 { top: 15%; right: 8%; font-size: 48px; animation-delay: 0s; }
.shape-2 { top: 55%; right: 22%; font-size: 38px; animation-delay: -2s; animation-duration: 14s; }
.shape-3 { bottom: 20%; right: 5%; font-size: 42px; animation-delay: -4s; }
.shape-4 { top: 30%; right: 35%; font-size: 28px; animation-delay: -6s; animation-duration: 10s; }
.shape-5 { bottom: 35%; right: 38%; font-size: 36px; animation-delay: -8s; animation-duration: 16s; }

@keyframes floatShape {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(15px, -20px) rotate(8deg);
  }
  50% {
    transform: translate(-10px, -40px) rotate(-5deg);
  }
  75% {
    transform: translate(20px, -15px) rotate(10deg);
  }
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 620px;
}

.hero-tag {
  display: inline-block;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 20px;
  backdrop-filter: blur(8px);
  animation: fadeSlideDown 0.8s ease 0.2s both;
}

.hero-title {
  font-size: 48px;
  font-weight: 800;
  color: #fff;
  line-height: 1.1;
  margin: 0 0 14px;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.3);
  animation: fadeSlideUp 0.8s ease 0.35s both;
}

.hero-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.55;
  margin: 0 0 28px;
  animation: fadeSlideUp 0.8s ease 0.5s both;
}

.hero-actions {
  display: flex;
  gap: 14px;
  margin-bottom: 36px;
  animation: fadeSlideUp 0.8s ease 0.65s both;
}

.btn-hero-primary {
  padding: 14px 32px;
  background: #fff;
  color: #1e40af;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.25s cubic-bezier(0.22, 0.61, 0.36, 1), box-shadow 0.25s ease;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
}
.btn-hero-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.22);
}
.btn-hero-primary:active {
  transform: translateY(-1px);
}

.btn-hero-secondary {
  padding: 14px 32px;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  border: 1.5px solid rgba(255, 255, 255, 0.4);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.25s ease, border-color 0.25s ease, transform 0.25s ease;
  backdrop-filter: blur(4px);
}
.btn-hero-secondary:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.65);
  transform: translateY(-2px);
}

.hero-stats {
  display: flex;
  gap: 40px;
  animation: fadeSlideUp 0.8s ease 0.8s both;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  padding: 12px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.25);
  min-width: 100px;
}

.stat-number {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  line-height: 1.1;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.5px;
  text-transform: uppercase;
  margin-top: 2px;
}

/* Carousel dots */
.hero-dots {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 2;
}
.hero-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  border: none;
  cursor: pointer;
  transition: width 0.3s ease, background 0.3s ease;
}
.hero-dot.active {
  width: 24px;
  border-radius: 4px;
  background: #fff;
}
.hero-dot:hover {
  background: rgba(255, 255, 255, 0.75);
}

/* Entrance animations */
@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes fadeSlideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ==================== PAGE BACKGROUND ==================== */
.catalog-page {
  position: relative;
}
.page-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}
.bg-blob-1 {
  width: 500px; height: 500px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.12) 0%, transparent 70%);
  top: -150px; left: -150px;
  animation: bgFloat1 18s ease-in-out infinite;
}
.bg-blob-2 {
  width: 400px; height: 400px;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.10) 0%, transparent 70%);
  top: 40%; right: -120px;
  animation: bgFloat2 22s ease-in-out infinite;
}
.bg-blob-3 {
  width: 450px; height: 450px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.08) 0%, transparent 70%);
  bottom: -150px; left: 30%;
  animation: bgFloat3 25s ease-in-out infinite;
}
@keyframes bgFloat1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(60px, 30px); }
}
@keyframes bgFloat2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-50px, -40px); }
}
@keyframes bgFloat3 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, -60px); }
}
.catalog-page > *:not(.page-bg) {
  position: relative;
  z-index: 1;
}

/* ==================== HERO TRUST BADGES ==================== */
.hero-trust {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  animation: fadeSlideUp 0.8s ease 0.95s both;
}
.trust-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  font-weight: 600;
}
.trust-item svg {
  color: rgba(255, 255, 255, 0.95);
}
.trust-divider {
  width: 1px;
  height: 14px;
  background: rgba(255, 255, 255, 0.25);
}

/* ==================== SECTIONS ==================== */
.section-wrap {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-text);
  margin: 0;
  position: relative;
  padding-left: 16px;
  letter-spacing: -0.02em;
}
.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 24px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--color-primary) 0%, #06b6d4 100%);
  animation: barPulse 3s ease-in-out infinite;
}

@keyframes barPulse {
  0%, 100% { opacity: 1; transform: translateY(-50%) scaleY(1); }
  50% { opacity: 0.7; transform: translateY(-50%) scaleY(0.7); }
}

.section-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.trending-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding: 4px 4px 12px;
  scroll-snap-type: x mandatory;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.trending-scroll::-webkit-scrollbar { display: none; }

.trending-card {
  flex: 0 0 270px;
  scroll-snap-align: start;
}

/* ==================== CATEGORY SECTION ==================== */
.category-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.category-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding: 4px 2px 8px;
  scroll-snap-type: x mandatory;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.category-scroll::-webkit-scrollbar { display: none; }

.category-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding: 16px 20px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
  min-width: 110px;
  scroll-snap-align: start;
  position: relative;
  overflow: hidden;
}
.category-chip::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--cat-color, var(--color-primary)) 0%, transparent 60%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.category-chip:hover {
  border-color: var(--cat-color, var(--color-primary-light));
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
}
.category-chip:hover::before { opacity: 0.05; }
.category-chip.active {
  border-color: var(--cat-color, var(--color-primary));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--cat-color, var(--color-primary)) 20%, transparent),
              0 8px 24px rgba(0, 0, 0, 0.08);
  background: color-mix(in srgb, var(--cat-color, var(--color-primary)) 10%, var(--color-surface));
}
.category-chip.active::before { opacity: 0.08; }

.cat-icon {
  font-size: 32px;
  line-height: 1;
  transition: transform 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
  position: relative;
  z-index: 1;
}
.category-chip:hover .cat-icon,
.category-chip.active .cat-icon {
  transform: scale(1.15);
}

.cat-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-text-secondary);
  text-align: center;
  white-space: nowrap;
  position: relative;
  z-index: 1;
  letter-spacing: 0.3px;
}
.category-chip.active .cat-label {
  color: var(--color-text);
}

.cat-count {
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: var(--cat-color, var(--color-primary));
  padding: 2px 8px;
  border-radius: var(--radius-full);
  min-width: 24px;
  text-align: center;
  position: relative;
  z-index: 1;
  box-shadow: 0 2px 8px color-mix(in srgb, var(--cat-color, var(--color-primary)) 40%, transparent);
}

/* ==================== SEARCH BAR ==================== */
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.search-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 0 16px;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}
.search-wrapper:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08),
              0 2px 8px rgba(0, 0, 0, 0.04);
}
.search-wrapper:focus-within svg:first-child {
  color: var(--color-primary);
  transform: scale(1.1);
}
.search-wrapper svg:first-child {
  color: var(--color-text-muted);
  transition: color 0.2s ease, transform 0.2s ease;
}

.search-input {
  flex: 1;
  padding: 10px 0;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--color-text);
  outline: none;
}
.search-input::placeholder {
  color: var(--color-text-muted);
}

.btn-search-clear {
  padding: 4px;
  color: var(--color-text-muted);
  border-radius: 50%;
  transition: color 0.15s;
  background: none;
  border: none;
  cursor: pointer;
}
.btn-search-clear:hover { color: var(--color-text); }

.btn-secondary {
  padding: 10px 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s;
  position: relative;
}
.btn-secondary:hover {
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

/* ==================== ACTIVE FILTER TAGS ==================== */
.active-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
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
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}
.filter-tag button:hover { opacity: 1; }

.btn-link {
  padding: 6px 10px;
  border: none;
  background: transparent;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}
.btn-link:hover { background: var(--color-primary-bg); }

/* ==================== CATALOG BODY ==================== */
.catalog-body {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.products-area {
  flex: 1;
  min-width: 0;
}

.alert {
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 14px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.alert-error {
  background: rgba(220, 38, 38, 0.08);
  color: #dc2626;
  border: 1px solid rgba(220, 38, 38, 0.2);
}

.loading-state,
.empty-state {
  padding: 48px 0;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 14px;
}

.dot-typing {
  display: flex;
  gap: 6px;
  justify-content: center;
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

.products-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 24px 0 16px;
}

.btn-primary {
  padding: 10px 18px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-dark);
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ==================== QUICK VIEW MODAL ==================== */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  max-width: 720px;
  width: 90%;
  max-height: 85vh;
  overflow-y: auto;
  position: relative;
}

.modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: background 0.15s;
}
.modal-close:hover { background: var(--color-border); }

.modal-body {
  display: flex;
  gap: 24px;
  padding: 28px;
}

.modal-image {
  flex: 0 0 280px;
  height: 280px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.modal-image .image-placeholder {
  opacity: 0.3;
}

.modal-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.modal-category {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.modal-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 6px;
}

.modal-manufacturer {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0 0 12px;
}

.modal-description {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin: 0 0 16px;
  flex: 1;
}

.modal-price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
}

.modal-price {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
}

.modal-original-price {
  font-size: 16px;
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.modal-btn {
  width: 100%;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

/* ==================== RESPONSIVE ==================== */
@media (max-width: 900px) {
  .hero-banner { padding: 36px 28px; min-height: 320px; }
  .hero-title { font-size: 32px; }
  .hero-stats { gap: 24px; }
  .stat-number { font-size: 24px; }
  .shape-1, .shape-3 { display: none; }
  .hero-trust { flex-wrap: wrap; gap: 10px; }
  .trust-divider:nth-of-type(even) { display: none; }
  .featured-grid { grid-template-columns: repeat(2, 1fr); }
  .products-grid { grid-template-columns: repeat(2, 1fr); }
  .modal-body { flex-direction: column; }
  .modal-image { flex: 0 0 200px; height: 200px; }
}

@media (max-width: 600px) {
  .hero-banner { padding: 28px 18px; min-height: 300px; }
  .hero-title { font-size: 24px; }
  .hero-subtitle { font-size: 14px; }
  .hero-actions { flex-direction: column; }
  .btn-hero-primary, .btn-hero-secondary { width: 100%; }
  .hero-stats { display: none; }
  .hero-trust { display: none; }
  .hero-dots { bottom: 10px; }
  .shape { display: none; }
  .section-title { font-size: 18px; }
  .cat-icon { font-size: 26px; }
  .category-chip { padding: 14px 14px; min-width: 96px; }
  .trending-card { flex: 0 0 240px; }
  .featured-grid { grid-template-columns: 1fr; }
  .products-grid { grid-template-columns: 1fr; }
  .catalog-body { flex-direction: column; }
  .catalog-page .bg-blob-3 { display: none; }
}
</style>