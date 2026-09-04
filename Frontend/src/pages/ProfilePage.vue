<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../stores/user.js'
import { useAuthStore } from '../stores/auth.js'

const userStore = useUserStore()
const authStore = useAuthStore()

const profileForm = ref({
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  dateOfBirth: '',
})

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const editingProfile = ref(false)
const changingPassword = ref(false)
const message = ref('')
const messageType = ref('')

async function loadProfile() {
  await userStore.fetchProfile()
  if (userStore.profile) {
    profileForm.value = {
      firstName: userStore.profile.firstName || '',
      lastName: userStore.profile.lastName || '',
      email: userStore.profile.email || '',
      phone: userStore.profile.phone || '',
      dateOfBirth: userStore.profile.dateOfBirth || '',
    }
  }
}

async function saveProfile() {
  try {
    await userStore.updateProfile(profileForm.value)
    editingProfile.value = false
    showMessage('Profile updated successfully', 'success')
  } catch (error) {
    showMessage('Failed to update profile', 'error')
  }
}

async function savePassword() {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showMessage('Passwords do not match', 'error')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    showMessage('Password must be at least 6 characters', 'error')
    return
  }
  try {
    await userStore.updatePassword(passwordForm.value.currentPassword, passwordForm.value.newPassword)
    passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    changingPassword.value = false
    showMessage('Password changed successfully', 'success')
  } catch (error) {
    showMessage('Failed to change password', 'error')
  }
}

function showMessage(msg, type) {
  message.value = msg
  messageType.value = type
  setTimeout(() => {
    message.value = ''
  }, 3000)
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="profile-page">
    <div class="page-header">
      <h1>My Profile</h1>
      <p class="header-subtitle">Manage your account information and preferences</p>
    </div>

    <div v-if="message" :class="['alert', 'alert-' + messageType]">
      {{ message }}
    </div>

    <div class="profile-content">
      <!-- Profile Information -->
      <div class="profile-section">
        <div class="section-header">
          <h2>Personal Information</h2>
          <button v-if="!editingProfile" class="btn-edit" @click="editingProfile = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
            Edit
          </button>
        </div>

        <div class="profile-details">
          <div class="detail-item">
            <label>First Name</label>
            <input v-if="editingProfile" v-model="profileForm.firstName" type="text" />
            <span v-else>{{ profileForm.firstName || 'Not set' }}</span>
          </div>
          <div class="detail-item">
            <label>Last Name</label>
            <input v-if="editingProfile" v-model="profileForm.lastName" type="text" />
            <span v-else>{{ profileForm.lastName || 'Not set' }}</span>
          </div>
          <div class="detail-item">
            <label>Email</label>
            <input v-if="editingProfile" v-model="profileForm.email" type="email" />
            <span v-else>{{ profileForm.email || 'Not set' }}</span>
          </div>
          <div class="detail-item">
            <label>Phone</label>
            <input v-if="editingProfile" v-model="profileForm.phone" type="tel" />
            <span v-else>{{ profileForm.phone || 'Not set' }}</span>
          </div>
          <div class="detail-item">
            <label>Date of Birth</label>
            <input v-if="editingProfile" v-model="profileForm.dateOfBirth" type="date" />
            <span v-else>{{ profileForm.dateOfBirth || 'Not set' }}</span>
          </div>
        </div>

        <div v-if="editingProfile" class="section-actions">
          <button class="btn-cancel" @click="editingProfile = false">Cancel</button>
          <button class="btn-save" @click="saveProfile">Save Changes</button>
        </div>
      </div>

      <!-- Change Password -->
      <div class="profile-section">
        <div class="section-header">
          <h2>Change Password</h2>
          <button v-if="!changingPassword" class="btn-edit" @click="changingPassword = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            Change
          </button>
        </div>

        <div v-if="changingPassword" class="password-form">
          <div class="form-group">
            <label>Current Password</label>
            <input v-model="passwordForm.currentPassword" type="password" />
          </div>
          <div class="form-group">
            <label>New Password</label>
            <input v-model="passwordForm.newPassword" type="password" />
          </div>
          <div class="form-group">
            <label>Confirm New Password</label>
            <input v-model="passwordForm.confirmPassword" type="password" />
          </div>
          <div class="section-actions">
            <button class="btn-cancel" @click="changingPassword = false">Cancel</button>
            <button class="btn-save" @click="savePassword">Update Password</button>
          </div>
        </div>
        <div v-else class="placeholder-text">
          Your password was last changed recently. Click "Change" to update it.
        </div>
      </div>

      <!-- Account Settings -->
      <div class="profile-section">
        <div class="section-header">
          <h2>Account Settings</h2>
        </div>
        <div class="settings-list">
          <div class="setting-item">
            <div class="setting-info">
              <h3>Email Notifications</h3>
              <p>Receive updates about your orders and promotions</p>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" checked />
              <span class="slider"></span>
            </label>
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <h3>SMS Notifications</h3>
              <p>Get text alerts for order updates</p>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" />
              <span class="slider"></span>
            </label>
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <h3>Two-Factor Authentication</h3>
              <p>Add an extra layer of security to your account</p>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" />
              <span class="slider"></span>
            </label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 80vh;
}

.page-header {
  margin-bottom: 32px;
}
.page-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}
.header-subtitle {
  font-size: 15px;
  color: var(--color-text-muted);
}

.alert {
  padding: 12px 16px;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  font-size: 14px;
}
.alert-success {
  background: #d1fae5;
  color: #065f46;
  border: 1px solid #34d399;
}
.alert-error {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #f87171;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}
.section-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.btn-edit {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: transparent;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-edit:hover {
  background: var(--color-primary);
  color: #fff;
}

.profile-details {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.detail-item label {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--color-text-muted);
  letter-spacing: 0.5px;
}
.detail-item input {
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-bg);
  color: var(--color-text);
}
.detail-item input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.detail-item span {
  font-size: 15px;
  color: var(--color-text);
}

.password-form {
  display: flex;
  flex-direction: column;
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
  color: var(--color-text);
}
.form-group input {
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-bg);
  color: var(--color-text);
}
.form-group input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.placeholder-text {
  font-size: 14px;
  color: var(--color-text-muted);
  padding: 20px 0;
}

.section-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}
.btn-cancel {
  padding: 10px 20px;
  background: transparent;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
.btn-cancel:hover {
  background: var(--color-bg);
}
.btn-save {
  padding: 10px 20px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.btn-save:hover {
  background: #1d4ed8;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
}
.setting-info h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}
.setting-info p {
  font-size: 13px;
  color: var(--color-text-muted);
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 24px;
}
.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}
.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--color-border);
  transition: 0.3s;
  border-radius: 24px;
}
.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
}
input:checked + .slider {
  background-color: var(--color-primary);
}
input:checked + .slider:before {
  transform: translateX(24px);
}
</style>