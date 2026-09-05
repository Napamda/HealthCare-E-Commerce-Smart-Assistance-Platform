import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getRecommendations, getPersonalizedRecommendations, getRecommendationsByCategory } from '../services/recommendation.js'

export const useRecommendationStore = defineStore('recommendation', () => {
  const recommendations = ref([])
  const personalized = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchRecommendations() {
    loading.value = true
    error.value = null
    try {
      recommendations.value = await getRecommendations()
    } catch (err) {
      error.value = 'Failed to load recommendations'
      console.error('Failed to fetch recommendations:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchPersonalized() {
    loading.value = true
    error.value = null
    try {
      personalized.value = await getPersonalizedRecommendations()
    } catch (err) {
      error.value = 'Failed to load personalized recommendations'
      console.error('Failed to fetch personalized recommendations:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchByCategory(category) {
    loading.value = true
    error.value = null
    try {
      const data = await getRecommendationsByCategory(category)
      return data
    } catch (err) {
      error.value = 'Failed to load category recommendations'
      console.error('Failed to fetch category recommendations:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    recommendations,
    personalized,
    loading,
    error,
    fetchRecommendations,
    fetchPersonalized,
    fetchByCategory,
    clearError,
  }
})