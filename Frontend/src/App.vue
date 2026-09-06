<script setup>
import NotificationContainer from './components/NotificationContainer.vue'
import AppNavbar from './components/AppNavbar.vue'
</script>

<template>
  <div class="app-shell">
    <AppNavbar />
    <main class="app-main">
      <router-view />
    </main>
    <NotificationContainer />
  </div>
</template>
