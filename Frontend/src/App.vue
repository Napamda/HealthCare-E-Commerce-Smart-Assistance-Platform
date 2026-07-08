<script setup>
import AppNavbar from './components/AppNavbar.vue'
</script>

<template>
  <div class="app-shell">
    <AppNavbar />
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>
