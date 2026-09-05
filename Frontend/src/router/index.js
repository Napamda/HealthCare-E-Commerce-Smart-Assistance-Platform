import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { ROLES, ROLE_DASHBOARD } from '../config/permissions.js'

const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../pages/auth/LoginPage.vue'),
    meta: { public: true },
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../pages/chat/ChatPage.vue'),
  },
  {
    path: '/chat/:id',
    name: 'ChatConversation',
    component: () => import('../pages/chat/ChatPage.vue'),
  },
  {
    path: '/consultations',
    name: 'Consultations',
    meta: { requiresRole: [ROLES.PATIENT] },
    component: () => import('../pages/doctor/ConsultationStatusPage.vue'),
  },
  {
    path: '/recommendations',
    name: 'Recommendations',
    component: () => import('../pages/products/RecommendationsPage.vue'),
  },
  {
    path: '/professionals',
    name: 'Professionals',
    component: () => import('../pages/directory/ProfessionalDirectoryPage.vue'),
  },
  {
    path: '/events',
    name: 'Events',
    component: () => import('../pages/events/EventListPage.vue'),
  },
  {
    path: '/events/manage',
    name: 'Event Management',
    meta: { requiresRole: [ROLES.DOCTOR, ROLES.ADMIN] },
    component: () => import('../pages/events/EventManagementPage.vue'),
  },
  {
    path: '/events/registrations',
    name: 'MyEventRegistrations',
    meta: { requiresRole: [ROLES.PATIENT, ROLES.DOCTOR, ROLES.PHARMACIST, ROLES.VENDOR, ROLES.ADMIN] },
    component: () => import('../pages/events/MyEventRegistrationsPage.vue'),
  },
  {
    path: '/events/:id',
    name: 'EventDetail',
    component: () => import('../pages/events/EventDetailPage.vue'),
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('../pages/products/ProductCatalogPage.vue'),
  },
  {
    path: '/products/:id',
    name: 'ProductDetail',
    component: () => import('../pages/products/ProductDetailPage.vue'),
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../pages/cart/CartPage.vue'),
  },
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('../pages/cart/CheckoutPage.vue'),
  },
  {
    path: '/payment/:orderId',
    name: 'Payment',
    component: () => import('../pages/PaymentPage.vue'),
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../pages/orders/OrderHistoryPage.vue'),
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('../pages/orders/OrderDetailPage.vue'),
  },
  {
    path: '/admin/products',
    name: 'AdminProducts',
    meta: { requiresRole: [ROLES.ADMIN] },
    component: () => import('../pages/products/AdminProductPage.vue'),
  },
  {
    path: '/admin/categories',
    name: 'AdminCategories',
    meta: { requiresRole: [ROLES.ADMIN] },
    component: () => import('../pages/AdminCategoryPage.vue'),
  },
  {
    path: '/inventory',
    name: 'InventoryManagement',
    meta: { requiresRole: [ROLES.VENDOR, ROLES.ADMIN] },
    component: () => import('../pages/inventory/InventoryManagementPage.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../pages/auth/RegisterPage.vue'),
    meta: { public: true },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../pages/ProfilePage.vue'),
  },
  {
    path: '/verify-email',
    name: 'VerifyEmail',
    component: () => import('../pages/auth/VerifyEmailPage.vue'),
    meta: { public: true },
  },

  {
    path: '/admin',
    name: 'AdminDashboard',
    meta: { requiresRole: [ROLES.ADMIN] },
    component: () => import('../pages/admin/AdminDashboardPage.vue'),
  },
  {
    path: '/doctor',
    name: 'DoctorDashboard',
    meta: { requiresRole: [ROLES.DOCTOR, ROLES.ADMIN] },
    component: () => import('../pages/doctor/DoctorDashboardPage.vue'),
  },
  {
    path: '/doctor/chat',
    name: 'DoctorChatList',
    meta: { requiresRole: [ROLES.DOCTOR, ROLES.ADMIN] },
    component: () => import('../pages/doctor/DoctorChatPage.vue'),
  },
  {
    path: '/doctor/chat/:conversationId',
    name: 'DoctorChatConversation',
    meta: { requiresRole: [ROLES.DOCTOR, ROLES.ADMIN] },
    component: () => import('../pages/doctor/DoctorChatPage.vue'),
  },
  {
    path: '/prescriptions',
    name: 'PrescriptionList',
    meta: { requiresRole: [ROLES.PATIENT] },
    component: () => import('../pages/prescriptions/PrescriptionListPage.vue'),
  },
  {
    path: '/prescriptions/upload',
    name: 'PrescriptionUpload',
    meta: { requiresRole: [ROLES.PATIENT] },
    component: () => import('../pages/prescriptions/PrescriptionUploadPage.vue'),
  },
  {
    path: '/prescriptions/:id',
    name: 'PrescriptionDetail',
    meta: { requiresRole: [ROLES.PATIENT] },
    component: () => import('../pages/prescriptions/PrescriptionDetailPage.vue'),
  },
  {
    path: '/pharmacist',
    name: 'PharmacistDashboard',
    meta: { requiresRole: [ROLES.PHARMACIST, ROLES.ADMIN] },
    component: () => import('../pages/pharmacist/PharmacistDashboardPage.vue'),
  },
  {
    path: '/vendor',
    name: 'VendorDashboard',
    meta: { requiresRole: [ROLES.VENDOR, ROLES.ADMIN] },
    component: () => import('../pages/vendor/VendorDashboardPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

let sessionRestoreAttempted = false

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  // On the very first navigation after a page load, use the refresh token to
  // settle who the real authenticated user is. The synchronously restored
  // localStorage snapshot is only a hint — the backend refresh response is
  // authoritative and prevents the patient→pharmacist user swap when stale
  // localStorage state is trusted for role checks.
  if (!sessionRestoreAttempted) {
    sessionRestoreAttempted = true
    await authStore.tryRestoreSession()
  }

  if (!to.meta.public && !authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.matched.length === 0) {
    // Role-based default landing — avoid bouncing everyone to Chat on 404.
    const fallback = authStore.isAuthenticated
      ? (authStore.userRole && ROLE_DASHBOARD
          ? (ROLE_DASHBOARD[authStore.userRole] || '/chat')
          : '/chat')
      : { name: 'Login' }
    next(fallback)
    return
  }

  const requiredRoles = to.meta.requiresRole

  if (!requiredRoles || requiredRoles.length === 0) {
    next()
    return
  }

  if (!authStore.canAccessRoute(requiredRoles)) {
    // Role mismatch — send them to their own role dashboard instead of Chat.
    // This prevents a patient landing on a pharmacist-restricted route (or
    // vice versa) from being silently redirected to a page whose UI then
    // shows the "other" account.
    const dashboard = authStore.isAuthenticated
      ? (ROLE_DASHBOARD[authStore.userRole] || '/chat')
      : { name: 'Login', query: { redirect: to.fullPath } }
    next(dashboard)
    return
  }

  next()
})

export default router
