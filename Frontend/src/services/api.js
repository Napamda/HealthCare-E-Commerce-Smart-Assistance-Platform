import axios from 'axios'
import { refreshAccessToken } from './auth.js'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

let refreshPromise = null
let refreshFailed = false  // Once refresh fails, stop trying until user re-logs in.

async function attemptRefreshAndClearOnFailure() {
  // Hard stop — don't attempt refresh again after a failure until login.
  if (refreshFailed) return false
  // De-duplicate concurrent refresh requests from parallel API calls.
  if (refreshPromise) return refreshPromise
  refreshPromise = Promise.resolve().then(async () => {
    const token = localStorage.getItem('refreshToken')
    if (!token) {
      forceLogout()
      return false
    }
    try {
      const data = await refreshAccessToken(token)
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      const user = {
        id: data.userId,
        email: data.email,
        firstName: data.firstName,
        lastName: data.lastName,
        role: data.role,
        avatarUrl: data.avatarUrl || null,
      }
      localStorage.setItem('user', JSON.stringify(user))
      return true
    } catch (_) {
      forceLogout()
      return false
    } finally {
      refreshPromise = null
    }
  })
  return refreshPromise
}

function forceLogout() {
  refreshFailed = true
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  // Hard redirect to login — this stops all pending API calls and
  // breaks the refresh loop when the server restarts and old tokens
  // are invalid.
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

// Allow the login page to reset the flag so the user can log in fresh.
export function resetRefreshState() {
  refreshFailed = false
}

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
    const status = error.response?.status

    if (!status) return Promise.reject(error)

    if (status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshed = await attemptRefreshAndClearOnFailure()
      if (refreshed) {
        const newToken = localStorage.getItem('accessToken')
        if (newToken) {
          originalRequest.headers.Authorization = `Bearer ${newToken}`
        }
        return apiClient.request(originalRequest)
      }
      return Promise.reject(error)
    }

    return Promise.reject(error)
  },
)

export default apiClient
