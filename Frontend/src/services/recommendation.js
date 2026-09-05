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
    .then((res) => res.data)
}
