<script setup>
import { computed } from 'vue'
import { useCartStore } from '../../stores/cart.js'

const cartStore = useCartStore()
const count = computed(() => cartStore.itemCount)
</script>

<template>
  <router-link to="/cart" class="cart-icon-link" title="Shopping Cart">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
      stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="9" cy="21" r="1" />
      <circle cx="20" cy="21" r="1" />
      <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
    </svg>
    <span v-if="count > 0" class="cart-badge">{{ count }}</span>
  </router-link>
</template>
