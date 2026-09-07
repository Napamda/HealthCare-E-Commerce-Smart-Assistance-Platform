<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart.js'
import FavoriteButton from './FavoriteButton.vue'

const props = defineProps({
  product: { type: Object, required: true },
  index: { type: Number, default: 0 },
})

const emit = defineEmits(['quickView'])

const router = useRouter()
const cartStore = useCartStore()
const adding = ref(false)
const addedConfirm = ref(false)
const isHovered = ref(false)
const isVisible = ref(false)

onMounted(() => {
  setTimeout(() => { isVisible.value = true }, 50 + props.index * 60)
})

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

function categoryColor(cat) {
  const colors = {
    VITAMINS: '#f59e0b', PAIN_RELIEF: '#ef4444', SKIN_CARE: '#ec4899',
    DIGESTIVE_HEALTH: '#10b981', RESPIRATORY: '#06b6d4', HEART_HEALTH: '#dc2626',
    DIABETES_CARE: '#7c3aed', FIRST_AID: '#0891b2', MEDICAL_DEVICES: '#6366f1',
    PERSONAL_CARE: '#14b8a6', WELLNESS: '#22c55e', BABY_CARE: '#f472b6',
    ELDERLY_CARE: '#8b5cf6', OTHER: '#6b7280',
  }
  return colors[cat] || '#6b7280'
}

const displayImage = computed(() => props.product.imageUrl || props.product.imageUrls?.[0] || null)
const imageLoadError = ref(false)

function onImageError() {
  imageLoadError.value = true
}

watch(() => props.product.id, () => {
  imageLoadError.value = false
})

const hasDiscount = computed(() =>
  (props.product.discountPercent > 0) || (props.product.originalPrice > props.product.price)
)
const discountPercent = computed(() => {
  if (props.product.discountPercent) return props.product.discountPercent
  if (props.product.originalPrice && props.product.price) {
    return Math.round((1 - props.product.price / props.product.originalPrice) * 100)
  }
  return 0
})

const stockStatus = computed(() => {
  const s = props.product.stockQuantity
  if (s == null) return null
  if (s <= 0) return { label: 'Out of Stock', cls: 'out-stock' }
  if (s <= 10) return { label: `Only ${s} left`, cls: 'low-stock' }
  return null
})

function formatPrice(price) {
  if (price == null) return '$0.00'
  return '$' + Number(price).toFixed(2)
}

function renderStars(rating) {
  const full = Math.floor(rating || 0)
  return '★'.repeat(full) + '☆'.repeat(5 - full)
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
  addedConfirm.value = true
  setTimeout(() => { addedConfirm.value = false }, 1500)
}
</script>

