import apiClient from './api.js'

/**
 * Fetch full stock status for every product.
 * @returns {Promise<Array>} StockInfoResponse[]
 */
export function getStockList() {
  return apiClient.get('/api/inventory/stock').then((res) => res.data)
}

/**
 * Fetch stock status for a single product.
 * @param {number} productId
 * @returns {Promise<object>} StockInfoResponse
 */
export function getProductStock(productId) {
  return apiClient.get(`/api/inventory/stock/${productId}`).then((res) => res.data)
}

/**
 * Fetch products whose stock has reached (or fallen below) their low-stock threshold.
 * @returns {Promise<Array>} StockInfoResponse[]
 */
export function getLowStockProducts() {
  return apiClient.get('/api/inventory/low-stock').then((res) => res.data)
}

/**
 * Fetch the stock history log for a product (newest first).
 * @param {number} productId
 * @returns {Promise<Array>} StockHistoryDto[]
 */
export function getStockHistory(productId) {
  return apiClient.get(`/api/inventory/history/${productId}`).then((res) => res.data)
}

/**
 * Fetch current stock reservations.
 * @returns {Promise<Array>} StockReservationDto[]
 */
export function getReservations() {
  return apiClient.get('/api/inventory/reservations').then((res) => res.data)
}

/**
 * Fetch recent stock movements across all products.
 * @returns {Promise<Array>} StockHistoryDto[]
 */
export function getRecentActivity() {
  return apiClient.get('/api/inventory/activity').then((res) => res.data)
}
/**
 * Validate stock availability for a list of items before checkout.
 * @param {Array<{productId: number, quantity: number}>} items
 * @returns {Promise<{valid: boolean, errors: Array}>} StockValidateResponse
 */
export function validateStock(items) {
  return apiClient.post('/api/inventory/validate', { items }).then((res) => res.data)
}

function postAdjust(productId, action, quantity, note) {
  return apiClient
    .post(`/api/inventory/${productId}/${action}`, { quantity, note })
    .then((res) => res.data)
}

/**
 * Add stock to a product. Returns the updated StockInfoResponse.
 * @param {number} productId
 * @param {number} quantity  Positive units to add
 * @param {string} [note]    Reason / reference note
 */
export function incrementStock(productId, quantity, note) {
  return postAdjust(productId, 'increment', quantity, note)
}

/**
 * Remove stock from a product. Returns the updated StockInfoResponse.
 * @param {number} productId
 * @param {number} quantity  Positive units to remove (cannot exceed current stock)
 * @param {string} [note]
 */
export function decrementStock(productId, quantity, note) {
  return postAdjust(productId, 'decrement', quantity, note)
}

/**
 * Set stock to an absolute level. Returns the updated StockInfoResponse.
 * @param {number} productId
 * @param {number} quantity  New absolute stock level (>= 0)
 * @param {string} [note]
 */
export function adjustStock(productId, quantity, note) {
  return postAdjust(productId, 'adjust', quantity, note)
}

/**
 * Update the low-stock warning threshold for a product.
 * @param {number} productId
 * @param {number} threshold
 * @returns {Promise<object>} StockInfoResponse
 */
export function setThreshold(productId, threshold) {
  return apiClient
    .put(`/api/inventory/${productId}/threshold`, { threshold })
    .then((res) => res.data)
}
