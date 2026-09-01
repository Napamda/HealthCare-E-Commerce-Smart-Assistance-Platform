<script setup>
import { useAuthorization } from '../composables/useAuthorization.js'

const auth = useAuthorization()
</script>

<template>
  <div class="vendor-dashboard">
    <div class="dashboard-header">
      <h1>Vendor Dashboard</h1>
      <p>Inventory management and order fulfillment</p>
    </div>

    <div class="dashboard-grid">
      <div class="stat-card">
        <span class="stat-icon">&#128230;</span>
        <h3>Inventory</h3>
        <p>Manage medication stock levels and supplies</p>
        <span class="stat-badge">Coming soon</span>
      </div>
      <div class="stat-card">
        <span class="stat-icon">&#128666;</span>
        <h3>Orders</h3>
        <p>Process incoming and outgoing orders</p>
        <span class="stat-badge">Coming soon</span>
      </div>
      <div class="stat-card">
        <span class="stat-icon">&#128196;</span>
        <h3>Shipments</h3>
        <p>Track deliveries and shipping status</p>
        <span class="stat-badge">Coming soon</span>
      </div>
    </div>

    <div class="permission-test">
      <h3>Permission-Based Rendering Test</h3>
      <div class="test-grid">
        <div :class="['test-chip', auth.can('manage_inventory') ? 'granted' : 'denied']">
          manage_inventory: {{ auth.can('manage_inventory') ? 'GRANTED' : 'DENIED' }}
        </div>
        <div :class="['test-chip', auth.can('process_orders') ? 'granted' : 'denied']">
          process_orders: {{ auth.can('process_orders') ? 'GRANTED' : 'DENIED' }}
        </div>
        <div :class="['test-chip', auth.can('vendor_operations') ? 'granted' : 'denied']">
          vendor_operations: {{ auth.can('vendor_operations') ? 'GRANTED' : 'DENIED' }}
        </div>
        <div :class="['test-chip', auth.can('manage_users') ? 'granted' : 'denied']">
          manage_users: {{ auth.can('manage_users') ? 'GRANTED' : 'DENIED' }}
        </div>
      </div>
    </div>
  </div>
</template>


