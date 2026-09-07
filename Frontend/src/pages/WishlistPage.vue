<script setup>
import { ref, onMounted, computed } from 'vue'
import { useWishlistStore } from '../stores/wishlist.js'
import { useProductStore } from '../stores/product.js'
import ProductCard from '../components/product/ProductCard.vue'

const wishlistStore = useWishlistStore()
const productStore = useProductStore()
const isLoading = ref(true)

const wishlistProducts = computed(() => {
  return productStore.products.filter((p) => wishlistStore.isWishlisted(p.id))
})

onMounted(async () => {
  await productStore.fetchProducts(0, 100)
  isLoading.value = false
})
</script>

<template>
  <div class="wishlist-page">
    <div class="page-header">
      <div class="header-content">
        <div class="header-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="#ef4444" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
        </div>
        <div>
          <h1 class="page-title">My Wishlist</h1>
          <p class="page-subtitle">{{ wishlistStore.count }} {{ wishlistStore.count === 1 ? 'item' : 'items' }} saved</p>
        </div>
      </div>
    </div>

    <div v-if="isLoading" class="loading-state">
      <div class="dot-typing"><span></span><span></span><span></span></div>
      <p>Loading your wishlist...</p>
    </div>

    <div v-else-if="wishlistProducts.length === 0" class="empty-state">
      <div class="empty-icon">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
        </svg>
      </div>
      <h2>Your wishlist is empty</h2>
      <p>Browse products and heart the ones you love to save them here</p>
      <router-link to="/products" class="btn-browse">Browse Products</router-link>
    </div>

    <div v-else class="product-grid">
      <ProductCard v-for="product in wishlistProducts" :key="product.id" :product="product" />
    </div>
  </div>
</template>

<style scoped>
.wishlist-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 32px 24px 64px;
}

.page-header {
  max-width: 1200px;
  margin: 0 auto 28px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fef2f2;
  border-radius: var(--radius-lg);
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--color-text-secondary);
  max-width: 1200px;
  margin: 0 auto;
}

.empty-icon {
  color: #d1d5db;
  margin-bottom: 16px;
}

.empty-state h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}

.btn-browse {
  margin-top: 16px;
  padding: 10px 24px;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: background 0.2s;
}
.btn-browse:hover {
  background: var(--color-primary-dark);
}

.product-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 900px) {
  .product-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .product-grid { grid-template-columns: 1fr; }
}

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
