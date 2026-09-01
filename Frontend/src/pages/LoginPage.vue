<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const rememberMe = ref(false)
const loading = ref(false)
const error = ref('')
const validationErrors = ref({})

function validate() {
  const errors = {}

  if (!email.value.trim()) {
    errors.email = 'Email is required'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    errors.email = 'Must be a valid email address'
  }

  if (!password.value) {
    errors.password = 'Password is required'
  }

  validationErrors.value = errors
  return Object.keys(errors).length === 0
}

async function handleLogin() {
  if (!validate()) return

  loading.value = true
  error.value = ''
  authStore.clearError()

  try {
    await authStore.login({
      email: email.value.trim().toLowerCase(),
      password: password.value,
      rememberMe: rememberMe.value,
    })

    const redirect = route.query.redirect || '/chat'
    router.push(redirect)
  } catch (e) {
    error.value = e.response?.data?.error || 'Login failed. Please check your credentials.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>Welcome back</h1>
        <p>Sign in to your HealthCare account</p>
      </div>

      <div v-if="error" class="alert alert-error">
        {{ error }}
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="field">
          <label for="login-email">Email</label>
          <input
            id="login-email"
            v-model="email"
            type="email"
            placeholder="you@example.com"
            :class="{ 'input-error': validationErrors.email }"
            autocomplete="email"
          />
          <span v-if="validationErrors.email" class="field-error">{{ validationErrors.email }}</span>
        </div>

        <div class="field">
          <label for="login-password">Password</label>
          <input
            id="login-password"
            v-model="password"
            type="password"
            placeholder="Enter your password"
            :class="{ 'input-error': validationErrors.password }"
            autocomplete="current-password"
          />
          <span v-if="validationErrors.password" class="field-error">{{ validationErrors.password }}</span>
        </div>

        <div class="field-row">
          <label class="checkbox-label">
            <input v-model="rememberMe" type="checkbox" />
            <span class="checkbox-text">Remember me</span>
          </label>
        </div>

        <button type="submit" class="btn-login" :disabled="loading">
          <span v-if="loading" class="btn-spinner"></span>
          <span v-else>Sign in</span>
        </button>
      </form>

      <p class="login-footer">
        Don't have an account?
        <router-link to="/register" class="link">Create one</router-link>
      </p>
    </div>
  </div>
</template>


