<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth.js'
import { useCartStore } from './stores/cart.js'
import AppNavbar from './components/AppNavbar.vue'

const authStore = useAuthStore()
const cartStore = useCartStore()

onMounted(() => {
  if (authStore.isAuthenticated) {
    cartStore.fetchCount()
  }
})
</script>

<template>
  <div class="app-shell">
    <AppNavbar />
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>
