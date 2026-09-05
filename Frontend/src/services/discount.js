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
 * Validate a discount code against a given subtotal.
 * @param {string} code
 * @param {number} subtotal
 * @returns {Promise<{valid: boolean, code: string, discountAmount: number, discountedSubtotal: number}>}
 */
export function validateDiscount(code, subtotal) {
  return api.get('/api/discounts/validate', {
    params: { code, subtotal },
  }).then((res) => res.data)
}