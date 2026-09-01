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


