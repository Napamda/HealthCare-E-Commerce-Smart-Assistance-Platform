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

/**
 * Get vendor dashboard statistics.
 * @returns {Promise<import('../types').VendorDashboardStats>}
 */
export function getDashboardStats() {
  return api.get('/api/vendor/dashboard/stats').then((res) => res.data)
}

/**
 * Get paginated orders with optional status filter.
 * @param {{ page?: number, size?: number, status?: string }} params
 * @returns {Promise<import('../types').PaginatedResponse>}
 */
export function getVendorOrders({ page = 0, size = 15, status } = {}) {
  const params = { page, size }
  if (status) params.status = status
  return api.get('/api/vendor/orders', { params }).then((res) => res.data)
}

/**
 * Update order status (CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED).
 * @param {number} orderId
 * @param {string} status
 * @returns {Promise<import('../types').OrderResponse>}
 */
export function updateOrderStatus(orderId, status) {
  return api.put(`/api/vendor/orders/${orderId}/status`, { status }).then((res) => res.data)
}

/**
 * Ship an order with an optional tracking number.
 * @param {number} orderId
 * @param {string} [trackingNumber]
 * @returns {Promise<import('../types').OrderResponse>}
 */
export function shipOrder(orderId, trackingNumber) {
  return api.put(`/api/vendor/orders/${orderId}/ship`, { trackingNumber }).then((res) => res.data)
}