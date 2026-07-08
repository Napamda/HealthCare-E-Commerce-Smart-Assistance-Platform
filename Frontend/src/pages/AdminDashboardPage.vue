<script setup>
import { useAuthorization } from '../composables/useAuthorization.js'

const auth = useAuthorization()
</script>

<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <h1>Admin Dashboard</h1>
      <p>System administration and oversight</p>
    </div>

    <div class="dashboard-grid">
      <div class="stat-card">
        <span class="stat-icon">&#9881;</span>
        <h3>User Management</h3>
        <p>Manage all user accounts, roles, and permissions</p>
        <span class="stat-badge">Coming soon</span>
      </div>
      <div class="stat-card">
        <span class="stat-icon">&#9776;</span>
        <h3>System Analytics</h3>
        <p>View platform-wide metrics and reports</p>
        <span class="stat-badge">Coming soon</span>
      </div>
      <div class="stat-card">
        <span class="stat-icon">&#128274;</span>
        <h3>Security &amp; Audit</h3>
        <p>Monitor access logs and security events</p>
        <span class="stat-badge">Coming soon</span>
      </div>
      <div class="stat-card">
        <span class="stat-icon">&#9883;</span>
        <h3>System Config</h3>
        <p>Configure platform settings and integrations</p>
        <span class="stat-badge">Coming soon</span>
      </div>
    </div>

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
        <div :class="['test-chip', auth.can('review_prescriptions') ? 'granted' : 'denied']">
          review_prescriptions: {{ auth.can('review_prescriptions') ? 'GRANTED' : 'DENIED' }}
        </div>
        <div :class="['test-chip', auth.can('manage_inventory') ? 'granted' : 'denied']">
          manage_inventory: {{ auth.can('manage_inventory') ? 'GRANTED' : 'DENIED' }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-dashboard {
  padding: 32px 40px;
  max-width: 1100px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.dashboard-header {
  margin-bottom: 32px;
}

.dashboard-header h1 {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 6px;
}

.dashboard-header p {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  padding: 24px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
}

.stat-icon {
  font-size: 28px;
  display: block;
  margin-bottom: 12px;
}

.stat-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 8px;
}

.stat-card p {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0 0 12px;
  line-height: 1.5;
}

.stat-badge {
  display: inline-block;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-primary);
  background: var(--color-primary-bg, rgba(37, 99, 235, 0.08));
  border-radius: var(--radius-full);
}

.permission-test {
  padding: 24px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
}

.permission-test h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px;
  color: var(--color-text);
}

.test-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.test-chip {
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
  border-radius: var(--radius-full);
}

.test-chip.granted {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.test-chip.denied {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
}
</style>
