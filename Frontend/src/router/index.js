import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/prescriptions',
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
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
