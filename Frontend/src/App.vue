<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const navItems = [
  { path: '/prescriptions', label: 'My Prescriptions' },
  { path: '/prescriptions/upload', label: 'Upload' },
]

function isActive(path) {
  return route.path === path
}

function navigate(path) {
  router.push(path)
}
</script>

<template>
  <div class="app-shell">
    <nav class="navbar">
      <div class="navbar-inner">
        <div class="brand" @click="navigate('/prescriptions')">
          <span class="brand-icon">&#9878;</span>
          <span class="brand-text">HealthCare</span>
        </div>
        <div class="nav-links">
          <button
            v-for="item in navItems"
            :key="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
            @click="navigate(item.path)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--color-bg);
}

.navbar {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.brand-icon {
  font-size: 24px;
  line-height: 1;
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
}

.nav-links {
  display: flex;
  gap: 4px;
}

.nav-link {
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all 0.15s ease;
}

.nav-link:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

.nav-link.active {
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: 600;
}

.main-content {
  min-height: calc(100vh - 56px);
}
</style>
