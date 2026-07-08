import { computed } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { ROLES, hasPermission, canAccessRoute } from '../config/permissions.js'

export function useAuthorization() {
  const authStore = useAuthStore()

  const isAdmin = computed(() => authStore.userRole === ROLES.ADMIN)
  const isDoctor = computed(() => authStore.userRole === ROLES.DOCTOR || authStore.userRole === ROLES.ADMIN)
  const isPharmacist = computed(() => authStore.userRole === ROLES.PHARMACIST || authStore.userRole === ROLES.ADMIN)
  const isVendor = computed(() => authStore.userRole === ROLES.VENDOR || authStore.userRole === ROLES.ADMIN)
  const isPatient = computed(() => authStore.userRole === ROLES.PATIENT || authStore.userRole === ROLES.ADMIN)

  function can(permission) {
    return hasPermission(authStore.userRole, permission)
  }

  function canAccess(...requiredRoles) {
    return canAccessRoute(authStore.userRole, requiredRoles)
  }

  function canAccessAny(...roles) {
    return authStore.hasRole(...roles) || authStore.userRole === ROLES.ADMIN
  }

  return {
    isAdmin,
    isDoctor,
    isPharmacist,
    isVendor,
    isPatient,
    can,
    canAccess,
    canAccessAny,
    ROLES,
  }
}
