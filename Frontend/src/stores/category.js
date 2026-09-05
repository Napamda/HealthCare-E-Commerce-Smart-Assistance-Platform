import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getCategoryTree,
  getAllCategories,
  createCategory,
  updateCategory,
  deleteCategory,
} from '../services/category.js'

export const useCategoryStore = defineStore('category', () => {
  const tree = ref([])
  const all = ref([])
  const loading = ref(false)
  const error = ref(null)

  const roots = computed(() => tree.value)
  const totalCount = computed(() => all.value.length)
  const hasCategories = computed(() => all.value.length > 0)

  function setError(err) {
    error.value = err?.response?.data?.message || err?.message || 'An error occurred'
  }

  async function fetchTree() {
    loading.value = true
    error.value = null
    try {
      tree.value = await getCategoryTree()
    } catch (err) {
      setError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      all.value = await getAllCategories()
    } catch (err) {
      setError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function refresh() {
    await Promise.all([fetchTree(), fetchAll()])
  }

  async function create(data) {
    error.value = null
    try {
      const created = await createCategory(data)
      await refresh()
      return created
    } catch (err) {
      setError(err)
      throw err
    }
  }

  async function update(id, data) {
    error.value = null
    try {
      const updated = await updateCategory(id, data)
      await refresh()
      return updated
    } catch (err) {
      setError(err)
      throw err
    }
  }

  async function remove(id) {
    error.value = null
    try {
      await deleteCategory(id)
      await refresh()
    } catch (err) {
      setError(err)
      throw err
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    tree,
    all,
    loading,
    error,
    roots,
    totalCount,
    hasCategories,
    fetchTree,
    fetchAll,
    refresh,
    create,
    update,
    remove,
    clearError,
  }
})
