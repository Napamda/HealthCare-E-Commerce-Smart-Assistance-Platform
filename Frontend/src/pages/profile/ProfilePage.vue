<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '../../stores/auth.js'
import { ROLE_LABELS } from '../../config/permissions.js'
import {
  getProfile,
  updateProfile,
  uploadAvatar,
  getHealthProfile,
  updateHealthProfile,
  getAddresses,
  addAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress,
} from '../../services/user.js'
import '../../styles/components/profile.css'

const authStore = useAuthStore()

const activeTab = ref('profile')
const loading = ref(true)
const saving = ref(false)
const message = ref('')
const messageType = ref('success')

// ---- Task 2.1 — Profile ----
const profileForm = ref({
  firstName: '',
  lastName: '',
  phone: '',
  dateOfBirth: '',
  gender: '',
})
const avatarUrl = ref(null)
const avatarFileInput = ref(null)
const uploadingAvatar = ref(false)

// ---- Task 2.2 — Health Profile ----
const healthForm = ref({
  allergies: [],
  chronicConditions: [],
  emergencyContacts: [],
  dataProcessingConsent: false,
  emailNotifications: true,
  profileVisible: false,
  shareHealthDataWithDoctors: true,
})
const allergyInput = ref('')
const conditionInput = ref('')

// ---- Task 2.3 — Addresses ----
const addresses = ref([])
const addressForm = ref(null) // null = form closed
const addressFormMode = ref('add') // 'add' | 'edit'
const editingAddressId = ref(null)

const roleLabel = computed(() => ROLE_LABELS[authStore.userRole] || authStore.userRole || '')
const initials = computed(() => {
  const f = profileForm.value.firstName || authStore.currentUser?.firstName || ''
  return f.charAt(0).toUpperCase() || '?'
})

function flash(type, text) {
  messageType.value = type
  message.value = text
  setTimeout(() => {
    message.value = ''
  }, 4000)
}

function errText(e) {
  return e.response?.data?.error || (Array.isArray(e.response?.data) ? e.response.data[0] : null) || e.message || 'Something went wrong'
}

onMounted(async () => {
  try {
    const [profile, health, addressList] = await Promise.all([
      getProfile(),
      getHealthProfile(),
      getAddresses(),
    ])
    profileForm.value = {
      firstName: profile.firstName || '',
      lastName: profile.lastName || '',
      phone: profile.phone || '',
      dateOfBirth: profile.dateOfBirth || '',
      gender: profile.gender || '',
    }
    avatarUrl.value = profile.avatarUrl || null

    healthForm.value = {
      allergies: health.allergies || [],
      chronicConditions: health.chronicConditions || [],
      emergencyContacts: health.emergencyContacts || [],
      dataProcessingConsent: health.dataProcessingConsent,
      emailNotifications: health.emailNotifications,
      profileVisible: health.profileVisible,
      shareHealthDataWithDoctors: health.shareHealthDataWithDoctors,
    }
    addresses.value = addressList
  } catch (e) {
    flash('error', errText(e))
  } finally {
    loading.value = false
  }
})

// =====================================================================
// Task 2.1 — Profile
// =====================================================================

async function saveProfile() {
  if (!profileForm.value.firstName.trim() || !profileForm.value.lastName.trim()) {
    flash('error', 'First name and last name are required.')
    return
  }
  saving.value = true
  try {
    const updated = await updateProfile({
      firstName: profileForm.value.firstName.trim(),
      lastName: profileForm.value.lastName.trim(),
      phone: profileForm.value.phone || null,
      dateOfBirth: profileForm.value.dateOfBirth || null,
      gender: profileForm.value.gender || null,
    })
    authStore.setUserFields({
      firstName: updated.firstName,
      lastName: updated.lastName,
      avatarUrl: updated.avatarUrl,
    })
    avatarUrl.value = updated.avatarUrl
    flash('success', 'Profile updated successfully.')
  } catch (e) {
    flash('error', errText(e))
  } finally {
    saving.value = false
  }
}

function triggerAvatarPicker() {
  avatarFileInput.value?.click()
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    flash('error', 'Avatar image must be 2MB or smaller.')
    return
  }
  uploadingAvatar.value = true
  try {
    const updated = await uploadAvatar(file)
    avatarUrl.value = updated.avatarUrl
    authStore.setUserFields({ avatarUrl: updated.avatarUrl })
    flash('success', 'Avatar updated.')
  } catch (e) {
    flash('error', errText(e))
  } finally {
    uploadingAvatar.value = false
  }
}

// =====================================================================
// Task 2.2 — Health Profile
// =====================================================================

