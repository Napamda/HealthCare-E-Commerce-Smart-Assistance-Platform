<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { ROLE_LABELS } from '../config/permissions.js'
import { getNavItems } from '../config/navigation.js'

const router = useRouter()
const authStore = useAuthStore()
const openDropdown = ref(null)

const roleLabel = computed(() => {
  if (!authStore.userRole) return ''
  return ROLE_LABELS[authStore.userRole] || authStore.userRole
})

const navItems = computed(() => {
  if (!authStore.userRole) return []
  return getNavItems(authStore.userRole)
})

function toggleDropdown(label) {
  openDropdown.value = openDropdown.value === label ? null : label
}

function closeDropdown() {
  openDropdown.value = null
}

function navigate(path) {
  closeDropdown()
  router.push(path)
}

async function handleLogout() {
  closeDropdown()
  await authStore.logout()
  router.push('/login')
}

function isDropdown(item) {
  return item.children && item.children.length > 0
}
</script>

<template>
  <nav class="app-nav">
    <div class="nav-left">
      <router-link to="/chat" class="nav-brand">HealthCare</router-link>
    </div>

    <div class="nav-center" v-if="authStore.isAuthenticated">
      <template v-for="item in navItems" :key="item.label">
        <template v-if="isDropdown(item)">
          <div class="nav-dropdown-wrapper">
            <button
              class="nav-link nav-dropdown-trigger"
              :class="{ 'nav-dropdown-open': openDropdown === item.label }"
              @click="toggleDropdown(item.label)"
            >
              {{ item.label }}
              <span class="nav-chevron">▾</span>
            </button>
            <div class="nav-dropdown-menu" v-show="openDropdown === item.label">
              <button
                v-for="child in item.children"
                :key="child.to"
                class="nav-dropdown-item"
                @click="navigate(child.to)"
              >
                {{ child.label }}
              </button>
            </div>
          </div>
        </template>

        <router-link v-else :to="item.to" class="nav-link" @click="closeDropdown">
          {{ item.label }}
        </router-link>
      </template>
    </div>

    <div class="nav-right">
      <template v-if="!authStore.isAuthenticated">
        <router-link to="/login" class="nav-link">Sign in</router-link>
        <router-link to="/register" class="nav-link nav-cta">Register</router-link>
      </template>
      <template v-else>
        <span class="nav-user">
          {{ authStore.currentUser?.firstName }}
          <span class="nav-role-badge">{{ roleLabel }}</span>
        </span>
        <button class="nav-logout" @click="handleLogout">Sign out</button>
      </template>
    </div>
  </nav>
</template>
