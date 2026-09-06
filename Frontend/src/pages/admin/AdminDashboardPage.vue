<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuthorization } from '../../composables/useAuthorization.js'
import { getDashboardStats } from '../../services/admin.js'

const auth = useAuthorization()

const loading = ref(true)
const stats = ref(null)

const ACTION_LABELS = {
  REGISTERED: 'Registered',
  LOGIN: 'Signed in',
  SUSPENDED: 'Suspended',
  ACTIVATED: 'Reactivated',
  ROLE_CHANGED: 'Role changed',
}

function formatMoney(v) {
  if (v == null) return '$0.00'
  return '$' + Number(v).toFixed(2)
}

function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString(undefined, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

const maxGrowth = computed(() => {
  if (!stats.value?.userGrowth?.length) return 1
  return Math.max(...stats.value.userGrowth.map((p) => p.count), 1)
})

onMounted(async () => {
  try {
    stats.value = await getDashboardStats()
  } catch (e) {
    // Backend may return empty if no data — still render the shell
    console.error('Dashboard stats error:', e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <h1>Admin Dashboard</h1>
      <p>System administration and oversight</p>
    </div>

    <div v-if="loading" class="dash-loading">Loading statistics…</div>

    <template v-else-if="stats">
      <!-- System statistics -->
      <div class="dashboard-grid">
        <router-link to="/admin/users" class="stat-card stat-card-link">
          <span class="stat-icon">&#9881;</span>
          <h3>User Management</h3>
          <p>Manage all user accounts, roles, and permissions</p>
          <span class="stat-badge stat-badge-live">{{ stats.totalUsers }} users</span>
        </router-link>
        <router-link to="/admin/moderation" class="stat-card stat-card-link">
          <span class="stat-icon">&#128230;</span>
          <h3>Moderation</h3>
          <p>Review vendor applications, products, and events</p>
          <span v-if="stats.pendingVendors || stats.pendingProducts || stats.pendingEvents" class="stat-badge stat-badge-pending">
            {{ (stats.pendingVendors || 0) + (stats.pendingProducts || 0) + (stats.pendingEvents || 0) }} pending
          </span>
          <span v-else class="stat-badge stat-badge-live">All clear</span>
        </router-link>
        <div class="stat-card">
          <span class="stat-icon">&#128722;</span>
          <h3>Orders</h3>
          <p>{{ stats.totalOrders }} total orders</p>
          <span class="stat-badge">{{ formatMoney(stats.totalRevenue) }} revenue</span>
        </div>
        <div class="stat-card">
          <span class="stat-icon">&#128193;</span>
          <h3>Catalog &amp; Events</h3>
          <p>{{ stats.totalProducts }} products · {{ stats.totalEvents }} events</p>
          <span class="stat-badge">Live</span>
        </div>
      </div>

      <!-- Users by role + Orders by status -->
      <div class="dash-row">
        <div class="dash-panel">
          <h2 class="dash-panel-title">Users by Role</h2>
          <div class="dash-role-grid">
            <div v-for="(count, role) in stats.usersByRole" :key="role" class="dash-role-chip">
              <span class="dash-role-name">{{ role }}</span>
              <span class="dash-role-count">{{ count }}</span>
            </div>
          </div>
        </div>

        <div class="dash-panel">
          <h2 class="dash-panel-title">Orders by Status</h2>
          <div class="dash-order-list">
            <div v-for="(count, status) in stats.ordersByStatus" :key="status" class="dash-order-row">
              <span class="dash-order-status">{{ status }}</span>
              <div class="dash-order-bar-wrapper">
                <div
                  class="dash-order-bar"
                  :style="{ width: Math.max((count / Math.max(...Object.values(stats.ordersByStatus), 1)) * 100, 3) + '%' }"
                ></div>
              </div>
              <span class="dash-order-count">{{ count }}</span>
            </div>
            <div v-if="!Object.keys(stats.ordersByStatus).length" class="dash-empty">No orders yet.</div>
          </div>
        </div>
      </div>

      <!-- User growth chart -->
      <div class="dash-panel dash-growth-panel">
        <h2 class="dash-panel-title">User Growth (Last 30 Days)</h2>
        <div v-if="stats.userGrowth?.length" class="dash-growth-chart">
          <div v-for="point in stats.userGrowth" :key="point.date" class="dash-growth-bar-wrap">
            <div class="dash-growth-bar" :style="{ height: (point.count / maxGrowth) * 100 + '%' }"></div>
          </div>
        </div>
        <p v-else class="dash-empty">No registration data yet.</p>
      </div>

      <!-- Recent activity feed -->
      <div class="dash-panel">
        <h2 class="dash-panel-title">Recent Activity</h2>
        <ul v-if="stats.recentActivity?.length" class="dash-activity-list">
          <li v-for="entry in stats.recentActivity" :key="entry.id" class="dash-activity-item">
            <span class="dash-activity-action" :class="'act-' + entry.action.toLowerCase().replace('_', '-')">
              {{ ACTION_LABELS[entry.action] || entry.action }}
            </span>
            <span class="dash-activity-details">{{ entry.details || '—' }}</span>
            <span class="dash-activity-time">{{ formatDateTime(entry.createdAt) }}</span>
          </li>
        </ul>
        <p v-else class="dash-empty">No recent activity recorded.</p>
      </div>
    </template>

    <div v-if="auth.can('manage_users')" class="permission-test">
      <h3>Permission-Based Rendering Test</h3>
      <div class="test-grid">
        <div :class="['test-chip', auth.can('manage_users') ? 'granted' : 'denied']">
          manage_users: {{ auth.can('manage_users') ? 'GRANTED' : 'DENIED' }}
        </div>
        <div :class="['test-chip', auth.can('view_analytics') ? 'granted' : 'denied']">
          view_analytics: {{ auth.can('view_analytics') ? 'GRANTED' : 'DENIED' }}
        </div>
        <div :class="['test-chip', auth.can('manage_system') ? 'granted' : 'denied']">
          manage_system: {{ auth.can('manage_system') ? 'GRANTED' : 'DENIED' }}
        </div>
      </div>
    </div>
  </div>
</template>
