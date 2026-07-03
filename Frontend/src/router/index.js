import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/prescriptions/upload',
  },
  {
    path: '/prescriptions/upload',
    name: 'PrescriptionUpload',
    component: () => import('../pages/PrescriptionUploadPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
