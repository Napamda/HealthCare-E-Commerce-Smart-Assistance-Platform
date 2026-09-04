<script setup>
import { ref, onMounted } from 'vue'
import { useAuthorization } from '../composables/useAuthorization.js'
import { useInventoryStore } from '../stores/inventory.js'

const auth = useAuthorization()
const inventoryStore = useInventoryStore()

const stats = ref({
  totalProducts: 0,
  totalUnits: 0,
  lowStock: 0,
  outOfStock: 0,
  pendingOrders: 0,
  recentActivity: []
})

const loading = ref(true)

onMounted(async () => {
  try {
    await inventoryStore.fetchStock()
    stats.value.totalProducts = inventoryStore.totalProducts
    stats.value.totalUnits = inventoryStore.totalUnits
    stats.value.lowStock = inventoryStore.lowStockCount
    stats.value.outOfStock = inventoryStore.outOfStockCount
  } catch (error) {
    console.error('Failed to load dashboard data:', error)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="vendor-dashboard">
    <!-- Header -->
    <div class="dashboard-header">
      <div class="header-content">
        <h1 class="header-title">Vendor Dashboard</h1>
        <p class="header-subtitle">Manage your inventory, track orders, and monitor stock levels</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="$router.push('/inventory')">
          <span class="btn-icon">📦</span>
          Manage Inventory
        </button>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="stats-section">
      <h2 class="section-title">Overview</h2>
      <div class="stats-grid">
        <div class="stat-card stat-primary">
          <div class="stat-icon">📦</div>
          <div class="stat-content">
            <div class="stat-label">Total Products</div>
            <div class="stat-value">{{ stats.totalProducts }}</div>
            <div class="stat-trend">Active inventory items</div>
          </div>
        </div>
        <div class="stat-card stat-success">
          <div class="stat-icon">📊</div>
          <div class="stat-content">
            <div class="stat-label">Units in Stock</div>
            <div class="stat-value">{{ stats.totalUnits }}</div>
            <div class="stat-trend">Total available units</div>
          </div>
        </div>
        <div class="stat-card stat-warning">
          <div class="stat-icon">⚠️</div>
          <div class="stat-content">
            <div class="stat-label">Low Stock</div>
            <div class="stat-value">{{ stats.lowStock }}</div>
            <div class="stat-trend">Products below threshold</div>
          </div>
        </div>
        <div class="stat-card stat-danger">
          <div class="stat-icon">🚨</div>
          <div class="stat-content">
            <div class="stat-label">Out of Stock</div>
            <div class="stat-value">{{ stats.outOfStock }}</div>
            <div class="stat-trend">Products unavailable</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="actions-section">
      <h2 class="section-title">Quick Actions</h2>
      <div class="actions-grid">
        <router-link to="/inventory" class="action-card">
          <div class="action-icon">📦</div>
          <div class="action-content">
            <h3 class="action-title">Inventory Management</h3>
            <p class="action-description">Monitor stock levels, update quantities, and view stock history</p>
          </div>
          <div class="action-arrow">→</div>
        </router-link>
        <div class="action-card action-disabled">
          <div class="action-icon">🛒</div>
          <div class="action-content">
            <h3 class="action-title">Order Management</h3>
            <p class="action-description">Process incoming orders and manage fulfillment</p>
          </div>
          <div class="action-badge">Coming Soon</div>
        </div>
        <div class="action-card action-disabled">
          <div class="action-icon">🚚</div>
          <div class="action-content">
            <h3 class="action-title">Shipments</h3>
            <p class="action-description">Track deliveries and shipping status</p>
          </div>
          <div class="action-badge">Coming Soon</div>
        </div>
        <div class="action-card action-disabled">
          <div class="action-icon">📈</div>
          <div class="action-content">
            <h3 class="action-title">Analytics</h3>
            <p class="action-description">View sales reports and performance metrics</p>
          </div>
          <div class="action-badge">Coming Soon</div>
        </div>
      </div>
    </div>

    <!-- Recent Activity -->
    <div class="activity-section">
      <h2 class="section-title">System Status</h2>
      <div class="activity-grid">
        <div class="activity-item">
          <div class="activity-icon activity-success">✓</div>
          <div class="activity-content">
            <div class="activity-title">Inventory System</div>
            <div class="activity-description">Stock tracking and management active</div>
          </div>
          <div class="activity-status">Operational</div>
        </div>
        <div class="activity-item">
          <div class="activity-icon activity-pending">⏳</div>
          <div class="activity-content">
            <div class="activity-title">Order Processing</div>
            <div class="activity-description">Automatic stock validation enabled</div>
          </div>
          <div class="activity-status">Configured</div>
        </div>
        <div class="activity-item">
          <div class="activity-icon activity-info">ℹ</div>
          <div class="activity-content">
            <div class="activity-title">Stock Reservations</div>
            <div class="activity-description">Checkout stock reservation active</div>
          </div>
          <div class="activity-status">Active</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vendor-dashboard {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

/* Header */
.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 40px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--color-border);
}

.header-content {
  flex: 1;
}

.header-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--color-text);
}

.header-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* Buttons */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary {
  background: var(--color-primary);
  color: #fff;
}

.btn-primary:hover {
  background: var(--color-primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.btn-icon {
  font-size: 16px;
}

/* Sections */
.section-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 20px;
  color: var(--color-text);
}

.stats-section,
.actions-section,
.activity-section {
  margin-bottom: 40px;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  font-size: 32px;
  line-height: 1;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1;
  margin-bottom: 4px;
}

.stat-trend {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.stat-primary .stat-icon { filter: hue-rotate(210deg); }
.stat-success .stat-icon { filter: hue-rotate(140deg); }
.stat-warning .stat-icon { filter: hue-rotate(30deg); }
.stat-danger .stat-icon { filter: hue-rotate(0deg); }

/* Actions Grid */
.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  text-decoration: none;
  color: inherit;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}

.action-card:hover:not(.action-disabled) {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.action-card.action-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-icon {
  font-size: 28px;
  line-height: 1;
}

.action-content {
  flex: 1;
}

.action-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--color-text);
}

.action-description {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 0;
}

.action-arrow {
  font-size: 20px;
  color: var(--color-primary);
  font-weight: 700;
}

.action-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: var(--color-bg);
  color: var(--color-text-muted);
  font-size: 10px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: var(--radius-full);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Activity Grid */
.activity-grid {
  display: grid;
  gap: 12px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px 20px;
}

.activity-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.activity-success {
  background: rgba(22, 163, 74, 0.1);
  color: var(--color-success);
}

.activity-pending {
  background: rgba(217, 119, 6, 0.1);
  color: var(--color-warning);
}

.activity-info {
  background: rgba(37, 99, 235, 0.1);
  color: var(--color-primary);
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.activity-description {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 0;
}

.activity-status {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-success);
  padding: 4px 12px;
  background: rgba(22, 163, 74, 0.1);
  border-radius: var(--radius-full);
}

/* Responsive */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .btn {
    width: 100%;
    justify-content: center;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .actions-grid {
    grid-template-columns: 1fr;
  }
}
</style>

