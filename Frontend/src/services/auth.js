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
  // MUST use plain axios — NOT the interceptor-equipped apiClient.
  // Otherwise a 401 from the refresh call triggers the interceptor,
  // which calls refreshAccessToken again → infinite loop.
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return axios
    .post(`${base}/api/auth/refresh`, { refreshToken }, {
      headers: { 'Content-Type': 'application/json' },
      timeout: 10000,
    })
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
