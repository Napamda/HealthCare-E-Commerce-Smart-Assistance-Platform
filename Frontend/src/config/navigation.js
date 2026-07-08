import { ROLES } from './permissions.js'

export const NAV_ITEMS = {
  [ROLES.PATIENT]: [
    { to: '/chat', label: 'AI Chat' },
    { to: '/consultations', label: 'Consultations' },
    { to: '/prescriptions', label: 'My Prescriptions' },
    { to: '/prescriptions/upload', label: 'Upload Rx' },
  ],

  [ROLES.DOCTOR]: [
    { to: '/doctor', label: 'Dashboard' },
    { to: '/consultations', label: 'Consultations' },
    { to: '/prescriptions', label: 'Prescriptions' },
    { to: '/chat', label: 'AI Chat' },
  ],

  [ROLES.PHARMACIST]: [
    { to: '/pharmacist', label: 'Dashboard' },
    { to: '/prescriptions', label: 'Prescriptions' },
    { to: '/consultations', label: 'Consultations' },
    { to: '/chat', label: 'AI Chat' },
  ],

  [ROLES.ADMIN]: [
    { to: '/admin', label: 'Dashboard' },
    {
      label: 'Role Views',
      children: [
        { to: '/doctor', label: 'Doctor View' },
        { to: '/pharmacist', label: 'Pharmacist View' },
        { to: '/vendor', label: 'Vendor View' },
      ],
    },
    { to: '/consultations', label: 'Consultations' },
    { to: '/prescriptions', label: 'Prescriptions' },
    { to: '/chat', label: 'AI Chat' },
  ],

  [ROLES.VENDOR]: [
    { to: '/vendor', label: 'Dashboard' },
    { to: '/chat', label: 'AI Chat' },
    { to: '/consultations', label: 'Consultations' },
  ],
}

export function getNavItems(role) {
  return NAV_ITEMS[role] || []
}
