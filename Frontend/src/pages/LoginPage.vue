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

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 32px 16px;
  background: var(--color-bg);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 36px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.login-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 6px;
}

.login-header p {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.alert {
  padding: 12px 16px;
  border-radius: var(--radius-md, 8px);
  font-size: 13px;
  margin-bottom: 20px;
}

.alert-error {
  background: #fee2e2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.field input[type="email"],
.field input[type="password"] {
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  font-size: 14px;
  color: var(--color-text);
  background: var(--color-bg);
  outline: none;
  transition: border-color 0.15s ease;
  box-sizing: border-box;
  width: 100%;
}

.field input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.field input.input-error {
  border-color: #dc2626;
}

.field-error {
  font-size: 12px;
  color: #dc2626;
}

.field-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--color-text);
}

.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
  cursor: pointer;
}

.checkbox-text {
  user-select: none;
}

.btn-login {
  padding: 12px 24px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md, 8px);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-login:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn-login:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
}

.link:hover {
  text-decoration: underline;
}
</style>
