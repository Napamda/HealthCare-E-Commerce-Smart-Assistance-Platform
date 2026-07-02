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
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
