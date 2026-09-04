<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { ROLE_LABELS } from '../config/permissions.js'
import { getNavItems } from '../config/navigation.js'
import NotificationBell from './NotificationBell.vue'

const router = useRouter()
const authStore = useAuthStore()

const openDropdown = ref(null)   // which top-level dropdown is open (desktop)
const userMenuOpen = ref(false)  // the single consolidated user menu
const mobileNavOpen = ref(false) // hamburger drawer, small screens only

const roleLabel = computed(() => {
  if (!authStore.userRole) return ''
  return ROLE_LABELS[authStore.userRole] || authStore.userRole
})

const navItems = computed(() => {
  if (!authStore.userRole) return []
  return getNavItems(authStore.userRole)
})

function isDropdown(item) {
  return item.children && item.children.length > 0
}

function toggleDropdown(label) {
  openDropdown.value = openDropdown.value === label ? null : label
}

function closeAllMenus() {
  openDropdown.value = null
  userMenuOpen.value = false
}

function navigate(path) {
  closeAllMenus()
  mobileNavOpen.value = false
  router.push(path)
}

function toggleUserMenu() {
  openDropdown.value = null
  userMenuOpen.value = !userMenuOpen.value
}

function toggleMobileNav() {
  mobileNavOpen.value = !mobileNavOpen.value
  closeAllMenus()
}

async function handleLogout() {
  closeAllMenus()
  await authStore.logout()
  router.push('/login')
}
</script>

<template>
  <nav class="app-nav" @click.self="closeAllMenus">
    <div class="nav-left">
      <router-link to="/chat" class="nav-brand" @click="mobileNavOpen = false">
        HealthCare
      </router-link>
    </div>

    <!-- Desktop primary nav -->
    <div class="nav-center" v-if="authStore.isAuthenticated">
      <template v-for="item in navItems" :key="item.label">
        <div v-if="isDropdown(item)" class="nav-dropdown-wrapper">
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

        <router-link v-else :to="item.to" class="nav-link" @click="closeAllMenus">
          {{ item.label }}
        </router-link>
      </template>
    </div>

    <!-- Right cluster: notifications + one consolidated user menu -->
    <div class="nav-right">
      <template v-if="!authStore.isAuthenticated">
        <router-link to="/login" class="nav-link">Sign in</router-link>
        <router-link to="/register" class="nav-link nav-cta">Register</router-link>
      </template>

      <template v-else>
        <NotificationBell class="nav-notification-slot" />

        <div class="nav-dropdown-wrapper">
          <button class="nav-user-trigger" @click="toggleUserMenu">
            <span class="nav-user-avatar">
              <img
                v-if="authStore.currentUser?.avatarUrl"
                :src="authStore.currentUser.avatarUrl"
                alt="Avatar"
              />
              <template v-else>{{ authStore.currentUser?.firstName?.[0] || '?' }}</template>
            </span>
            <span class="nav-user-name">{{ authStore.currentUser?.firstName }}</span>
            <span class="nav-chevron">▾</span>
          </button>

          <div class="nav-dropdown-menu nav-user-menu" v-show="userMenuOpen">
            <div class="nav-user-menu-header">
              <span class="nav-role-badge">{{ roleLabel }}</span>
            </div>
            <button class="nav-dropdown-item" @click="navigate('/profile')">
              My Profile
            </button>
            <button class="nav-dropdown-item nav-logout-item" @click="handleLogout">
              Sign out
            </button>
          </div>
        </div>

        <!-- Hamburger — only visible under the mobile breakpoint, see navbar.css -->
        <button class="nav-hamburger" @click="toggleMobileNav" aria-label="Open menu">
          <span /><span /><span />
        </button>
      </template>
    </div>

    <!-- Mobile drawer: same navItems config, no separate route list to maintain -->
    <div class="nav-mobile-drawer" v-if="mobileNavOpen && authStore.isAuthenticated">
      <template v-for="item in navItems" :key="'m-' + item.label">
        <template v-if="isDropdown(item)">
          <div class="nav-mobile-group-label">{{ item.label }}</div>
          <button
            v-for="child in item.children"
            :key="child.to"
            class="nav-mobile-link nav-mobile-sublink"
            @click="navigate(child.to)"
          >
            {{ child.label }}
          </button>
        </template>
        <button v-else class="nav-mobile-link" @click="navigate(item.to)">
          {{ item.label }}
        </button>
      </template>
    </div>
  </nav>
</template>
