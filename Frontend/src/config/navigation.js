import { ROLES } from './permissions.js'

export const NAV_ITEMS = {
  [ROLES.PATIENT]: [
    { to: '/chat', label: 'AI Chat' },
    {
      label: 'Products',
      children: [
        { to: '/products', label: 'Browse Products' },
      ],
    },
    {
      label: 'Health',
      children: [
        { to: '/consultations', label: 'My Consultations' },
        { to: '/prescriptions', label: 'My Prescriptions' },
        { to: '/prescriptions/upload', label: 'Upload Prescription' },
      ],
    },
  ],

  [ROLES.DOCTOR]: [
    { to: '/doctor', label: 'Dashboard' },
    {
      label: 'Consultations',
      children: [
        { to: '/consultations', label: 'All Consultations' },
      ],
    },
    {
      label: 'Prescriptions',
      children: [
        { to: '/prescriptions', label: 'All Prescriptions' },
      ],
    },
    {
      label: 'Products',
      children: [
        { to: '/products', label: 'Browse Products' },
      ],
    },
    { to: '/chat', label: 'AI Chat' },
  ],

  [ROLES.PHARMACIST]: [
    { to: '/pharmacist', label: 'Dashboard' },
    {
      label: 'Prescriptions',
      children: [
        { to: '/prescriptions', label: 'All Prescriptions' },
      ],
    },
    {
      label: 'Products',
      children: [
        { to: '/products', label: 'Browse Products' },
      ],
    },
    { to: '/chat', label: 'AI Chat' },
    { to: '/consultations', label: 'Consultations' },
  ],

  [ROLES.ADMIN]: [
    { to: '/admin', label: 'Dashboard' },
    {
      label: 'Products',
      children: [
        { to: '/products', label: 'Browse Products' },
        { to: '/admin/products', label: 'Manage Products' },
      ],
    },
    {
      label: 'Consultations',
      children: [
        { to: '/consultations', label: 'All Consultations' },
      ],
    },
    {
      label: 'Prescriptions',
      children: [
        { to: '/prescriptions', label: 'All Prescriptions' },
      ],
    },
    {
      label: 'Role Views',
      children: [
        { to: '/doctor', label: 'Doctor View' },
        { to: '/pharmacist', label: 'Pharmacist View' },
        { to: '/vendor', label: 'Vendor View' },
      ],
    },
    { to: '/chat', label: 'AI Chat' },
  ],

  [ROLES.VENDOR]: [
    { to: '/vendor', label: 'Dashboard' },
    {
      label: 'Products',
      children: [
        { to: '/products', label: 'Browse Products' },
      ],
    },
    { to: '/chat', label: 'AI Chat' },
    { to: '/consultations', label: 'Consultations' },
  ],
}

export function getNavItems(role) {
  return NAV_ITEMS[role] || []
}
