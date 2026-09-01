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

export function getCart() {
  return api.get('/api/cart').then((res) => res.data)
}

export function getCartCount() {
  return api.get('/api/cart/count').then((res) => res.data)
}

export function addToCart({ productId, productName, productImage, quantity, unitPrice }) {
  return api.post('/api/cart/add', {
    productId, productName, productImage, quantity, unitPrice,
  }).then((res) => res.data)
}

export function updateCartQuantity(cartItemId, quantity) {
  return api.put(`/api/cart/${cartItemId}`, null, { params: { quantity } })
    .then((res) => res.data)
}

export function removeFromCart(cartItemId) {
  return api.delete(`/api/cart/${cartItemId}`)
}

export function clearCart() {
  return api.delete('/api/cart/clear')
}
