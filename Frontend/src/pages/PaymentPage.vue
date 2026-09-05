<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '../stores/order.js'
import { usePaymentStore } from '../stores/payment.js'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()
const paymentStore = usePaymentStore()

const orderId = computed(() => Number(route.params.orderId))
const order = computed(() => orderStore.selectedOrder)
const paymentMethods = ref([])
const selectedMethod = ref('CARD')
const processing = ref(false)
const paymentComplete = ref(false)
const paymentResult = ref(null)
const showCardForm = ref(false)
const cardForm = ref({
  cardNumber: '',
  cardHolder: '',
  expiryMonth: '',
  expiryYear: '',
  cvv: ''
})

const canPay = computed(() => {
  return order.value && order.value.status === 'PENDING'
})

const totalAmount = computed(() => {
  return order.value ? order.value.totalAmount : 0
})

function formatPrice(price) {
  if (price == null) return '$0.00'
  return '$' + Number(price).toFixed(2)
}

function selectMethod(method) {
  selectedMethod.value = method
  showCardForm.value = method === 'CARD'
}

function validateCardForm() {
  const errors = {}
  if (!cardForm.value.cardNumber.trim()) errors.cardNumber = 'Card number is required'
  if (!cardForm.value.cardHolder.trim()) errors.cardHolder = 'Card holder name is required'
  if (!cardForm.value.expiryMonth) errors.expiryMonth = 'Expiry month is required'
  if (!cardForm.value.expiryYear) errors.expiryYear = 'Expiry year is required'
  if (!cardForm.value.cvv) errors.cvv = 'CVV is required'
  return Object.keys(errors).length === 0 ? null : errors
}

async function initiatePayment() {
  processing.value = true
  try {
    const payment = await paymentStore.initiatePayment(orderId.value, selectedMethod.value)
    if (selectedMethod.value === 'CASH_ON_DELIVERY') {
      paymentComplete.value = true
      paymentResult.value = payment
    } else if (selectedMethod.value === 'CARD') {
      showCardForm.value = true
    } else if (selectedMethod.value === 'PAYPAL') {
      await executePayPalPayment(payment.id)
    }
  } catch (error) {
    console.error('Payment initiation failed:', error)
  } finally {
    processing.value = false
  }
}

async function executeCardPayment() {
  const errors = validateCardForm()
  if (errors) {
    alert('Please fill in all card details')
    return
  }

  processing.value = true
  try {
    const payment = await paymentStore.executeCardPayment(
      paymentStore.currentPayment.id,
      cardForm.value
    )
    paymentComplete.value = true
    paymentResult.value = payment
  } catch (error) {
    console.error('Card payment failed:', error)
    alert('Payment failed: ' + (error.message || 'Unknown error'))
  } finally {
    processing.value = false
  }
}

async function executePayPalPayment(paymentId) {
  processing.value = true
  try {
    const payment = await paymentStore.executePayPalPayment(paymentId)
    paymentComplete.value = true
    paymentResult.value = payment
  } catch (error) {
    console.error('PayPal payment failed:', error)
    alert('Payment failed: ' + (error.message || 'Unknown error'))
  } finally {
    processing.value = false
  }
}

function goToOrders() {
  router.push('/orders')
}

function goToReceipt() {
  if (paymentResult.value) {
    router.push(`/payments/${paymentResult.value.id}/receipt`)
  }
}

onMounted(async () => {
  await orderStore.fetchOrderById(orderId.value)
  await paymentStore.fetchPaymentMethods()
  paymentMethods.value = paymentStore.methods
})
</script>

