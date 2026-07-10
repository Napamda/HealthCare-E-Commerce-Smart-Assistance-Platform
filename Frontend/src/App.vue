<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth.js'
import { useCartStore } from './stores/cart.js'
import CartIcon from './components/cart/CartIcon.vue'
import { ROLE_LABELS, ROLE_DASHBOARD } from './config/permissions.js'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const dashboardLink = computed(() => {
  if (!authStore.userRole) return '/chat'
  return ROLE_DASHBOARD[authStore.userRole] || '/chat'
})

const roleLabel = computed(() => {
  if (!authStore.userRole) return ''
  return ROLE_LABELS[authStore.userRole] || authStore.userRole
})

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}

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

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--color-bg);
}

.app-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 56px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.nav-left { display: flex; align-items: center; }

.nav-brand {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  text-decoration: none;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  padding: 6px 12px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  text-decoration: none;
  border-radius: var(--radius-md);
  transition: all 0.15s;
}
.nav-link:hover {
  color: var(--color-text);
  background: var(--color-bg);
}
.nav-link.router-link-active {
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

.nav-cta {
  background: var(--color-primary);
  color: #fff !important;
  font-weight: 600;
}
.nav-cta:hover {
  background: #1d4ed8 !important;
  color: #fff !important;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  padding: 6px 12px;
}

.nav-role-badge {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--color-bg);
  color: var(--color-primary);
  border: 1px solid var(--color-border);
}

.nav-logout {
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
}
.nav-logout:hover {
  color: #dc2626;
  border-color: #dc2626;
  background: #fef2f2;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text);
}
</style>
