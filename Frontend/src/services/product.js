import apiClient from './api.js'

/**
 * List products with pagination and optional sorting.
 * @param {number} [page=0]
 * @param {number} [size=12]
 * @param {string} [sort] One of price_asc, price_desc, name_asc, name_desc, newest, popular, stock_asc
 * @returns {Promise<object>} Paginated product response
 */
export function listProducts(page = 0, size = 12, sort) {
  return apiClient
    .get('/api/products', { params: { page, size, sort } })
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
 * @param {string} [params.sort] One of price_asc, price_desc, name_asc, name_desc, newest, popular, stock_asc
 * @returns {Promise<object>} Paginated search result
 */
export function searchProducts({ keyword, category, minPrice, maxPrice, page = 0, size = 12, sort } = {}) {
  return apiClient
    .get('/api/products/search', {
      params: { keyword, category, minPrice, maxPrice, page, size, sort },
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

/* ============ Product Images ============ */

/**
 * List all images for a product (public).
 * @param {number} productId
 * @returns {Promise<Array<{id:number,url:string,thumbnailUrl:string,primary:boolean,sortOrder:number}>>}
 */
export function getProductImages(productId) {
  return apiClient.get(`/api/products/${productId}/images`).then((res) => res.data)
}

/**
 * Upload an image for a product (admin). Generates a 240px thumbnail server-side.
 * @param {number} productId
 * @param {File} file
 * @returns {Promise<object>} ImageResponse
 */
export function uploadProductImage(productId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient
    .post(`/api/admin/products/${productId}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data)
}

/**
 * Set an image as the product's primary image (admin).
 * @param {number} imageId
 * @returns {Promise<object>} ImageResponse
 */
export function setPrimaryImage(imageId) {
  return apiClient
    .put(`/api/admin/products/images/${imageId}/primary`)
    .then((res) => res.data)
}

/**
 * Delete an image (admin).
 * @param {number} imageId
 * @returns {Promise<void>}
 */
export function deleteProductImage(imageId) {
  return apiClient.delete(`/api/admin/products/images/${imageId}`)
}