<template>
  <div class="payment-page">
    <div class="payment-header">
      <button class="back-btn" @click="goToOrders">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
        </svg>
        Back to Orders
      </button>
      <h1>Payment</h1>
    </div>

    <div v-if="orderStore.loading" class="payment-loading">
      <div class="loading-spinner">
        <svg class="spinner" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
      </div>
      <p>Loading payment details...</p>
    </div>

    <div v-else-if="!canPay" class="payment-error">
      <div class="error-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
      </div>
      <h2>Order cannot be paid</h2>
      <p>This order is not eligible for payment.</p>
      <button class="btn-back" @click="goToOrders">Back to Orders</button>
    </div>

    <div v-else-if="paymentComplete" class="payment-success">
      <div class="success-icon">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
          <polyline points="22 4 12 14.01 9 11.01" />
        </svg>
      </div>
      <h2>Payment Successful!</h2>
      <p class="success-message">Your payment has been processed successfully.</p>
      <div class="payment-details">
        <div class="detail-row">
          <span>Order #{{ order.orderNumber || order.id }}</span>
        </div>
        <div class="detail-row">
          <span>Amount Paid</span>
          <span class="amount">{{ formatPrice(totalAmount) }}</span>
        </div>
        <div class="detail-row" v-if="paymentResult">
          <span>Transaction ID</span>
          <span class="transaction-id">{{ paymentResult.transactionId }}</span>
        </div>
      </div>
      <div class="success-actions">
        <button class="btn-primary" @click="goToReceipt">View Receipt</button>
        <button class="btn-secondary" @click="goToOrders">View Orders</button>
      </div>
    </div>

    <div v-else class="payment-content">
      <div class="order-summary">
        <h3>Order Summary</h3>
        <div class="summary-info">
          <div class="summary-row">
            <span>Order #{{ order.orderNumber || order.id }}</span>
          </div>
          <div class="summary-items">
            <div v-for="item in order.items.slice(0, 3)" :key="item.id" class="summary-item">
              <span>{{ item.productName }} × {{ item.quantity }}</span>
              <span>{{ formatPrice(item.subtotal) }}</span>
            </div>
            <span v-if="order.items.length > 3" class="more-items">+{{ order.items.length - 3 }} more items</span>
          </div>
          <div class="summary-divider"></div>
          <div class="summary-total">
            <span>Total Amount</span>
            <span class="total-amount">{{ formatPrice(totalAmount) }}</span>
          </div>
        </div>
      </div>

      <div class="payment-methods">
        <h3>Select Payment Method</h3>
        <div class="methods-grid">
          <div
            v-for="method in paymentMethods"
            :key="method.method"
            class="method-card"
            :class="{ selected: selectedMethod === method.method }"
            @click="selectMethod(method.method)"
          >
            <div class="method-icon">
              <svg v-if="method.method === 'CARD'" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="1" y="4" width="22" height="16" rx="2" ry="2" />
                <line x1="1" y1="10" x2="23" y2="10" />
              </svg>
              <svg v-else-if="method.method === 'PAYPAL'" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M7.03 13.5l1.58-7.5h3.5c1.5 0 2.5.5 2.5 2 0 1.5-1 2-2.5 2h-2l-1 4.5H7.03z" />
                <path d="M13.03 13.5l1.58-7.5h3.5c1.5 0 2.5.5 2.5 2 0 1.5-1 2-2.5 2h-2l-1 4.5h-2.08z" />
              </svg>
              <svg v-else width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="1" y="3" width="15" height="13" />
                <polygon points="16 8 20 8 23 11 23 16 16 16 16 8" />
                <circle cx="5.5" cy="18.5" r="2.5" />
                <circle cx="18.5" cy="18.5" r="2.5" />
              </svg>
            </div>
            <div class="method-info">
              <span class="method-name">{{ method.displayName }}</span>
              <span class="method-desc">{{ method.description }}</span>
            </div>
            <div class="method-badge" v-if="selectedMethod === method.method">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12" />
              </svg>
            </div>
          </div>
        </div>

        <!-- Card Form -->
        <div v-if="showCardForm" class="card-form">
          <h4>Card Details</h4>
          <div class="form-group">
            <label>Card Number</label>
            <input
              v-model="cardForm.cardNumber"
              type="text"
              placeholder="1234 5678 9012 3456"
              maxlength="19"
            />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Card Holder</label>
              <input
                v-model="cardForm.cardHolder"
                type="text"
                placeholder="John Doe"
              />
            </div>
            <div class="form-group">
              <label>Expiry</label>
              <div class="expiry-inputs">
                <input
                  v-model="cardForm.expiryMonth"
                  type="text"
                  placeholder="MM"
                  maxlength="2"
                />
                <span>/</span>
                <input
                  v-model="cardForm.expiryYear"
                  type="text"
                  placeholder="YY"
                  maxlength="2"
                />
              </div>
            </div>
          </div>
          <div class="form-group">
            <label>CVV</label>
            <input
              v-model="cardForm.cvv"
              type="text"
              placeholder="123"
              maxlength="4"
            />
          </div>
        </div>

        <button
          class="btn-pay"
          :disabled="processing"
          @click="initiatePayment"
        >
          <span v-if="processing">Processing...</span>
          <span v-else>Pay {{ formatPrice(totalAmount) }}</span>
        </button>

        <div class="payment-security">
          <div class="security-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            <span>Secure 256-bit SSL encryption</span>
          </div>
          <div class="security-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
            <span>PCI DSS compliant</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.payment-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 80vh;
}

