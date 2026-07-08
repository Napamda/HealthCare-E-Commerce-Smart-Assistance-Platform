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
    path: '/admin/products',
    name: 'AdminProducts',
    component: () => import('../pages/AdminProductPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
