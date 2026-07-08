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

function goToChat() {
  router.push('/chat')
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
        <button class="btn btn-primary btn-full" @click="goToChat">
          Continue to Chat
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

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--color-bg);
}

.register-card {
  width: 100%;
  max-width: 520px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: 40px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h1 {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
}

.card-header p {
  font-size: 14px;
  color: var(--color-text-muted);
}

.success-state {
  text-align: center;
}

.success-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: #16a34a;
  color: #fff;
  border-radius: 50%;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 16px;
}

.success-state h2 {
  font-size: 20px;
  color: var(--color-text);
  margin-bottom: 8px;
}

.success-state p {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: 20px;
}

.verification-info {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px;
  margin-bottom: 20px;
  text-align: left;
}

.verification-info .hint {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 6px;
}

.verification-link {
  display: block;
  font-size: 12px;
  color: var(--color-primary);
  word-break: break-all;
  line-height: 1.5;
}

.form-error {
  padding: 12px 16px;
  background: #fee2e2;
  border: 1px solid #fecaca;
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}

.form-error p {
  font-size: 13px;
  color: #991b1b;
  margin: 0;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.form-group input,
.role-select {
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text);
  background: var(--color-bg);
  outline: none;
  transition: border-color 0.15s ease;
  font-family: inherit;
}

.form-group input:focus,
.role-select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-group input::placeholder {
  color: var(--color-text-muted);
}

.input-error {
  border-color: #dc2626 !important;
}

.field-error {
  font-size: 12px;
  color: #dc2626;
}

.role-select {
  cursor: pointer;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  transition: all 0.15s ease;
  border: none;
  cursor: pointer;
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-full {
  width: 100%;
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
  to { transform: rotate(360deg); }
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: var(--color-text-muted);
}

.link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 600;
}

.link:hover {
  text-decoration: underline;
}

@media (max-width: 480px) {
  .register-card {
    padding: 24px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
