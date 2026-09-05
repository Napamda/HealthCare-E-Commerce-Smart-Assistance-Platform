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
    <nav class="app-nav">
      <div class="nav-left">
        <router-link to="/chat" class="nav-brand">HealthCare</router-link>
      </div>
      <div class="nav-right">
        <router-link to="/chat" class="nav-link">Chat</router-link>
        <router-link to="/consultations" class="nav-link">Consultations</router-link>
        <router-link to="/products" class="nav-link">Products</router-link>
        <router-link
          v-if="authStore.hasRole('VENDOR')"
          to="/inventory"
          class="nav-link"
        >Inventory</router-link>

        <router-link
          v-if="authStore.hasRole('ADMIN')"
          to="/admin"
          class="nav-link"
        >Admin</router-link>
        <router-link
          v-if="authStore.hasRole('DOCTOR')"
          to="/doctor"
          class="nav-link"
        >Doctor</router-link>
        <router-link
          v-if="authStore.hasRole('PHARMACIST')"
          to="/pharmacist"
          class="nav-link"
        >Pharmacist</router-link>
        <router-link
          v-if="authStore.hasRole('VENDOR')"
          to="/vendor"
          class="nav-link"
        >Vendor</router-link>
        <router-link
          v-if="authStore.hasRole('DOCTOR', 'PHARMACIST', 'ADMIN')"
          :to="dashboardLink"
          class="nav-link"
        >Dashboard</router-link>

        <template v-if="authStore.isAuthenticated">
          <CartIcon />
          <router-link to="/orders" class="nav-link">Orders</router-link>
          <span class="nav-user">
            {{ authStore.currentUser?.firstName }}
            <span class="nav-role-badge">{{ roleLabel }}</span>
          </span>
          <button class="nav-logout" @click="handleLogout">Sign out</button>
        </template>
        <template v-else>
          <router-link to="/login" class="nav-link">Sign in</router-link>
          <router-link to="/register" class="nav-link nav-cta">Register</router-link>
        </template>
      </div>
    </nav>
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>
