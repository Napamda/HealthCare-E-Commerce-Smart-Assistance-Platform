<script setup>
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useRecommendationStore } from '../stores/recommendation.js'
import { useProductStore } from '../stores/product.js'
import ProductCard from '../components/product/ProductCard.vue'

const router = useRouter()
const recommendationStore = useRecommendationStore()
const productStore = useProductStore()

const personalized = computed(() => recommendationStore.personalized)
const loading = computed(() => recommendationStore.loading)

function formatPrice(price) {
  if (price == null) return '$0.00'
  return '$' + Number(price).toFixed(2)
}

function goToProduct(productId) {
  router.push(`/products/${productId}`)
}

function goToCatalog() {
  router.push('/products')
}

async function loadRecommendations() {
  await recommendationStore.fetchPersonalized()
}

onMounted(() => {
  loadRecommendations()
})
</script>

<template>
  <div class="recommendations-page">
    <div class="page-header">
      <div class="header-content">
        <h1>Recommended for You</h1>
        <p class="header-subtitle">Personalized healthcare product recommendations based on your needs</p>
      </div>
      <button class="btn-refresh" @click="loadRecommendations" title="Refresh recommendations">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M23 4v6h-6" />
          <path d="M1 20v-6h6" />
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
        </svg>
        Refresh
      </button>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner">
        <svg class="spinner" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
      </div>
      <p>Loading recommendations...</p>
    </div>

    <div v-else-if="personalized.length === 0" class="empty-state">
      <div class="empty-illustration">
        <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="1" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20.42 4.58a5.4 5.4 0 0 0-7.65 0l-.77.78-.77-.78a5.4 5.4 0 0 0-7.65 0C1.46 6.7 1.33 10.28 4 13l8 8 8-8c2.67-2.72 2.54-6.3.42-8.42z" />
        </svg>
      </div>
      <h2>No recommendations yet</h2>
      <p>Start browsing our products to get personalized recommendations</p>
      <button class="btn-browse" @click="goToCatalog">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1" />
          <circle cx="20" cy="21" r="1" />
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
        </svg>
        Browse Products
      </button>
    </div>

    <div v-else class="recommendations-content">
      <div class="recommendations-grid">
        <div v-for="product in personalized" :key="product.id" class="recommendation-card">
          <div class="recommendation-badge">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.42 4.58a5.4 5.4 0 0 0-7.65 0l-.77.78-.77-.78a5.4 5.4 0 0 0-7.65 0C1.46 6.7 1.33 10.28 4 13l8 8 8-8c2.67-2.72 2.54-6.3.42-8.42z" />
            </svg>
            Recommended
          </div>
          <ProductCard :product="product" />
        </div>
      </div>

      <div class="recommendations-footer">
        <p class="footer-text">Based on your browsing history and healthcare needs</p>
        <button class="btn-view-all" @click="goToCatalog">
          View All Products
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recommendations-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 80vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  gap: 20px;
}
.header-content h1 {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}
.header-subtitle {
  font-size: 15px;
  color: var(--color-text-muted);
}
.btn-refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: transparent;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-refresh:hover {
  color: var(--color-text);
  border-color: var(--color-text);
  background: var(--color-bg);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  color: var(--color-text-secondary);
}
.loading-spinner {
  margin-bottom: 16px;
}
.spinner {
  animation: spin 1s linear infinite;
  color: var(--color-primary);
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  color: var(--color-text-secondary);
}
.empty-illustration {
  color: var(--color-text-muted);
  margin-bottom: 24px;
  opacity: 0.6;
}
.empty-state h2 {
  font-size: 24px;
  color: var(--color-text);
  margin-bottom: 8px;
}
.empty-state p {
  font-size: 15px;
  color: var(--color-text-muted);
  margin-bottom: 32px;
  max-width: 400px;
}
.btn-browse {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}
.btn-browse:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.recommendations-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.recommendation-card {
  position: relative;
}

.recommendation-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(146, 64, 14, 0.2);
}

.recommendations-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}
.footer-text {
  font-size: 14px;
  color: var(--color-text-muted);
}
.btn-view-all {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-view-all:hover {
  background: #1d4ed8;
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
  .recommendations-grid {
    grid-template-columns: 1fr;
  }
  .recommendations-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
}
</style>