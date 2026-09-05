<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth.js'
import { getPharmacistProfile, createPharmacistProfile, updatePharmacistProfile } from '../../services/pharmacist.js'
import '../../styles/components/profile.css'

const authStore = useAuthStore()
const loading = ref(true)
const saving = ref(false)
const message = ref('')
const messageType = ref('success')

const pharmacistForm = ref({
  pharmacyName: '',
  pharmacyAddress: '',
  pharmacyCity: '',
  pharmacyState: '',
  pharmacyPostalCode: '',
  pharmacyCountry: '',
  licenseNumber: '',
  licenseState: '',
  licenseExpirationDate: '',
  pharmacyPhone: '',
  pharmacyEmail: '',
  bio: '',
  specialization: '',
  active: true,
})

function flash(type, text) {
  messageType.value = type
  message.value = text
  setTimeout(() => {
    message.value = ''
  }, 4000)
}

function errText(e) {
  return e.response?.data?.error || e.message || 'Something went wrong'
}

onMounted(async () => {
  try {
    const profile = await getPharmacistProfile()
    if (profile.id) {
      pharmacistForm.value = {
        pharmacyName: profile.pharmacyName || '',
        pharmacyAddress: profile.pharmacyAddress || '',
        pharmacyCity: profile.pharmacyCity || '',
        pharmacyState: profile.pharmacyState || '',
        pharmacyPostalCode: profile.pharmacyPostalCode || '',
        pharmacyCountry: profile.pharmacyCountry || '',
        licenseNumber: profile.licenseNumber || '',
        licenseState: profile.licenseState || '',
        licenseExpirationDate: profile.licenseExpirationDate || '',
        pharmacyPhone: profile.pharmacyPhone || '',
        pharmacyEmail: profile.pharmacyEmail || '',
        bio: profile.bio || '',
        specialization: profile.specialization || '',
        active: profile.active,
      }
    }
  } catch (e) {
    flash('error', errText(e))
  } finally {
    loading.value = false
  }
})

async function saveProfile() {
  if (!pharmacistForm.value.pharmacyName.trim()) {
    flash('error', 'Pharmacy name is required.')
    return
  }
  saving.value = true
  try {
    if (pharmacistForm.value.id) {
      await updatePharmacistProfile(pharmacistForm.value)
      flash('success', 'Pharmacist profile updated successfully.')
    } else {
      await createPharmacistProfile(pharmacistForm.value)
      flash('success', 'Pharmacist profile created successfully.')
    }
  } catch (e) {
    flash('error', errText(e))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <div class="profile-container">
      <header class="profile-header">
        <h1>Pharmacist Profile</h1>
        <p>Manage your pharmacy practice information</p>
      </header>

      <div v-if="message" class="profile-message" :class="messageType">{{ message }}</div>

      <div v-if="loading" class="profile-loading">Loading your profile…</div>

      <template v-else>
        <section class="profile-card">
          <h2>Pharmacy Information</h2>
          <form class="profile-form" @submit.prevent="saveProfile">
            <div class="form-row">
              <label class="form-field">
                <span>Pharmacy Name *</span>
                <input v-model="pharmacistForm.pharmacyName" type="text" maxlength="200" required />
              </label>
              <label class="form-field">
                <span>Specialization *</span>
                <input v-model="pharmacistForm.specialization" type="text" maxlength="100" required />
              </label>
            </div>
            <label class="form-field form-field-wide">
              <span>Pharmacy Address</span>
              <input v-model="pharmacistForm.pharmacyAddress" type="text" maxlength="255" />
            </label>
            <div class="form-row">
              <label class="form-field">
                <span>City</span>
                <input v-model="pharmacistForm.pharmacyCity" type="text" maxlength="100" />
              </label>
              <label class="form-field">
                <span>State/Province</span>
                <input v-model="pharmacistForm.pharmacyState" type="text" maxlength="100" />
              </label>
              <label class="form-field">
                <span>Postal Code</span>
                <input v-model="pharmacistForm.pharmacyPostalCode" type="text" maxlength="20" />
              </label>
              <label class="form-field">
                <span>Country</span>
                <input v-model="pharmacistForm.pharmacyCountry" type="text" maxlength="100" />
              </label>
            </div>

            <h2>Contact Information</h2>
            <div class="form-row">
              <label class="form-field">
                <span>Pharmacy Phone</span>
                <input v-model="pharmacistForm.pharmacyPhone" type="tel" maxlength="50" />
              </label>
              <label class="form-field">
                <span>Pharmacy Email</span>
                <input v-model="pharmacistForm.pharmacyEmail" type="email" maxlength="255" />
              </label>
            </div>

            <h2>License Information</h2>
            <div class="form-row">
              <label class="form-field">
                <span>License Number *</span>
                <input v-model="pharmacistForm.licenseNumber" type="text" maxlength="50" required />
              </label>
              <label class="form-field">
                <span>License State</span>
                <input v-model="pharmacistForm.licenseState" type="text" maxlength="20" />
              </label>
              <label class="form-field">
                <span>License Expiration Date</span>
                <input v-model="pharmacistForm.licenseExpirationDate" type="date" />
              </label>
            </div>

            <h2>Professional Bio</h2>
            <label class="form-field form-field-wide">
              <span>Bio</span>
              <textarea v-model="pharmacistForm.bio" maxlength="1000" rows="4" />
            </label>

            <label class="toggle-row">
              <input v-model="pharmacistForm.active" type="checkbox" />
              <span><strong>Active Profile</strong> — show your profile in professional directory</span>
            </label>

            <div class="form-actions">
              <button type="submit" class="btn btn-primary" :disabled="saving">
                {{ saving ? 'Saving…' : 'Save Profile' }}
              </button>
            </div>
          </form>
        </section>
      </template>
    </div>
  </div>
</template>