<template>
  <div
    class="product-card"
    :class="{ visible: isVisible, hovered: isHovered }"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
    @click="navigateToDetail"
  >
    <div class="card-image" :style="{ background: `linear-gradient(135deg, ${categoryColor(product.category)}10, ${categoryColor(product.category)}05)` }">
      <img v-if="displayImage && !imageLoadError" :src="displayImage" :alt="product.name" class="product-img" :class="{ zoomed: isHovered }" @error="onImageError" />
      <div v-else class="image-placeholder" :style="{ color: categoryColor(product.category) }">
        <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" />
        </svg>
      </div>

      <Transition name="overlay-fade">
        <div v-if="isHovered" class="card-image-overlay">
          <FavoriteButton :product-id="product.id" size="sm" />
          <button class="btn-quick-view" @click.stop="emit('quickView', product)" title="Quick View">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
          </button>
        </div>
      </Transition>

      <span v-if="hasDiscount" class="discount-badge">-{{ discountPercent }}%</span>
      <span class="category-tag" :style="{ background: categoryColor(product.category) + 'E6', color: '#fff' }">{{ categoryLabel(product.category) }}</span>
      <span v-if="stockStatus" class="stock-tag" :class="stockStatus.cls">{{ stockStatus.label }}</span>
    </div>

    <div class="card-body">
      <h3 class="product-name">{{ product.name }}</h3>
      <p v-if="product.manufacturer" class="product-manufacturer">{{ product.manufacturer }}</p>
      <p v-if="product.description" class="product-description">{{ product.description }}</p>

      <div class="card-footer">
        <div class="price-block">
          <span v-if="hasDiscount && product.originalPrice" class="original-price">{{ formatPrice(product.originalPrice) }}</span>
          <span class="product-price">{{ formatPrice(product.price) }}</span>
        </div>
        <div class="rating-stars">
          <span class="stars">{{ renderStars(product.ratings) }}</span>
          <span class="rating-value">{{ (product.ratings || 0).toFixed(1) }}</span>
        </div>
      </div>

      <button
        class="btn-card-cart"
        :class="{ added: addedConfirm }"
        :disabled="adding"
        @click="handleAddToCart"
      >
        <svg v-if="!addedConfirm" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" />
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
        </svg>
        <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
        {{ addedConfirm ? 'Added!' : adding ? 'Adding...' : 'Add to Cart' }}
      </button>

      <div v-if="product.prescriptionRequired" class="rx-badge">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" /><path d="M9 12h6M12 9v6" />
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
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.5s cubic-bezier(0.22, 0.61, 0.36, 1), transform 0.5s cubic-bezier(0.22, 0.61, 0.36, 1), border-color 0.3s ease, box-shadow 0.35s cubic-bezier(0.22, 0.61, 0.36, 1);
  position: relative;
}
.product-card::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  opacity: 0;
  box-shadow: 0 0 0 2px var(--color-primary), 0 0 20px rgba(37, 99, 235, 0.15);
  transition: opacity 0.35s ease;
  pointer-events: none;
  z-index: 5;
}
.product-card.visible {
  opacity: 1;
  transform: translateY(0);
}
.product-card.hovered {
  border-color: var(--color-primary);
  box-shadow: 0 12px 36px rgba(37, 99, 235, 0.15);
  transform: translateY(-6px);
}
.product-card.hovered::after {
  opacity: 1;
}

.card-image {
  width: 100%;
  height: 220px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.product-img.zoomed {
  transform: scale(1.1);
}

.image-placeholder {
  opacity: 0.35;
}

.card-image-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 12px;
  gap: 8px;
}

.overlay-fade-enter-active { transition: opacity 0.25s ease; }
.overlay-fade-leave-active { transition: opacity 0.15s ease; }
.overlay-fade-enter-from,
.overlay-fade-leave-to { opacity: 0; }

.btn-quick-view {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.95);
  color: #374151;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease;
}
.btn-quick-view:hover {
  transform: scale(1.1);
  background: #fff;
}

.discount-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 10px;
  background: #ef4444;
  color: #fff;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 700;
  z-index: 2;
  animation: pulseBadge 2s infinite;
}
@keyframes pulseBadge {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.06); }
}

.category-tag {
  position: absolute;
  bottom: 10px;
  left: 10px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 600;
  z-index: 2;
  backdrop-filter: blur(4px);
}

.stock-tag {
  position: absolute;
  bottom: 10px;
  right: 10px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 600;
  z-index: 2;
  backdrop-filter: blur(4px);
}
.low-stock { background: rgba(254, 243, 199, 0.92); color: #d97706; }
.out-stock { background: rgba(254, 226, 226, 0.92); color: #dc2626; }

.card-body {
  padding: 16px;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-manufacturer {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 4px;
}

.product-description {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.4;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.price-block {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.original-price {
  font-size: 13px;
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.product-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

.rating-stars {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stars {
  font-size: 13px;
  color: #f59e0b;
  letter-spacing: 1px;
}

.rating-value {
  font-size: 12px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.btn-card-cart {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 10px;
  background: var(--color-bg);
  color: var(--color-primary);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
  position: relative;
  overflow: hidden;
}
.btn-card-cart::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--color-primary);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
  z-index: 0;
}
.btn-card-cart:hover:not(:disabled)::before {
  transform: scaleX(1);
}
.btn-card-cart:hover:not(:disabled) {
  color: #fff;
  border-color: var(--color-primary);
}
.btn-card-cart > * {
  position: relative;
  z-index: 1;
}
.btn-card-cart:disabled {
  opacity: 0.5;
  cursor: default;
}
.btn-card-cart.added {
  background: #10b981;
  color: #fff;
  border-color: #10b981;
}
.btn-card-cart.added::before {
  display: none;
}

.rx-badge {
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
</style>