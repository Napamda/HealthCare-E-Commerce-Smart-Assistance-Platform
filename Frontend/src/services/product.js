import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

/**
 * List products with pagination.
 * @param {number} [page=0]
 * @param {number} [size=12]
 * @returns {Promise<object>} Paginated product response
 */
export function listProducts(page = 0, size = 12) {
  return apiClient
    .get('/api/products', { params: { page, size } })
    .then((res) => res.data)
}

/**
 * Search products by keyword, category, and price range.
 * @param {object} params
 * @param {string} [params.keyword]
 * @param {string} [params.category]
 * @param {number} [params.minPrice]
 * @param {number} [params.maxPrice]
 * @param {number} [params.page=0]
 * @param {number} [params.size=12]
 * @returns {Promise<object>} Paginated search result
 */
export function searchProducts({ keyword, category, minPrice, maxPrice, page = 0, size = 12 } = {}) {
  return apiClient
    .get('/api/products/search', {
      params: { keyword, category, minPrice, maxPrice, page, size },
    })
    .then((res) => res.data)
}

/**
 * Get a single product by ID.
 * @param {number} id
 * @returns {Promise<object>} ProductResponse
 */
export function getProduct(id) {
  return apiClient.get(`/api/products/${id}`).then((res) => res.data)
}

/**
 * Get all product categories.
 * @returns {Promise<string[]>} Category names
 */
export function getCategories() {
  return apiClient.get('/api/products/categories').then((res) => res.data)
}

/**
 * Get all product categories with product counts.
 * @returns {Promise<Array<{name: string, count: number}>>}
 */
export function getCategoriesWithCounts() {
  return apiClient.get('/api/products/categories/with-counts').then((res) => res.data)
}

/**
 * Create a new product (admin).
 * @param {object} productData
 * @returns {Promise<object>} ProductResponse
 */
export function createProduct(productData) {
  return apiClient.post('/api/admin/products', productData).then((res) => res.data)
}

/**
 * Update an existing product (admin).
 * @param {number} id
 * @param {object} productData
 * @returns {Promise<object>} ProductResponse
 */
export function updateProduct(id, productData) {
  return apiClient.put(`/api/admin/products/${id}`, productData).then((res) => res.data)
}

/**
 * Delete a product (admin).
 * @param {number} id
 * @returns {Promise<void>}
 */
export function deleteProduct(id) {
  return apiClient.delete(`/api/admin/products/${id}`)
}
