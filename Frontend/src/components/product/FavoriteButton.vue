<script setup>
import { ref } from 'vue'
import { useWishlistStore } from '../../stores/wishlist.js'

const props = defineProps({
  productId: { type: [Number, String], required: true },
  size: { type: String, default: 'md' },
})

const wishlistStore = useWishlistStore()
const animating = ref(false)

async function toggleFavorite() {
  animating.value = true
  await wishlistStore.toggleItem(props.productId)
  setTimeout(() => { animating.value = false }, 400)
}
</script>

<template>
  <button
    class="fav-btn"
    :class="{
      'is-fav': wishlistStore.isWishlisted(productId),
      'is-animating': animating,
      'size-sm': size === 'sm',
      'size-lg': size === 'lg',
    }"
    @click.stop="toggleFavorite"
    :title="wishlistStore.isWishlisted(productId) ? 'Remove from wishlist' : 'Add to wishlist'"
  >
    <svg viewBox="0 0 24 24"
      :width="size === 'sm' ? 16 : size === 'lg' ? 24 : 20"
      :height="size === 'sm' ? 16 : size === 'lg' ? 24 : 20"
      fill="currentColor" stroke="currentColor" stroke-width="2"
      stroke-linecap="round" stroke-linejoin="round">
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
    </svg>
  </button>
</template>

<style scoped>
.fav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(255,255,255,0.9);
  backdrop-filter: blur(8px);
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.fav-btn:hover {
  color: #ef4444;
  transform: scale(1.1);
  box-shadow: 0 4px 16px rgba(239,68,68,0.2);
}
.fav-btn.is-fav {
  color: #ef4444;
  background: rgba(254,226,226,0.95);
}
.fav-btn.is-animating {
  animation: heartPop 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.fav-btn.size-sm {
  width: 28px;
  height: 28px;
}
.fav-btn.size-lg {
  width: 44px;
  height: 44px;
}

@keyframes heartPop {
  0% { transform: scale(1); }
  30% { transform: scale(1.3); }
  60% { transform: scale(0.9); }
  100% { transform: scale(1); }
}
</style>
