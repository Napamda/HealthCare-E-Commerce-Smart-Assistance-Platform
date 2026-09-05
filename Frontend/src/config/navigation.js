import { ROLES } from './permissions.js'

// Single source of truth for top-nav items, per role.
// ChatSidebar (and anything else) should import getNavItems()
// instead of hardcoding its own router.push() targets — that's
// what let /products and /admin/products go missing before.
export const NAV_ITEMS = {
  [ROLES.PATIENT]: [
    { to: '/chat', label: 'AI Chat' },
    { to: '/products', label: 'Shop' },
    { to: '/events', label: 'Events' },
    { to: '/events/registrations', label: 'My Events' },
    { to: '/consultations', label: 'Consultations' },
    { to: '/prescriptions', label: 'Prescriptions' },
  ],

  [ROLES.DOCTOR]: [
    { to: '/doctor', label: 'Dashboard' },
    { to: '/consultations', label: 'Consultations' },
    { to: '/events', label: 'Events' },
    { to: '/events/manage', label: 'Manage Events' },
    { to: '/events/registrations', label: 'My Events' },
    { to: '/prescriptions', label: 'Prescriptions' },
    { to: '/chat', label: 'AI Chat' },
  ],

  [ROLES.PHARMACIST]: [
    { to: '/pharmacist', label: 'Dashboard' },
    { to: '/prescriptions', label: 'Prescriptions' },
    { to: '/events', label: 'Events' },
    { to: '/events/registrations', label: 'My Events' },
    { to: '/consultations', label: 'Consultations' },
  ],

  [ROLES.ADMIN]: [
    { to: '/admin', label: 'Dashboard' },
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
    { to: '/consultations', label: 'Consultations' },
    { to: '/prescriptions', label: 'Prescriptions' },
  ],

  [ROLES.VENDOR]: [
    { to: '/vendor', label: 'Dashboard' },
    { to: '/inventory', label: 'Inventory' },
    { to: '/products', label: 'Shop' },
    { to: '/events', label: 'Events' },
    { to: '/events/registrations', label: 'My Events' },
    { to: '/chat', label: 'AI Chat' },
    { to: '/consultations', label: 'Consultations' },
  ],
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
