<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../../services/auth.js'

const router = useRouter()

const form = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  firstName: '',
  lastName: '',
  role: '',
  dateOfBirth: '',
  clinicName: '',
  medicalLicenseNumber: '',
  specialty: '',
  pharmacyName: '',
  pharmacistLicenseNumber: '',
  specialization: '',
  businessName: '',
  businessLicense: '',
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
  role: '',
  email: '',
  password: '',
  confirmPassword: '',
  firstName: '',
  lastName: '',
  roleDetails: '',
})

function validateForm() {
  let valid = true
  Object.keys(fieldErrors).forEach((k) => (fieldErrors[k] = ''))

  if (!form.role) {
    fieldErrors.role = 'Select an account type first'
    valid = false
  }

  if (form.role === 'DOCTOR' && !form.clinicName.trim()) {
    fieldErrors.roleDetails = 'Clinic name is required'
    valid = false
  } else if (form.role === 'PHARMACIST' && !form.pharmacyName.trim()) {
    fieldErrors.roleDetails = 'Pharmacy name is required'
    valid = false
  } else if (form.role === 'VENDOR' && !form.businessName.trim()) {
    fieldErrors.roleDetails = 'Business name is required'
    valid = false
  }

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
      dateOfBirth: form.dateOfBirth || undefined,
      clinicName: form.clinicName.trim() || undefined,
      medicalLicenseNumber: form.medicalLicenseNumber.trim() || undefined,
      specialty: form.specialty.trim() || undefined,
      pharmacyName: form.pharmacyName.trim() || undefined,
      pharmacistLicenseNumber: form.pharmacistLicenseNumber.trim() || undefined,
      specialization: form.specialization.trim() || undefined,
      businessName: form.businessName.trim() || undefined,
      businessLicense: form.businessLicense.trim() || undefined,
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

function goVerifyNow() {
  try {
    const token = new URL(verificationLink.value).searchParams.get('token')
    if (token) router.push({ path: '/verify-email', query: { token } })
  } catch (_) {}
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
        <div class="verify-actions">
          <button class="btn btn-primary btn-full" @click="goVerifyNow">
            Verify Email Now
          </button>
          <button class="btn btn-secondary btn-full" @click="goToLogin">
            Go to Login
          </button>
        </div>
      </div>

      <form v-else @submit.prevent="handleSubmit" class="register-form">
        <div v-if="error" class="form-error">
          <p>{{ error }}</p>
        </div>

        <fieldset class="role-picker">
          <legend>Choose your account type</legend>
          <label v-for="r in roles" :key="r.value" class="role-option" :class="{ selected: form.role === r.value }">
            <input v-model="form.role" type="radio" name="role" :value="r.value" />
            <span>{{ r.label }}</span>
          </label>
        </fieldset>
        <span v-if="fieldErrors.role" class="field-error">{{ fieldErrors.role }}</span>

        <template v-if="form.role">
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

        <div v-if="form.role === 'PATIENT'" class="form-group">
          <label for="dateOfBirth">Date of Birth</label>
          <input id="dateOfBirth" v-model="form.dateOfBirth" type="date" />
        </div>

        <div v-if="form.role === 'DOCTOR'" class="role-fields">
          <div class="form-group"><label for="clinicName">Clinic Name *</label><input id="clinicName" v-model="form.clinicName" type="text" /></div>
          <div class="form-row">
            <div class="form-group"><label for="medicalLicenseNumber">Medical License</label><input id="medicalLicenseNumber" v-model="form.medicalLicenseNumber" type="text" /></div>
            <div class="form-group"><label for="specialty">Specialty</label><input id="specialty" v-model="form.specialty" type="text" /></div>
          </div>
        </div>

        <div v-if="form.role === 'PHARMACIST'" class="role-fields">
          <div class="form-group"><label for="pharmacyName">Pharmacy Name *</label><input id="pharmacyName" v-model="form.pharmacyName" type="text" /></div>
          <div class="form-row">
            <div class="form-group"><label for="pharmacistLicenseNumber">License Number</label><input id="pharmacistLicenseNumber" v-model="form.pharmacistLicenseNumber" type="text" /></div>
            <div class="form-group"><label for="specialization">Specialization</label><input id="specialization" v-model="form.specialization" type="text" /></div>
          </div>
        </div>

        <div v-if="form.role === 'VENDOR'" class="role-fields">
          <div class="form-group"><label for="businessName">Business Name *</label><input id="businessName" v-model="form.businessName" type="text" /></div>
          <div class="form-group"><label for="businessLicense">Business License</label><input id="businessLicense" v-model="form.businessLicense" type="text" /></div>
        </div>
        <span v-if="fieldErrors.roleDetails" class="field-error">{{ fieldErrors.roleDetails }}</span>

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
        </template>
      </form>
    </div>
  </div>
</template>

