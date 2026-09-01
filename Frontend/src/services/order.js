import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export function createOrder(orderData) {
  return api.post('/api/orders', orderData).then((res) => res.data)
}

export function getUserOrders(page = 0, size = 10) {
  return api.get('/api/orders', { params: { page, size } }).then((res) => res.data)
}

export function getOrderById(orderId) {
  return api.get(`/api/orders/${orderId}`).then((res) => res.data)
}

export function cancelOrder(orderId) {
  return api.post(`/api/orders/${orderId}/cancel`).then((res) => res.data)
}
