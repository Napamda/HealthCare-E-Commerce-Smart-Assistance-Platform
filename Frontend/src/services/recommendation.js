import apiClient from './api.js'

export function getRecommendations({
  query = '',
  symptoms = [],
  currentConditions = [],
  previousConditions = [],
  preferredCategory = '',
  allergies = [],
  recentlyBrowsedCategories = [],
  maxResults = 5,
} = {}) {
  console.log('Making recommendation API call with:', {
    query,
    symptoms,
    currentConditions,
    previousConditions,
    preferredCategory,
    allergies,
    recentlyBrowsedCategories,
    maxResults,
  })
  return apiClient
    .post('/api/recommendations', {
      query,
      symptoms,
      currentConditions,
      previousConditions,
      preferredCategory,
      allergies,
      recentlyBrowsedCategories,
      maxResults,
    })
    .then((res) => {
      console.log('API response received:', res)
      console.log('Response data:', res.data)
      return res.data
    })
    .catch((error) => {
      console.error('API call failed:', error)
      console.error('Error response:', error.response)
      throw error
    })
}