.payment-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}
.payment-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: transparent;
  cursor: pointer;
  transition: all 0.15s;
}
.back-btn:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

.payment-loading, .payment-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  color: var(--color-text-secondary);
}
.loading-spinner {
  margin-bottom: 16px;
}
.spinner {
  animation: spin 1s linear infinite;
  color: var(--color-primary);
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.error-icon {
  color: #dc2626;
  margin-bottom: 16px;
}
.payment-error h2 {
  font-size: 20px;
  color: var(--color-text);
  margin-bottom: 8px;
}
.btn-back {
  margin-top: 16px;
  padding: 10px 20px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.payment-success {
  text-align: center;
  padding: 60px 20px;
}
.success-icon {
  color: #16a34a;
  margin-bottom: 24px;
}
.payment-success h2 {
  font-size: 28px;
  color: var(--color-text);
  margin-bottom: 8px;
}
.success-message {
  font-size: 16px;
  color: var(--color-text-muted);
  margin-bottom: 32px;
}
.payment-details {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 32px;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
  font-size: 14px;
  color: var(--color-text-secondary);
}
.detail-row:last-child {
  border-bottom: none;
}
.amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}
.transaction-id {
  font-family: monospace;
  font-size: 13px;
}
.success-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.btn-primary {
  padding: 14px 28px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}
.btn-secondary {
  padding: 14px 28px;
  background: transparent;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
}

.payment-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 32px;
}

@media (max-width: 1024px) {
  .payment-content { grid-template-columns: 1fr; }
}

.order-summary {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  height: fit-content;
}
.order-summary h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}
.summary-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.summary-row {
  font-size: 14px;
  color: var(--color-text-secondary);
}
.summary-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.summary-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--color-text);
}
.more-items {
  font-size: 12px;
  color: var(--color-text-muted);
  font-style: italic;
}
.summary-divider {
  height: 1px;
  background: var(--color-border);
  margin: 8px 0;
}
.summary-total {
  display: flex;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  padding-top: 8px;
}
.total-amount {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
}

.payment-methods {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}
.payment-methods h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 20px;
}
.methods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.method-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.15s;
}
.method-card:hover {
  border-color: var(--color-primary);
  background: var(--color-bg);
}
.method-card.selected {
  border-color: var(--color-primary);
  background: #eff6ff;
}
.method-icon {
  color: var(--color-primary);
  flex-shrink: 0;
}
.method-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.method-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}
.method-desc {
  font-size: 13px;
  color: var(--color-text-muted);
}
.method-badge {
  color: #16a34a;
  flex-shrink: 0;
}

.card-form {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 20px;
  margin-bottom: 24px;
}
.card-form h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}
.form-group {
  margin-bottom: 16px;
}
.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
}
.form-group input {
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  outline: none;
  transition: border 0.15s;
}
.form-group input:focus {
  border-color: var(--color-primary);
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.expiry-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}
.expiry-inputs input {
  width: 60px;
  text-align: center;
}
.expiry-inputs span {
  color: var(--color-text-muted);
}

.btn-pay {
  width: 100%;
  padding: 16px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}
.btn-pay:hover:not(:disabled) {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}
.btn-pay:disabled {
  opacity: 0.6;
  cursor: default;
}

.payment-security {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
}
.security-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-muted);
}
.security-item svg {
  color: #16a34a;
}

@media (max-width: 640px) {
  .methods-grid {
    grid-template-columns: 1fr;
  }
  .form-row {
    grid-template-columns: 1fr;
  }
  .success-actions {
    flex-direction: column;
  }
}
</style>