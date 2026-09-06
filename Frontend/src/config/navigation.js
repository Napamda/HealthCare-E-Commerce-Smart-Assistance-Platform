import { ROLES } from './permissions.js'

// Single source of truth for top-nav items, per role.
// ChatSidebar (and anything else) should import getNavItems()
// instead of hardcoding its own router.push() targets — that's
// what let /products and /admin/products go missing before.
export const NAV_ITEMS = {
  [ROLES.PATIENT]: [
    { to: '/chat', label: 'AI Chat' },
    {
      label: 'Shop',
      children: [
        { to: '/products', label: 'Products' },
        { to: '/recommendations', label: 'AI Recommendations' },
        { to: '/cart', label: 'Cart' },
        { to: '/orders', label: 'Orders' },
      ],
    },
    {
      label: 'Care',
      children: [
        { to: '/consultations', label: 'Consultations' },
        { to: '/prescriptions', label: 'Prescriptions' },
      ],
    },
    {
      label: 'Events',
      children: [
        { to: '/events', label: 'Browse Events' },
        { to: '/events/registrations', label: 'My Events' },
      ],
    },
  ],

  [ROLES.DOCTOR]: [
    { to: '/doctor', label: 'Dashboard' },
    { to: '/events', label: 'Events' },
    { to: '/events/manage', label: 'Manage Events' },
    { to: '/events/registrations', label: 'My Events' },
    { to: '/chat', label: 'AI Chat' },
  ],

  [ROLES.PHARMACIST]: [
    { to: '/pharmacist', label: 'Dashboard' },
    { to: '/events', label: 'Events' },
    { to: '/events/registrations', label: 'My Events' },
  ],

  [ROLES.ADMIN]: [
    { to: '/admin', label: 'Dashboard' },
    { to: '/admin/users', label: 'Manage Users' },
    { to: '/admin/products', label: 'Manage Products' },
    { to: '/admin/categories', label: 'Manage Categories' },
    { to: '/inventory', label: 'Manage Inventory' },
    { to: '/events/manage', label: 'Manage Events' },
    {
      label: 'Role Views',
      children: [
        { to: '/doctor', label: 'Doctor View' },
        { to: '/pharmacist', label: 'Pharmacist View' },
        { to: '/vendor', label: 'Vendor View' },
      ],
    },
  ],

  [ROLES.VENDOR]: [
    { to: '/vendor', label: 'Dashboard' },
    { to: '/inventory', label: 'Inventory' },
    { to: '/products', label: 'Shop' },
    { to: '/events', label: 'Events' },
    { to: '/events/registrations', label: 'My Events' },
    { to: '/chat', label: 'AI Chat' },
  ],
}

// Single source of truth for the "My Profile" destination per role.
// Every role has its own dedicated profile page.
export const ROLE_PROFILE = {
  [ROLES.PATIENT]: '/patient/profile',
  [ROLES.DOCTOR]: '/doctor/profile',
  [ROLES.PHARMACIST]: '/pharmacist/profile',
  [ROLES.VENDOR]: '/vendor/profile',
  [ROLES.ADMIN]: '/admin/profile',
}

export function getNavItems(role) {
  return NAV_ITEMS[role] || []
}

// Flat list of every path referenced above, so a route guard or a
// test can assert "every route with a page either appears here or
// is deliberately excluded" instead of that drifting silently again.
export function getAllNavPaths() {
  return Object.values(NAV_ITEMS)
    .flat()
    .flatMap((item) => (item.children ? item.children : [item]))
    .map((item) => item.to)
}