function addChip(listName, inputValue) {
  const value = inputValue.trim()
  if (!value) return
  const list = healthForm.value[listName]
  if (list.some((v) => v.toLowerCase() === value.toLowerCase())) {
    return
  }
  list.push(value)
  // Clear the input
  if (listName === 'allergies') {
    allergyInput.value = ''
  } else if (listName === 'chronicConditions') {
    conditionInput.value = ''
  }
}

function removeChip(listName, index) {
  healthForm.value[listName].splice(index, 1)
}

function addEmergencyContact() {
  healthForm.value.emergencyContacts.push({ name: '', phone: '', relationship: '' })
}

function removeEmergencyContact(index) {
  healthForm.value.emergencyContacts.splice(index, 1)
}

async function saveHealthProfile() {
  saving.value = true
  try {
    const saved = await updateHealthProfile({
      allergies: healthForm.value.allergies,
      chronicConditions: healthForm.value.chronicConditions,
      emergencyContacts: healthForm.value.emergencyContacts.filter((c) => c.name.trim()),
      dataProcessingConsent: healthForm.value.dataProcessingConsent,
      emailNotifications: healthForm.value.emailNotifications,
      profileVisible: healthForm.value.profileVisible,
      shareHealthDataWithDoctors: healthForm.value.shareHealthDataWithDoctors,
    })
    healthForm.value = {
      allergies: saved.allergies || [],
      chronicConditions: saved.chronicConditions || [],
      emergencyContacts: saved.emergencyContacts || [],
      dataProcessingConsent: saved.dataProcessingConsent,
      emailNotifications: saved.emailNotifications,
      profileVisible: saved.profileVisible,
      shareHealthDataWithDoctors: saved.shareHealthDataWithDoctors,
    }
    flash('success', 'Health profile saved.')
  } catch (e) {
    flash('error', errText(e))
  } finally {
    saving.value = false
  }
}

// =====================================================================
// Task 2.3 — Address Management
// =====================================================================

function openAddAddress() {
  addressFormMode.value = 'add'
  editingAddressId.value = null
  addressForm.value = {
    label: '',
    recipientName: profileForm.value.firstName + ' ' + profileForm.value.lastName,
    phone: profileForm.value.phone || '',
    street: '',
    city: '',
    state: '',
    postalCode: '',
    country: '',
    isDefault: addresses.value.length === 0,
  }
}

function openEditAddress(address) {
  addressFormMode.value = 'edit'
  editingAddressId.value = address.id
  addressForm.value = {
    label: address.label || '',
    recipientName: address.recipientName,
    phone: address.phone,
    street: address.street,
    city: address.city,
    state: address.state || '',
    postalCode: address.postalCode || '',
    country: address.country || '',
    isDefault: address.isDefault,
  }
}

function closeAddressForm() {
  addressForm.value = null
  editingAddressId.value = null
}

async function saveAddress() {
  const form = addressForm.value
  if (!form.recipientName.trim() || !form.phone.trim() || !form.street.trim() || !form.city.trim()) {
    flash('error', 'Recipient name, phone, street and city are required.')
    return
  }
  saving.value = true
  try {
    const payload = {
      label: form.label || null,
      recipientName: form.recipientName.trim(),
      phone: form.phone.trim(),
      street: form.street.trim(),
      city: form.city.trim(),
      state: form.state || null,
      postalCode: form.postalCode || null,
      country: form.country || null,
      isDefault: form.isDefault,
    }
    if (addressFormMode.value === 'add') {
      const created = await addAddress(payload)
      addresses.value.unshift(created)
      flash('success', 'Address added.')
    } else {
      const updated = await updateAddress(editingAddressId.value, payload)
      const idx = addresses.value.findIndex((a) => a.id === editingAddressId.value)
      if (idx !== -1) addresses.value.splice(idx, 1, updated)
      flash('success', 'Address updated.')
    }
    if (payload.isDefault) {
      addresses.value.forEach((a) => {
        a.isDefault = addresses.value.indexOf(a) === addresses.value.findIndex((x) => x.isDefault)
      })
      addresses.value = [...addresses.value]
    }
    closeAddressForm()
  } catch (e) {
    flash('error', errText(e))
  } finally {
    saving.value = false
  }
}

async function removeAddress(address) {
  if (!confirm('Delete this address?')) return
  try {
    await deleteAddress(address.id)
    addresses.value = addresses.value.filter((a) => a.id !== address.id)
    flash('success', 'Address deleted.')
  } catch (e) {
    flash('error', errText(e))
  }
}

async function makeDefault(address) {
  try {
    const updated = await setDefaultAddress(address.id)
    addresses.value = addresses.value.map((a) => ({ ...a, isDefault: a.id === updated.id }))
    flash('success', 'Default address updated.')
  } catch (e) {
    flash('error', errText(e))
  }
}
</script>

