<script setup>
import { onMounted, computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useProductStore } from '../stores/product.js'

const route = useRoute()
const router = useRouter()
const store = useProductStore()
const { selectedProduct, isLoading, error } = storeToRefs(store)

const activeTab = ref('ingredients')

const productId = computed(() => Number(route.params.id))

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

function formatPrice(price) {
  if (price == null) return '$0.00'
  return '$' + Number(price).toFixed(2)
}

function renderStars(rating) {
  return '★'.repeat(Math.floor(rating || 0)) + '☆'.repeat(5 - Math.floor(rating || 0))
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

function goBack() {
  router.push('/products')
}

onMounted(async () => {
  await store.fetchProductById(productId.value)
})
</script>

<template>
  <div class="detail-page">
    <!-- Loading -->
    <div v-if="isLoading" class="loading-state">
      <div class="dot-typing"><span></span><span></span><span></span></div>
      <p>Loading product details...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-state">
      <h2>Failed to load product</h2>
      <p>{{ error }}</p>
      <button class="btn-back" @click="goBack">Back to Products</button>
    </div>

    <!-- Not found -->
    <div v-else-if="!selectedProduct" class="error-state">
      <h2>Product Not Found</h2>
      <button class="btn-back" @click="goBack">Back to Products</button>
    </div>

    <!-- Detail content -->
    <template v-else>
      <button class="btn-back-link" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12" />
          <polyline points="12 19 5 12 12 5" />
        </svg>
        Back to Products
      </button>

      <div class="detail-layout">
        <!-- Image -->
        <div class="detail-image-section">
          <div class="detail-image">
            <img
              v-if="selectedProduct.imageUrl"
              :src="selectedProduct.imageUrl"
              :alt="selectedProduct.name"
            />
            <div v-else class="image-placeholder">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
            </div>
          </div>
        </div>

        <!-- Info -->
        <div class="detail-info">
          <span class="detail-category">{{ categoryLabel(selectedProduct.category) }}</span>
          <h1 class="detail-name">{{ selectedProduct.name }}</h1>

          <div v-if="selectedProduct.manufacturer" class="detail-manufacturer">
            by {{ selectedProduct.manufacturer }}
          </div>

          <div class="detail-price-row">
            <span class="detail-price">{{ formatPrice(selectedProduct.price) }}</span>
            <span v-if="selectedProduct.stockQuantity > 0" class="in-stock">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              In Stock ({{ selectedProduct.stockQuantity }})
            </span>
            <span v-else class="out-of-stock">Out of Stock</span>
          </div>

          <div class="rating-row">
            <span class="stars-big">{{ renderStars(selectedProduct.ratings) }}</span>
            <span class="rating-num">{{ (selectedProduct.ratings || 0).toFixed(1) }} / 5</span>
          </div>

          <div v-if="selectedProduct.prescriptionRequired" class="rx-alert">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
            This product requires a valid prescription
          </div>

          <div v-if="selectedProduct.dosage" class="detail-field">
            <span class="field-label">Dosage:</span>
            <span>{{ selectedProduct.dosage }}</span>
          </div>

          <!-- Tabs -->
          <div class="tabs">
            <button
              class="tab-btn"
              :class="{ active: activeTab === 'description' }"
              @click="activeTab = 'description'"
            >Description</button>
            <button
              class="tab-btn"
              :class="{ active: activeTab === 'ingredients' }"
              @click="activeTab = 'ingredients'"
            >Ingredients</button>
            <button
              v-if="selectedProduct.sideEffects"
              class="tab-btn"
              :class="{ active: activeTab === 'sideEffects' }"
              @click="activeTab = 'sideEffects'"
            >Side Effects</button>
            <button
              class="tab-btn"
              :class="{ active: activeTab === 'reviews' }"
              @click="activeTab = 'reviews'"
            >Reviews</button>
          </div>

          <div class="tab-content">
            <div v-if="activeTab === 'description'" class="tab-pane">
              <p>{{ selectedProduct.description || 'No description available.' }}</p>
            </div>
            <div v-else-if="activeTab === 'ingredients'" class="tab-pane">
              <p>{{ selectedProduct.ingredients || 'No ingredient information available.' }}</p>
            </div>
            <div v-else-if="activeTab === 'sideEffects'" class="tab-pane">
              <p>{{ selectedProduct.sideEffects }}</p>
            </div>
            <div v-else-if="activeTab === 'reviews'" class="tab-pane">
              <div v-if="selectedProduct.reviews?.length > 0" class="reviews-list">
                <div v-for="(review, i) in selectedProduct.reviews" :key="i" class="review-item">
                  <div class="review-header">
                    <span class="reviewer-name">{{ review.reviewerName || 'Anonymous' }}</span>
                    <span class="review-stars">{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5 - review.rating) }}</span>
                  </div>
                  <p class="review-comment">{{ review.comment }}</p>
                  <span class="review-date">{{ formatDate(review.createdAt) }}</span>
                </div>
              </div>
              <p v-else class="no-reviews">No reviews yet. Be the first to review!</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 32px 24px;
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 20px;
  color: var(--color-text-secondary);
}
.error-state h2 {
  color: var(--color-text);
  margin-bottom: 8px;
}

.btn-back-link, .btn-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
  transition: background 0.15s;
}
.btn-back-link:hover, .btn-back:hover { background: var(--color-surface); }

/* Layout */
.detail-layout {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

@media (max-width: 768px) {
  .detail-layout { grid-template-columns: 1fr; }
}

.detail-image-section {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}
.detail-image { width: 100%; }
.detail-image img { width: 100%; height: 100%; object-fit: cover; }
.image-placeholder { color: var(--color-text-muted); opacity: 0.5; }

.detail-info {
  padding: 32px;
}

.detail-category {
  display: inline-block;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
  background: var(--color-primary-bg);
  border: 1px solid var(--color-primary-light);
  margin-bottom: 12px;
}

.detail-name {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 6px;
}

.detail-manufacturer {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-bottom: 16px;
}

.detail-price-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.detail-price {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
}
.in-stock {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-success);
  font-weight: 500;
}
.out-of-stock {
  font-size: 13px;
  color: var(--color-danger);
  font-weight: 500;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.stars-big { color: #f59e0b; font-size: 20px; letter-spacing: 2px; }
.rating-num { font-size: 13px; color: var(--color-text-muted); }

.rx-alert {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fef2f2;
  color: var(--color-danger);
  border: 1px solid #fecaca;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 16px;
}

.detail-field {
  display: flex;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border);
}
.field-label {
  font-weight: 600;
  color: var(--color-text);
  flex-shrink: 0;
}

/* Tabs */
.tabs {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--color-border);
  margin-top: 20px;
}
.tab-btn {
  padding: 10px 20px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-muted);
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.15s;
}
.tab-btn:hover { color: var(--color-text); }
.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tab-content { padding: 16px 0; }
.tab-pane { font-size: 14px; color: var(--color-text-secondary); line-height: 1.7; }

/* Reviews */
.reviews-list { display: flex; flex-direction: column; gap: 16px; }
.review-item {
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
}
.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.reviewer-name { font-size: 13px; font-weight: 600; color: var(--color-text); }
.review-stars { color: #f59e0b; font-size: 12px; }
.review-comment { font-size: 13px; color: var(--color-text-secondary); margin-bottom: 6px; }
.review-date { font-size: 11px; color: var(--color-text-muted); }
.no-reviews { font-style: italic; color: var(--color-text-muted); }
</style>
