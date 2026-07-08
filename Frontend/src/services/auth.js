import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken) {
          const data = await refreshAccessToken(refreshToken)
          localStorage.setItem('accessToken', data.accessToken)
          localStorage.setItem('refreshToken', data.refreshToken)
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
          return apiClient(originalRequest)
        }
      } catch (_) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export function register(registrationData) {
  return apiClient
    .post('/api/auth/register', registrationData)
    .then((res) => res.data)
}

export function login(credentials) {
  return apiClient
    .post('/api/auth/login', credentials)
    .then((res) => res.data)
}

export function refreshAccessToken(refreshToken) {
  return apiClient
    .post('/api/auth/refresh', { refreshToken })
    .then((res) => res.data)
}

export function logoutRequest() {
  return apiClient
    .post('/api/auth/logout')
    .then((res) => res.data)
}

export function verifyEmail(token) {
  return apiClient
    .get('/api/auth/verify-email', { params: { token } })
    .then((res) => res.data)
}