<template>
  <div class="profile-page">
    <div class="profile-container">
      <header class="profile-header">
        <div class="profile-avatar" @click="triggerAvatarPicker" title="Change avatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="Avatar" />
          <span v-else>{{ initials }}</span>
          <span class="profile-avatar-edit" :class="{ uploading: uploadingAvatar }">
            {{ uploadingAvatar ? '…' : '✎' }}
          </span>
        </div>
        <input
          ref="avatarFileInput"
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          class="profile-avatar-input"
          @change="handleAvatarChange"
        />
        <div class="profile-header-info">
          <h1>My Profile</h1>
          <p>
            <span class="profile-role-badge">{{ roleLabel }}</span>
            {{ authStore.currentUser?.email }}
          </p>
        </div>
      </header>

      <div v-if="message" class="profile-message" :class="messageType">{{ message }}</div>

      <div v-if="loading" class="profile-loading">Loading your profile…</div>

      <template v-else>
        <nav class="profile-tabs">
          <button
            class="profile-tab"
            :class="{ active: activeTab === 'profile' }"
            @click="activeTab = 'profile'"
          >
            Profile
          </button>
          <button
            class="profile-tab"
            :class="{ active: activeTab === 'health' }"
            @click="activeTab = 'health'"
          >
            Health Profile
          </button>
          <button
            class="profile-tab"
            :class="{ active: activeTab === 'addresses' }"
            @click="activeTab = 'addresses'"
          >
            Addresses
          </button>
        </nav>

        <!-- ===================== Profile tab ===================== -->
        <section v-show="activeTab === 'profile'" class="profile-card">
          <h2>Personal Information</h2>
          <form class="profile-form" @submit.prevent="saveProfile">
            <div class="form-row">
              <label class="form-field">
                <span>First name *</span>
                <input v-model="profileForm.firstName" type="text" maxlength="100" required />
              </label>
              <label class="form-field">
                <span>Last name *</span>
                <input v-model="profileForm.lastName" type="text" maxlength="100" required />
              </label>
            </div>
            <div class="form-row">
              <label class="form-field">
                <span>Phone</span>
                <input v-model="profileForm.phone" type="tel" placeholder="+250 788 123 456" />
              </label>
              <label class="form-field">
                <span>Date of birth</span>
                <input v-model="profileForm.dateOfBirth" type="date" />
              </label>
              <label class="form-field">
                <span>Gender</span>
                <select v-model="profileForm.gender">
                  <option value="">Prefer not to say</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </label>
            </div>
            <div class="form-actions">
              <button type="submit" class="btn btn-primary" :disabled="saving">
                {{ saving ? 'Saving…' : 'Save Changes' }}
              </button>
            </div>
          </form>
        </section>

        <!-- ===================== Health tab ===================== -->
        <section v-show="activeTab === 'health'" class="profile-card">
          <h2>Health Profile</h2>

          <div class="health-section">
            <h3>Allergies</h3>
            <div class="chip-input">
              <div class="chips">
                <span v-for="(a, i) in healthForm.allergies" :key="'a' + i" class="chip chip-allergy">
                  {{ a }}
                  <button type="button" @click="removeChip('allergies', i)">×</button>
                </span>
                <span v-if="healthForm.allergies.length === 0" class="chips-empty">No allergies recorded</span>
              </div>
              <input
                v-model="allergyInput"
                type="text"
                placeholder="Type an allergy and press Enter"
                @keydown.enter.prevent="addChip('allergies', allergyInput)"
              />
            </div>
          </div>

          <div class="health-section">
            <h3>Chronic Conditions</h3>
            <div class="chip-input">
              <div class="chips">
                <span v-for="(c, i) in healthForm.chronicConditions" :key="'c' + i" class="chip chip-condition">
                  {{ c }}
                  <button type="button" @click="removeChip('chronicConditions', i)">×</button>
                </span>
                <span v-if="healthForm.chronicConditions.length === 0" class="chips-empty">No conditions recorded</span>
              </div>
              <input
                v-model="conditionInput"
                type="text"
                placeholder="Type a condition and press Enter"
                @keydown.enter.prevent="addChip('chronicConditions', conditionInput)"
              />
            </div>
          </div>

          <div class="health-section">
            <div class="health-section-header">
              <h3>Emergency Contacts</h3>
              <button type="button" class="btn btn-ghost" @click="addEmergencyContact">+ Add Contact</button>
            </div>
            <div
              v-for="(contact, i) in healthForm.emergencyContacts"
              :key="'ec' + i"
              class="contact-row"
            >
              <input v-model="contact.name" type="text" placeholder="Name" />
              <input v-model="contact.phone" type="tel" placeholder="Phone" />
              <input v-model="contact.relationship" type="text" placeholder="Relationship" />
              <button type="button" class="btn btn-danger-ghost" @click="removeEmergencyContact(i)">Remove</button>
            </div>
            <p v-if="healthForm.emergencyContacts.length === 0" class="chips-empty">
              No emergency contacts yet.
            </p>
          </div>

          <div class="health-section">
            <h3>Consent &amp; Privacy</h3>
            <label class="toggle-row">
              <input v-model="healthForm.dataProcessingConsent" type="checkbox" />
              <span>
                <strong>Data processing consent</strong> — allow the platform to process my
                personal data to provide its services.
              </span>
            </label>
            <label class="toggle-row">
              <input v-model="healthForm.emailNotifications" type="checkbox" />
              <span><strong>Email notifications</strong> — receive newsletters and product updates.</span>
            </label>
            <label class="toggle-row">
              <input v-model="healthForm.profileVisible" type="checkbox" />
              <span><strong>Visible profile</strong> — let healthcare professionals see my profile.</span>
            </label>
            <label class="toggle-row">
              <input v-model="healthForm.shareHealthDataWithDoctors" type="checkbox" />
              <span><strong>Share health data with doctors</strong> — doctors I consult with can view my health profile.</span>
            </label>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-primary" :disabled="saving" @click="saveHealthProfile">
              {{ saving ? 'Saving…' : 'Save Health Profile' }}
            </button>
          </div>
        </section>

        <!-- ===================== Addresses tab ===================== -->
        <section v-show="activeTab === 'addresses'" class="profile-card">
          <div class="health-section-header">
            <h2>Saved Addresses</h2>
            <button v-if="!addressForm" type="button" class="btn btn-primary" @click="openAddAddress">
              + Add Address
            </button>
          </div>

          <div v-if="addressForm" class="address-form-card">
            <h3>{{ addressFormMode === 'add' ? 'New Address' : 'Edit Address' }}</h3>
            <form @submit.prevent="saveAddress">
              <div class="form-row">
                <label class="form-field">
                  <span>Label</span>
                  <input v-model="addressForm.label" type="text" placeholder="Home, Work…" maxlength="50" />
                </label>
                <label class="form-field">
                  <span>Recipient name *</span>
                  <input v-model="addressForm.recipientName" type="text" maxlength="100" required />
                </label>
                <label class="form-field">
                  <span>Phone *</span>
                  <input v-model="addressForm.phone" type="tel" required />
                </label>
              </div>
              <div class="form-row">
                <label class="form-field form-field-wide">
                  <span>Street *</span>
                  <input v-model="addressForm.street" type="text" maxlength="255" required />
                </label>
                <label class="form-field">
                  <span>City *</span>
                  <input v-model="addressForm.city" type="text" maxlength="100" required />
                </label>
              </div>
              <div class="form-row">
                <label class="form-field">
                  <span>State / Province</span>
                  <input v-model="addressForm.state" type="text" maxlength="100" />
                </label>
                <label class="form-field">
                  <span>Postal code</span>
                  <input v-model="addressForm.postalCode" type="text" maxlength="20" />
                </label>
                <label class="form-field">
                  <span>Country</span>
                  <input v-model="addressForm.country" type="text" maxlength="100" />
                </label>
              </div>
              <label class="toggle-row">
                <input v-model="addressForm.isDefault" type="checkbox" />
                <span>Set as default address</span>
              </label>
              <div class="form-actions">
                <button type="submit" class="btn btn-primary" :disabled="saving">
                  {{ saving ? 'Saving…' : 'Save Address' }}
                </button>
                <button type="button" class="btn btn-ghost" @click="closeAddressForm">Cancel</button>
              </div>
            </form>
          </div>

          <div v-else class="address-grid">
            <div
              v-for="address in addresses"
              :key="address.id"
              class="address-card"
              :class="{ 'is-default': address.isDefault }"
            >
              <div class="address-card-header">
                <span class="address-label">{{ address.label || 'Address' }}</span>
                <span v-if="address.isDefault" class="address-default-badge">Default</span>
              </div>
              <p class="address-line">{{ address.recipientName }} · {{ address.phone }}</p>
              <p class="address-line">
                {{ address.street }}, {{ address.city }}{{ address.state ? ', ' + address.state : ''
                }}{{ address.postalCode ? ' ' + address.postalCode : '' }}
              </p>
              <p v-if="address.country" class="address-line">{{ address.country }}</p>
              <div class="address-card-actions">
                <button v-if="!address.isDefault" type="button" class="btn btn-ghost" @click="makeDefault(address)">
                  Set Default
                </button>
                <button type="button" class="btn btn-ghost" @click="openEditAddress(address)">Edit</button>
                <button type="button" class="btn btn-danger-ghost" @click="removeAddress(address)">Delete</button>
              </div>
            </div>
            <div v-if="addresses.length === 0" class="profile-empty">
              No saved addresses yet. Add one for faster checkout.
            </div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>
