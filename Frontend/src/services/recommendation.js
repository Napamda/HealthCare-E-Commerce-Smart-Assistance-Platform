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

export function getRecommendations() {
  return api.get('/api/recommendations').then((res) => res.data)
}

export function getPersonalizedRecommendations() {
  return api.get('/api/recommendations/personalized').then((res) => res.data)
}

export function getRecommendationsByCategory(category) {
  return api.get(`/api/recommendations/category/${category}`).then((res) => res.data)
}