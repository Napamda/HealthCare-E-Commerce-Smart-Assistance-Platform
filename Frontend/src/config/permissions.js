export const ROLES = {
  PATIENT: 'PATIENT',
  DOCTOR: 'DOCTOR',
  PHARMACIST: 'PHARMACIST',
  ADMIN: 'ADMIN',
  VENDOR: 'VENDOR',
}

export const ROLE_LABELS = {
  [ROLES.PATIENT]: 'Patient',
  [ROLES.DOCTOR]: 'Doctor',
  [ROLES.PHARMACIST]: 'Pharmacist',
  [ROLES.ADMIN]: 'Administrator',
  [ROLES.VENDOR]: 'Vendor',
}

export const ROLE_DASHBOARD = {
  [ROLES.ADMIN]: '/admin',
  [ROLES.DOCTOR]: '/doctor',
  [ROLES.PHARMACIST]: '/pharmacist',
  [ROLES.VENDOR]: '/vendor',
  [ROLES.PATIENT]: '/products',
}

export const ROLE_PERMISSIONS = {
  [ROLES.ADMIN]: [
    'manage_users',
    'view_analytics',
    'manage_system',
    'view_all_records',
    'manage_prescriptions',
    'review_prescriptions',
    'manage_consultations',
    'manage_inventory',
    'vendor_operations',
  ],
  [ROLES.DOCTOR]: [
    'manage_prescriptions',
    'manage_consultations',
    'view_patient_records',
    'create_diagnosis',
  ],
  [ROLES.PHARMACIST]: [
    'review_prescriptions',
    'dispense_medication',
    'view_prescription_history',
  ],
  [ROLES.VENDOR]: [
    'manage_inventory',
    'process_orders',
    'vendor_operations',
  ],
  [ROLES.PATIENT]: [
    'view_own_records',
    'create_consultation',
    'chat_with_ai',
    'view_own_prescriptions',
  ],
}

export function getPermissionsForRole(role) {
  return ROLE_PERMISSIONS[role] || []
}

export function hasPermission(userRole, permission) {
  if (userRole === ROLES.ADMIN) return true
  const permissions = ROLE_PERMISSIONS[userRole] || []
  return permissions.includes(permission)
}

export function canAccessRoute(userRole, requiredRoles) {
  if (!requiredRoles || requiredRoles.length === 0) return true
  if (userRole === ROLES.ADMIN) return true
  return requiredRoles.some((r) => r.toUpperCase() === userRole?.toUpperCase())
}
