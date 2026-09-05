import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// GET /api/categories — tree of active categories (with productCount, children)
export function getCategoryTree() {
  return apiClient.get('/api/categories').then((r) => r.data)
}

// GET /api/categories/all — flat list including inactive categories
export function getAllCategories() {
  return apiClient.get('/api/categories/all').then((r) => r.data)
}

// GET /api/categories/{id}
export function getCategory(id) {
  return apiClient.get(`/api/categories/${id}`).then((r) => r.data)
}

// POST /api/categories
export function createCategory(data) {
  return apiClient.post('/api/categories', data).then((r) => r.data)
}

// PUT /api/categories/{id}
export function updateCategory(id, data) {
  return apiClient.put(`/api/categories/${id}`, data).then((r) => r.data)
}

// DELETE /api/categories/{id}
export function deleteCategory(id) {
  return apiClient.delete(`/api/categories/${id}`).then((r) => r.data)
}

/**
 * Maps a category display name to the product-search `category` value.
 * Mirrors the backend normalisation (uppercase, non-alphanumeric -> "_").
 * e.g. "Pain Relief" -> "PAIN_RELIEF", "Vitamins" -> "VITAMINS"
 */
export function categoryToFilterValue(name) {
  if (!name) return ''
  return name
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
}

/** Flattens a category tree into an indented, parent-sorted list. */
export function flattenCategoryTree(nodes, depth = 0, result = []) {
  for (const node of nodes || []) {
    result.push({ ...node, depth })
    if (node.children && node.children.length) {
      flattenCategoryTree(node.children, depth + 1, result)
    }
  }
  return result
}
