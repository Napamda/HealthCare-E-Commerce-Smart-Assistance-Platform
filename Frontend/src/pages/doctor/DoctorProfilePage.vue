<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth.js'
import { getDoctorProfile, createDoctorProfile, updateDoctorProfile } from '../../services/doctor.js'
import '../../styles/components/profile.css'

const authStore = useAuthStore()
const loading = ref(true)
const saving = ref(false)
const message = ref('')
const messageType = ref('success')

const doctorForm = ref({
  clinicName: '',
  clinicAddress: '',
  clinicCity: '',
  clinicState: '',
  clinicPostalCode: '',
  clinicCountry: '',
  medicalLicenseNumber: '',
  licenseState: '',
  licenseExpirationDate: '',
  specialty: '',
  subSpecialty: '',
  bio: '',
  consultationFee: '',
  clinicPhone: '',
  clinicEmail: '',
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
    const profile = await getDoctorProfile()
    if (profile.id) {
      doctorForm.value = {
        clinicName: profile.clinicName || '',
        clinicAddress: profile.clinicAddress || '',
        clinicCity: profile.clinicCity || '',
        clinicState: profile.clinicState || '',
        clinicPostalCode: profile.clinicPostalCode || '',
        clinicCountry: profile.clinicCountry || '',
        medicalLicenseNumber: profile.medicalLicenseNumber || '',
        licenseState: profile.licenseState || '',
        licenseExpirationDate: profile.licenseExpirationDate || '',
        specialty: profile.specialty || '',
        subSpecialty: profile.subSpecialty || '',
        bio: profile.bio || '',
        consultationFee: profile.consultationFee || '',
        clinicPhone: profile.clinicPhone || '',
        clinicEmail: profile.clinicEmail || '',
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
  if (!doctorForm.value.clinicName.trim()) {
    flash('error', 'Clinic name is required.')
    return
  }
  saving.value = true
  try {
    if (doctorForm.value.id) {
      await updateDoctorProfile(doctorForm.value)
      flash('success', 'Doctor profile updated successfully.')
    } else {
      await createDoctorProfile(doctorForm.value)
      flash('success', 'Doctor profile created successfully.')
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
        <h1>Doctor Profile</h1>
        <p>Manage your medical practice information</p>
      </header>

      <div v-if="message" class="profile-message" :class="messageType">{{ message }}</div>

      <div v-if="loading" class="profile-loading">Loading your profile…</div>

      <template v-else>
        <section class="profile-card">
          <h2>Clinic Information</h2>
          <form class="profile-form" @submit.prevent="saveProfile">
            <div class="form-row">
              <label class="form-field">
                <span>Clinic Name *</span>
                <input v-model="doctorForm.clinicName" type="text" maxlength="200" required />
              </label>
              <label class="form-field">
                <span>Specialty *</span>
                <input v-model="doctorForm.specialty" type="text" maxlength="100" required />
              </label>
            </div>
            <div class="form-row">
              <label class="form-field">
                <span>Sub-specialty</span>
                <input v-model="doctorForm.subSpecialty" type="text" maxlength="100" />
              </label>
              <label class="form-field">
                <span>Consultation Fee</span>
                <input v-model="doctorForm.consultationFee" type="text" maxlength="100" placeholder="$100" />
              </label>
            </div>
            <label class="form-field form-field-wide">
              <span>Clinic Address</span>
              <input v-model="doctorForm.clinicAddress" type="text" maxlength="255" />
            </label>
            <div class="form-row">
              <label class="form-field">
                <span>City</span>
                <input v-model="doctorForm.clinicCity" type="text" maxlength="100" />
              </label>
              <label class="form-field">
                <span>State/Province</span>
                <input v-model="doctorForm.clinicState" type="text" maxlength="100" />
              </label>
              <label class="form-field">
                <span>Postal Code</span>
                <input v-model="doctorForm.clinicPostalCode" type="text" maxlength="20" />
              </label>
              <label class="form-field">
                <span>Country</span>
                <input v-model="doctorForm.clinicCountry" type="text" maxlength="100" />
              </label>
            </div>

            <h2>Contact Information</h2>
            <div class="form-row">
              <label class="form-field">
                <span>Clinic Phone</span>
                <input v-model="doctorForm.clinicPhone" type="tel" maxlength="50" />
              </label>
              <label class="form-field">
                <span>Clinic Email</span>
                <input v-model="doctorForm.clinicEmail" type="email" maxlength="255" />
              </label>
            </div>

            <h2>License Information</h2>
            <div class="form-row">
              <label class="form-field">
                <span>Medical License Number *</span>
                <input v-model="doctorForm.medicalLicenseNumber" type="text" maxlength="50" required />
              </label>
              <label class="form-field">
                <span>License State</span>
                <input v-model="doctorForm.licenseState" type="text" maxlength="20" />
              </label>
              <label class="form-field">
                <span>License Expiration Date</span>
                <input v-model="doctorForm.licenseExpirationDate" type="date" />
              </label>
            </div>

            <h2>Professional Bio</h2>
            <label class="form-field form-field-wide">
              <span>Bio</span>
              <textarea v-model="doctorForm.bio" maxlength="1000" rows="4" />
            </label>

            <label class="toggle-row">
              <input v-model="doctorForm.active" type="checkbox" />
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