<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart.js'

const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
})

const router = useRouter()
const cartStore = useCartStore()
const adding = ref(false)

const categoryLabel = computed(() => {
  const labels = {
    VITAMINS: 'Vitamins', PAIN_RELIEF: 'Pain Relief', SKIN_CARE: 'Skin Care',
    DIGESTIVE_HEALTH: 'Digestive Health', RESPIRATORY: 'Respiratory', HEART_HEALTH: 'Heart Health',
    DIABETES_CARE: 'Diabetes Care', FIRST_AID: 'First Aid', MEDICAL_DEVICES: 'Medical Devices',
    PERSONAL_CARE: 'Personal Care', WELLNESS: 'Wellness', BABY_CARE: 'Baby Care',
    ELDERLY_CARE: 'Elderly Care', OTHER: 'Other',
  }
  return labels[props.product.category] || props.product.category
})

function formatPrice(price) {
  if (price == null) return '$0.00'
  return '$' + Number(price).toFixed(2)
}

function renderStars(rating) {
  const full = Math.floor(rating || 0)
  let stars = ''
  for (let i = 0; i < 5; i++) stars += i < full ? '★' : '☆'
  return stars
}

function navigateToDetail() {
  router.push(`/products/${props.product.id}`)
}

async function handleAddToCart(e) {
  e.stopPropagation()
  if (adding.value) return
  adding.value = true
  await cartStore.addItem(props.product)
  adding.value = false
}
</script>

<template>
  <div class="product-card" @click="navigateToDetail">
    <!-- Image placeholder -->
    <div class="card-image">
      <img
        v-if="product.imageUrl"
        :src="product.imageUrl"
        :alt="product.name"
        class="product-img"
      />
      <div v-else class="image-placeholder">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
      </div>
      <span class="category-badge">{{ categoryLabel }}</span>
    </div>

    <!-- Info -->
    <div class="card-info">
      <h3 class="product-name">{{ product.name }}</h3>
      <p v-if="product.manufacturer" class="product-manufacturer">{{ product.manufacturer }}</p>
      <p class="product-description">{{ product.description?.substring(0, 80) }}{{ product.description?.length > 80 ? '...' : '' }}</p>

      <div class="card-footer">
        <div class="rating-stars" :title="'Rating: ' + (product.ratings || 0)">
          <span class="stars">{{ renderStars(product.ratings) }}</span>
          <span class="rating-value">{{ (product.ratings || 0).toFixed(1) }}</span>
        </div>
        <span class="product-price">{{ formatPrice(product.price) }}</span>
      </div>

      <button class="btn-card-cart" :disabled="adding" @click="handleAddToCart">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" />
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
        </svg>
        {{ adding ? 'Adding...' : 'Add to Cart' }}
      </button>

      <div v-if="product.prescriptionRequired" class="prescription-badge">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <path d="M9 12h6M12 9v6" />
        </svg>
        Rx Required
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.card-image {
  position: relative;
  width: 100%;
  height: 200px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  color: var(--color-text-muted);
  opacity: 0.5;
}

.category-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  color: var(--color-primary);
  border: 1px solid var(--color-primary-light);
}

.card-info {
  padding: 16px;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
  line-height: 1.3;
}

.product-manufacturer {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 6px;
}

.product-description {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.4;
  margin-bottom: 12px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rating-stars {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stars {
  font-size: 14px;
  color: #f59e0b;
  letter-spacing: 1px;
}

.rating-value {
  font-size: 12px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.product-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

.prescription-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  padding: 4px 10px;
  background: #fef2f2;
  color: var(--color-danger);
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  border: 1px solid #fecaca;
}

.btn-card-cart {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 12px;
  padding: 8px;
  background: var(--color-bg);
  color: var(--color-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-card-cart:hover:not(:disabled) {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}
.btn-card-cart:disabled { opacity: 0.5; cursor: default; }
</style>
