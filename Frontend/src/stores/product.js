import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  listProducts,
  searchProducts,
  getProduct,
  getCategories,
  getCategoriesWithCounts,
  createProduct,
  updateProduct,
  deleteProduct,
} from '../services/product.js'

export const useProductStore = defineStore('product', () => {
  // ----------------------------------------------------------------
  // State
  // ----------------------------------------------------------------
  const products = ref([])
  const selectedProduct = ref(null)
  const categories = ref([])
  const categoryCounts = ref([])
  const isLoading = ref(false)
  const error = ref(null)

  // Pagination
  const pagination = ref({
    page: 0,
    size: 12,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  })

  // Filters
  const filters = ref({
    keyword: '',
    category: '',
    minPrice: null,
    maxPrice: null,
  })

  // ----------------------------------------------------------------
  // Getters
  // ----------------------------------------------------------------
  const totalProducts = computed(() => pagination.value.totalElements)
  const currentPage = computed(() => pagination.value.page)
  const isLastPage = computed(() => pagination.value.last)

  // ----------------------------------------------------------------
  // Actions
  // ----------------------------------------------------------------
  async function fetchProducts(page = 0, size = 12) {
    isLoading.value = true
    error.value = null

    try {
      const hasActiveFilters =
        filters.value.keyword ||
        filters.value.category ||
        filters.value.minPrice !== null ||
        filters.value.maxPrice !== null

      let data
      if (hasActiveFilters) {
        data = await searchProducts({
          keyword: filters.value.keyword || undefined,
          category: filters.value.category || undefined,
          minPrice: filters.value.minPrice,
          maxPrice: filters.value.maxPrice,
          page,
          size,
        })
      } else {
        data = await listProducts(page, size)
      }

      products.value = data.content || []
      pagination.value = {
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        first: data.first,
        last: data.last,
      }
    } catch (err) {
      error.value = err.response?.data?.error || err.message
    } finally {
      isLoading.value = false
    }
  }

  async function fetchProductById(id) {
    isLoading.value = true
    error.value = null

    try {
      const data = await getProduct(id)
      selectedProduct.value = data
      return data
    } catch (err) {
      error.value = err.response?.data?.error || err.message
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function fetchCategories() {
    try {
      const data = await getCategories()
      categories.value = data
    } catch (err) {
      error.value = err.response?.data?.error || err.message
    }
  }

  async function fetchCategoryCounts() {
    try {
      const data = await getCategoriesWithCounts()
      categoryCounts.value = data || []
    } catch (err) {
      error.value = err.response?.data?.error || err.message
    }
  }

  async function applyFilters(newFilters) {
    filters.value = { ...filters.value, ...newFilters }
    await fetchProducts(0, pagination.value.size)
  }

  async function clearFilters() {
    filters.value = {
      keyword: '',
      category: '',
      minPrice: null,
      maxPrice: null,
    }
    await fetchProducts(0, pagination.value.size)
  }

  async function loadMore() {
    if (isLastPage.value || isLoading.value) return
    const nextPage = pagination.value.page + 1

    isLoading.value = true
    error.value = null

    try {
      const hasActiveFilters =
        filters.value.keyword ||
        filters.value.category ||
        filters.value.minPrice !== null ||
        filters.value.maxPrice !== null

      let data
      if (hasActiveFilters) {
        data = await searchProducts({
          keyword: filters.value.keyword || undefined,
          category: filters.value.category || undefined,
          minPrice: filters.value.minPrice,
          maxPrice: filters.value.maxPrice,
          page: nextPage,
          size: pagination.value.size,
        })
      } else {
        data = await listProducts(nextPage, pagination.value.size)
      }

      products.value = [...products.value, ...(data.content || [])]
      pagination.value = {
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        first: data.first,
        last: data.last,
      }
    } catch (err) {
      error.value = err.response?.data?.error || err.message
    } finally {
      isLoading.value = false
    }
  }

  async function addProduct(productData) {
    isLoading.value = true
    error.value = null

    try {
      const data = await createProduct(productData)
      products.value.unshift(data)
      return data
    } catch (err) {
      error.value = err.response?.data?.error || err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function editProduct(id, productData) {
    isLoading.value = true
    error.value = null

    try {
      const data = await updateProduct(id, productData)
      const index = products.value.findIndex((p) => p.id === id)
      if (index !== -1) {
        products.value[index] = data
      }
      if (selectedProduct.value?.id === id) {
        selectedProduct.value = data
      }
      return data
    } catch (err) {
      error.value = err.response?.data?.error || err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function removeProduct(id) {
    isLoading.value = true
    error.value = null

    try {
      await deleteProduct(id)
      products.value = products.value.filter((p) => p.id !== id)
      if (selectedProduct.value?.id === id) {
        selectedProduct.value = null
      }
    } catch (err) {
      error.value = err.response?.data?.error || err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    // state
    products,
    selectedProduct,
    categories,
    categoryCounts,
    isLoading,
    error,
    pagination,
    filters,
    // getters
    totalProducts,
    currentPage,
    isLastPage,
    // actions
    fetchProducts,
    fetchProductById,
    fetchCategories,
    fetchCategoryCounts,
    applyFilters,
    clearFilters,
    loadMore,
    addProduct,
    editProduct,
    removeProduct,
    clearError,
  }
})
