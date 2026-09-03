<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { verifyEmail } from '../../services/auth.js'

const route = useRoute()
const router = useRouter()

const state = ref('loading')
const message = ref('')

async function handleVerify() {
  const token = route.query.token

  if (!token) {
    state.value = 'error'
    message.value = 'This verification link is invalid or incomplete.'
    return
  }

  state.value = 'loading'

  try {
    const result = await verifyEmail(token)
    message.value = result.message || 'Email verified successfully. You can now log in.'
    state.value = 'success'
  } catch (e) {
    const serverError = e.response?.data?.error
    message.value =
      serverError || 'Verification failed. The link may be invalid or expired.'
    state.value = 'error'
  }
}

onMounted(handleVerify)

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="verify-page">
    <div class="verify-card">
      <div class="card-header">
        <h1>Email Verification</h1>
        <p>Confirm your email address to activate your account</p>
      </div>

      <div v-if="state === 'loading'" class="verify-state">
        <div class="spinner"></div>
        <p>Verifying your email address...</p>
      </div>

      <div v-else-if="state === 'success'" class="verify-state">
        <div class="success-icon">&#10003;</div>
        <h2>Email Verified</h2>
        <p>{{ message }}</p>
        <button class="btn btn-primary btn-full" @click="goToLogin">
          Go to Login
        </button>
      </div>

      <div v-else class="verify-state">
        <div class="error-icon">&#10005;</div>
        <h2>Verification Failed</h2>
        <p>{{ message }}</p>
        <button class="btn btn-primary btn-full" @click="goToLogin">
          Go to Login
        </button>
      </div>
    </div>
  </div>
</template>
