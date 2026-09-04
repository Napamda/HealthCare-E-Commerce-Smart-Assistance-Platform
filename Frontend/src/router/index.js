import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { ROLES } from '../config/permissions.js'

const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../pages/LoginPage.vue'),
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../pages/ChatPage.vue'),
  },
  {
    path: '/chat/:id',
    name: 'ChatConversation',
    component: () => import('../pages/ChatPage.vue'),
  },
  {
    path: '/consultations',
    name: 'Consultations',
    component: () => import('../pages/ConsultationStatusPage.vue'),
  },
  {
    path: '/professionals',
    name: 'Professionals',
    component: () => import('../pages/ProfessionalDirectoryPage.vue'),
  },
  {
    path: '/events',
    name: 'Events',
    component: () => import('../pages/EventListPage.vue'),
  },
  {
    path: '/events/manage',
    name: 'Event Management',
    meta: { requiresRole: [ROLES.DOCTOR, ROLES.ADMIN] },
    component: () => import('../pages/EventManagementPage.vue'),
  },
  {
    path: '/events/registrations',
    name: 'MyEventRegistrations',
    meta: { requiresRole: [ROLES.PATIENT, ROLES.DOCTOR, ROLES.PHARMACIST, ROLES.VENDOR, ROLES.ADMIN] },
    component: () => import('../pages/MyEventRegistrationsPage.vue'),
  },
  {
    path: '/events/:id',
    name: 'EventDetail',
    component: () => import('../pages/EventDetailPage.vue'),
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('../pages/ProductCatalogPage.vue'),
  },
  {
    path: '/products/:id',
    name: 'ProductDetail',
    component: () => import('../pages/ProductDetailPage.vue'),
  },
  {
    path: '/recommendations',
    name: 'Recommendations',
    component: () => import('../pages/RecommendationsPage.vue'),
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../pages/CartPage.vue'),
  },
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('../pages/CheckoutPage.vue'),
  },
  {
    path: '/payment/:orderId',
    name: 'Payment',
    component: () => import('../pages/PaymentPage.vue'),
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../pages/OrderHistoryPage.vue'),
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('../pages/OrderDetailPage.vue'),
  },
  {
    path: '/admin/products',
    name: 'AdminProducts',
    meta: { requiresRole: [ROLES.ADMIN] },
    component: () => import('../pages/AdminProductPage.vue'),
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
    component: () => import('../pages/InventoryManagementPage.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../pages/RegisterPage.vue'),
  },
  
  {
    path: '/admin',
    name: 'AdminDashboard',
    meta: { requiresRole: [ROLES.ADMIN] },
    component: () => import('../pages/AdminDashboardPage.vue'),
  },
  {
    path: '/doctor',
    name: 'DoctorDashboard',
    meta: { requiresRole: [ROLES.DOCTOR, ROLES.ADMIN] },
    component: () => import('../pages/DoctorDashboardPage.vue'),
  },
  {
    path: '/prescriptions',
    name: 'PrescriptionList',
    component: () => import('../pages/PrescriptionListPage.vue'),
  },
  {
    path: '/prescriptions/upload',
    name: 'PrescriptionUpload',
    component: () => import('../pages/PrescriptionUploadPage.vue'),
  },
  {
    path: '/prescriptions/:id',
    name: 'PrescriptionDetail',
    component: () => import('../pages/PrescriptionDetailPage.vue'),
  },
  {
    path: '/pharmacist',
    name: 'PharmacistDashboard',
    meta: { requiresRole: [ROLES.PHARMACIST, ROLES.ADMIN] },
    component: () => import('../pages/PharmacistDashboardPage.vue'),
  },
  {
    path: '/vendor',
    name: 'VendorDashboard',
    meta: { requiresRole: [ROLES.VENDOR, ROLES.ADMIN] },
    component: () => import('../pages/VendorDashboardPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

let sessionRestoreAttempted = false

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  if (!sessionRestoreAttempted && !authStore.isAuthenticated) {
    sessionRestoreAttempted = true
    await authStore.tryRestoreSession()
  }

  const requiredRoles = to.meta.requiresRole

  if (!requiredRoles || requiredRoles.length === 0) {
    next()
    return
  }

  if (!authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (!authStore.canAccessRoute(requiredRoles)) {
    next({ name: 'Chat' })
    return
  }

  next()
})

export default router
