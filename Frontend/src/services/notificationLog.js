import apiClient from './api.js'

// Email logs
export function getEmailLogs(type = '') {
  const url = type
    ? `/api/notifications/emails?type=${encodeURIComponent(type)}`
    : '/api/notifications/emails'
  return apiClient.get(url).then((res) => res.data)
}

export function getEmailLog(id) {
  return apiClient.get(`/api/notifications/emails/${id}`).then((res) => res.data)
}

// SMS logs
export function getSmsLogs(type = '') {
  const url = type
    ? `/api/notifications/sms?type=${encodeURIComponent(type)}`
    : '/api/notifications/sms'
  return apiClient.get(url).then((res) => res.data)
}

// Test triggers
export function testWelcomeEmail(email, name) {
  return apiClient
    .post(`/api/notifications/test/welcome?email=${encodeURIComponent(email)}&name=${encodeURIComponent(name)}`)
    .then((res) => res.data)
}

export function testOrderEmail(email, name, orderId, total) {
  return apiClient
    .post(`/api/notifications/test/order?email=${encodeURIComponent(email)}&name=${encodeURIComponent(name)}&orderId=${orderId}&total=${encodeURIComponent(total)}`)
    .then((res) => res.data)
}

export function testPrescriptionEmail(email, name, prescriptionId, status, comments = '') {
  return apiClient
    .post(`/api/notifications/test/prescription?email=${encodeURIComponent(email)}&name=${encodeURIComponent(name)}&prescriptionId=${prescriptionId}&status=${encodeURIComponent(status)}&comments=${encodeURIComponent(comments)}`)
    .then((res) => res.data)
}

export function testEventEmail(email, name, eventTitle, eventDate, venue = '') {
  return apiClient
    .post(`/api/notifications/test/event?email=${encodeURIComponent(email)}&name=${encodeURIComponent(name)}&eventTitle=${encodeURIComponent(eventTitle)}&eventDate=${encodeURIComponent(eventDate)}&venue=${encodeURIComponent(venue)}`)
    .then((res) => res.data)
}
