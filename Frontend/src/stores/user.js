import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserProfile, updateUserProfile, changePassword, getUserAddresses, addAddress, updateAddress, deleteAddress } from '../services/user.js'

export const useUserStore = defineStore('user', () => {
  const profile = ref(null)
  const addresses = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchProfile() {
    loading.value = true
    error.value = null
    try {
      profile.value = await getUserProfile()
    } catch (err) {
      error.value = 'Failed to load profile'
      console.error('Failed to fetch profile:', err)
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(profileData) {
    loading.value = true
    error.value = null
    try {
      profile.value = await updateUserProfile(profileData)
      return profile.value
    } catch (err) {
      error.value = 'Failed to update profile'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updatePassword(currentPassword, newPassword) {
    loading.value = true
    error.value = null
    try {
      await changePassword(currentPassword, newPassword)
    } catch (err) {
      error.value = 'Failed to change password'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchAddresses() {
    loading.value = true
    error.value = null
    try {
      addresses.value = await getUserAddresses()
    } catch (err) {
      error.value = 'Failed to load addresses'
      console.error('Failed to fetch addresses:', err)
    } finally {
      loading.value = false
    }
  }

  async function addNewAddress(addressData) {
    loading.value = true
    error.value = null
    try {
      const newAddress = await addAddress(addressData)
      addresses.value.push(newAddress)
      return newAddress
    } catch (err) {
      error.value = 'Failed to add address'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateExistingAddress(addressId, addressData) {
    loading.value = true
    error.value = null
    try {
      const updated = await updateAddress(addressId, addressData)
      const index = addresses.value.findIndex(a => a.id === addressId)
      if (index !== -1) addresses.value[index] = updated
      return updated
    } catch (err) {
      error.value = 'Failed to update address'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function removeAddress(addressId) {
    loading.value = true
    error.value = null
    try {
      await deleteAddress(addressId)
      addresses.value = addresses.value.filter(a => a.id !== addressId)
    } catch (err) {
      error.value = 'Failed to delete address'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    profile,
    addresses,
    loading,
    error,
    fetchProfile,
    updateProfile,
    updatePassword,
    fetchAddresses,
    addNewAddress,
    updateExistingAddress,
    removeAddress,
    clearError,
  }
})