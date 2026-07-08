import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logoutRequest, refreshAccessToken } from '../services/auth.js'
import { ROLES, getPermissionsForRole, canAccessRoute as checkRouteAccess } from '../config/permissions.js'

export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref(restoreUser())
  const accessToken = ref(localStorage.getItem('accessToken') || null)
  const refreshToken = ref(localStorage.getItem('refreshToken') || null)
  const loading = ref(false)
  const error = ref(null)

  const isAuthenticated = computed(() => currentUser.value !== null && accessToken.value !== null)

  const userRole = computed(() => currentUser.value?.role || null)

  const permissions = computed(() => {
    if (!currentUser.value?.role) return []
    return getPermissionsForRole(currentUser.value.role)
  })

  const isAdmin = computed(() => userRole.value === ROLES.ADMIN)

  function hasRole(...roles) {
    if (!currentUser.value) return false
    if (currentUser.value.role === ROLES.ADMIN) return true
    return roles.some((r) => r.toUpperCase() === currentUser.value.role?.toUpperCase())
  }

  function can(permission) {
    if (userRole.value === ROLES.ADMIN) return true
    return permissions.value.includes(permission)
  }

  function canAccessRoute(requiredRoles) {
    return checkRouteAccess(userRole.value, requiredRoles)
  }

  function restoreUser() {
    try {
      const stored = localStorage.getItem('user')
      return stored ? JSON.parse(stored) : null
    } catch (_) {
      return null
    }
  }

  function persistSession(data) {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)

    const user = {
      id: data.userId,
      email: data.email,
      firstName: data.firstName,
      lastName: data.lastName,
      role: data.role,
    }
    currentUser.value = user
    localStorage.setItem('user', JSON.stringify(user))
  }

  function tryRestoreSession() {
    const token = localStorage.getItem('refreshToken')
    if (!token) return Promise.resolve(false)

    return refreshAccessToken(token)
      .then((data) => {
        persistSession(data)
        return true
      })
      .catch(() => {
        clearSession()
        return false
      })
  }

  async function login(credentials) {
    loading.value = true
    error.value = null
    try {
      const data = await loginApi(credentials)
      persistSession(data)
      return data
    } catch (e) {
      error.value = e.response?.data?.error || 'Login failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      await logoutRequest()
    } catch (_) {
    } finally {
      clearSession()
    }
  }

  function clearSession() {
    currentUser.value = null
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }

  function clearError() {
    error.value = null
  }

  return {
    currentUser,
    accessToken,
    refreshToken,
    isAuthenticated,
    userRole,
    permissions,
    isAdmin,
    loading,
    error,
    hasRole,
    can,
    canAccessRoute,
    login,
    logout,
    tryRestoreSession,
    clearError,
  }
})
