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
 * Valid vendor status transitions.
 * Vendors CANNOT move an order from SHIPPED to DELIVERED.
 */
export const VALID_VENDOR_TRANSITIONS = {
  PENDING:    ['CONFIRMED', 'CANCELLED'],
  CONFIRMED:  ['PROCESSING', 'CANCELLED'],
  PROCESSING: ['SHIPPED', 'CANCELLED'],
  SHIPPED:    [],                 // ← locked; customer must confirm delivery
  DELIVERED:  [],
  CANCELLED:  [],
}

/**
 * Frontend verification helper for vendor status changes.
 * Use this in your vendor order-management UI to disable/hide invalid options.
 * @param {string} currentStatus
 * @param {string} nextStatus
 * @returns {boolean}
 */
export function isValidVendorTransition(currentStatus, nextStatus) {
  const allowed = VALID_VENDOR_TRANSITIONS[currentStatus] || []
  return allowed.includes(nextStatus)
}

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
 * Update order status.  
 * DELIVERED should never be sent from the vendor side for a SHIPPED order.
 * @param {number} orderId
 * @param {string} status
 * @returns {Promise<import('../types').OrderResponse>}
 */
export function updateOrderStatus(orderId, status) {
  return api.put(`/api/vendor/orders/${orderId}/status`, { status }).then((res) => res.data)
}

/**
 * Ship an order.
 * A tracking number is auto-generated and assigned by the server when one is not provided,
 * so every shipped order is guaranteed to have a tracking number.
 * @param {number} orderId
 * @param {string} [trackingNumber]
 * @returns {Promise<import('../types').OrderResponse>}
 */
export function shipOrder(orderId, trackingNumber) {
  return api.put(`/api/vendor/orders/${orderId}/ship`, { trackingNumber }).then((res) => res.data)
}

/**
 * Get the vendor's own business profile (application + approval status).
 * @returns {Promise<import('../types').VendorProfile>}
 */
export function getVendorProfile() {
  return api.get('/api/vendor/profile').then((res) => res.data)
}

/**
 * Update the vendor's own business profile.
 * @param {{ businessName: string, businessLicense?: string | null }} data
 * @returns {Promise<import('../types').VendorProfile>}
 */
export function updateVendorProfile(data) {
  return api.put('/api/vendor/profile', data).then((res) => res.data)
}