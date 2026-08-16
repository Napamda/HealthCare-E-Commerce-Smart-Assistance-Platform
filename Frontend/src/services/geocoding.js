import apiClient from './api.js'

export function geocodeAddress(address) {
  return apiClient
    .get('/api/geocode', { params: { address } })
    .then((res) => res.data)
}

export function reverseGeocode(lat, lng) {
  return apiClient
    .get('/api/geocode/reverse', { params: { lat, lng } })
    .then((res) => res.data)
}
