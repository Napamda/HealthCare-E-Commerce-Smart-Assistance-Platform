<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../services/auth.js'

const router = useRouter()

const form = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  firstName: '',
  lastName: '',
  role: 'PATIENT',
})

const submitting = ref(false)
const error = ref('')
const success = ref(false)
const verificationLink = ref('')

const roles = [
  { value: 'PATIENT', label: 'Patient' },
  { value: 'PHARMACIST', label: 'Pharmacist' },
  { value: 'DOCTOR', label: 'Doctor' },
  { value: 'VENDOR', label: 'Vendor' },
  { value: 'ADMIN', label: 'Administrator' },
]

const fieldErrors = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  firstName: '',
  lastName: '',
})

function validateForm() {
  let valid = true
  Object.keys(fieldErrors).forEach((k) => (fieldErrors[k] = ''))

  if (!form.firstName.trim()) {
    fieldErrors.firstName = 'First name is required'
    valid = false
  }

  if (!form.lastName.trim()) {
    fieldErrors.lastName = 'Last name is required'
    valid = false
  }

  if (!form.email.trim()) {
    fieldErrors.email = 'Email is required'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    fieldErrors.email = 'Enter a valid email address'
    valid = false
  }

  if (!form.password) {
    fieldErrors.password = 'Password is required'
    valid = false
  } else if (form.password.length < 8) {
    fieldErrors.password = 'Password must be at least 8 characters'
    valid = false
  } else if (!/(?=.*[A-Z])(?=.*[a-z])(?=.*\d)/.test(form.password)) {
    fieldErrors.password =
      'Must contain uppercase, lowercase, and a digit'
    valid = false
  }

  if (form.password !== form.confirmPassword) {
    fieldErrors.confirmPassword = 'Passwords do not match'
    valid = false
  }

  return valid
}

async function handleSubmit() {
  error.value = ''
  success.value = false

  if (!validateForm()) return

  submitting.value = true
  try {
    const result = await register({
      email: form.email.trim(),
      password: form.password,
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      role: form.role,
    })
    success.value = true
    verificationLink.value = result.verificationLink || ''
  } catch (e) {
    const msg = e.response?.data?.error
    if (Array.isArray(msg)) {
      error.value = msg.join('; ')
    } else {
      error.value = msg || 'Registration failed. Please try again.'
    }
  } finally {
    submitting.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">
      <div class="card-header">
        <h1>Create Account</h1>
        <p>Join the HealthCare platform</p>
      </div>

      <div v-if="success" class="success-state">
        <div class="success-icon">&#10003;</div>
        <h2>Registration Successful</h2>
        <p>Your account has been created. Please verify your email to activate your account.</p>
        <div v-if="verificationLink" class="verification-info">
          <p class="hint">Verification link (for testing):</p>
          <code class="verification-link">{{ verificationLink }}</code>
        </div>
        <button class="btn btn-primary btn-full" @click="goToLogin">
          Go to Login
        </button>
      </div>

      <form v-else @submit.prevent="handleSubmit" class="register-form">
        <div v-if="error" class="form-error">
          <p>{{ error }}</p>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label for="firstName">First Name</label>
            <input
              id="firstName"
              v-model="form.firstName"
              type="text"
              placeholder="John"
              :class="{ 'input-error': fieldErrors.firstName }"
            />
            <span v-if="fieldErrors.firstName" class="field-error">{{ fieldErrors.firstName }}</span>
          </div>
          <div class="form-group">
            <label for="lastName">Last Name</label>
            <input
              id="lastName"
              v-model="form.lastName"
              type="text"
              placeholder="Doe"
              :class="{ 'input-error': fieldErrors.lastName }"
            />
            <span v-if="fieldErrors.lastName" class="field-error">{{ fieldErrors.lastName }}</span>
          </div>
        </div>

        <div class="form-group">
          <label for="email">Email</label>
          <input
            id="email"
            v-model="form.email"
            type="email"
            placeholder="john.doe@example.com"
            :class="{ 'input-error': fieldErrors.email }"
          />
          <span v-if="fieldErrors.email" class="field-error">{{ fieldErrors.email }}</span>
        </div>

        <div class="form-group">
          <label for="password">Password</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            placeholder="Min. 8 characters with uppercase, lowercase, and digit"
            :class="{ 'input-error': fieldErrors.password }"
          />
          <span v-if="fieldErrors.password" class="field-error">{{ fieldErrors.password }}</span>
        </div>

        <div class="form-group">
          <label for="confirmPassword">Confirm Password</label>
          <input
            id="confirmPassword"
            v-model="form.confirmPassword"
            type="password"
            placeholder="Re-enter your password"
            :class="{ 'input-error': fieldErrors.confirmPassword }"
          />
          <span v-if="fieldErrors.confirmPassword" class="field-error">{{ fieldErrors.confirmPassword }}</span>
        </div>

        <div class="form-group">
          <label for="role">Account Type</label>
          <select id="role" v-model="form.role" class="role-select">
            <option v-for="r in roles" :key="r.value" :value="r.value">
              {{ r.label }}
            </option>
          </select>
        </div>

        <button
          type="submit"
          class="btn btn-primary btn-full"
          :disabled="submitting"
        >
          <span v-if="submitting" class="btn-spinner"></span>
          {{ submitting ? 'Creating Account...' : 'Create Account' }}
        </button>

        <p class="form-footer">
          Already have an account?
          <router-link to="/login" class="link">Sign in</router-link>
        </p>
      </form>
    </div>
  </div>
</template>


