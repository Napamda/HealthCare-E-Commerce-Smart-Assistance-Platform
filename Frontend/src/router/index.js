import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/chat',
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
    component: () => import('../pages/PharmacistDashboardPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
