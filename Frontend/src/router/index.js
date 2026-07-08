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
    component: () => import('../pages/PharmacistDashboardPage.vue'),
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
