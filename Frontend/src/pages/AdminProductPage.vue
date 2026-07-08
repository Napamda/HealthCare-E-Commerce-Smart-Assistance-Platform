<script setup>
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useProductStore } from '../stores/product.js'
import ProductForm from '../components/product/ProductForm.vue'

const store = useProductStore()
const {
  products,
  categories,
  isLoading,
  error,
  pagination,
  totalProducts,
} = storeToRefs(store)

const showForm = ref(false)
const editingProduct = ref(null)
const searchQuery = ref('')
const deleteConfirmId = ref(null)

onMounted(async () => {
  await store.fetchCategories()
  await store.fetchProducts(0, 50)
})

function openNewForm() {
  editingProduct.value = null
  showForm.value = true
}

function openEditForm(product) {
  editingProduct.value = { ...product }
  showForm.value = true
}

function onFormClose() {
  showForm.value = false
  editingProduct.value = null
}

async function onFormSave(payload) {
  try {
    if (editingProduct.value?.id) {
      await store.editProduct(editingProduct.value.id, payload)
    } else {
      await store.addProduct(payload)
    }
    onFormClose()
  } catch {
    // error handled in store
  }
}

async function confirmDelete(product) {
  deleteConfirmId.value = product.id
}

async function executeDelete() {
  if (!deleteConfirmId.value) return
  try {
    await store.removeProduct(deleteConfirmId.value)
  } catch {
    // error handled in store
  } finally {
    deleteConfirmId.value = null
  }
}

function cancelDelete() {
  deleteConfirmId.value = null
}

function formatPrice(price) {
  if (price == null) return '$0.00'
  return '$' + Number(price).toFixed(2)
}

function categoryLabel(cat) {
  const labels = {
    VITAMINS: 'Vitamins', PAIN_RELIEF: 'Pain Relief', SKIN_CARE: 'Skin Care',
    DIGESTIVE_HEALTH: 'Digestive Health', RESPIRATORY: 'Respiratory', HEART_HEALTH: 'Heart Health',
    DIABETES_CARE: 'Diabetes Care', FIRST_AID: 'First Aid', MEDICAL_DEVICES: 'Medical Devices',
    PERSONAL_CARE: 'Personal Care', WELLNESS: 'Wellness', BABY_CARE: 'Baby Care',
    ELDERLY_CARE: 'Elderly Care', OTHER: 'Other',
  }
  return labels[cat] || cat
}

const filteredProducts = computed(() => {
  if (!searchQuery.value) return products.value
  const q = searchQuery.value.toLowerCase()
  return products.value.filter(
    (p) =>
      p.name?.toLowerCase().includes(q) ||
      p.manufacturer?.toLowerCase().includes(q) ||
      p.category?.toLowerCase().includes(q),
  )
})
</script>

<template>
  <div class="admin-page">
    <!-- Header -->
    <div class="admin-header">
      <div>
        <h1 class="admin-title">Product Management</h1>
        <p class="admin-subtitle">{{ totalProducts }} products total</p>
      </div>
      <button class="btn-add" @click="openNewForm">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        Add Product
      </button>
    </div>

    <!-- Error -->
    <div v-if="error" class="error-banner">
      <span>{{ error }}</span>
      <button class="btn-dismiss" @click="store.clearError()">Dismiss</button>
    </div>

    <!-- Search -->
    <div class="admin-search">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="search-icon">
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
      <input
        v-model="searchQuery"
        type="text"
        class="search-input"
        placeholder="Search by name, manufacturer or category..."
      />
    </div>

    <!-- Loading -->
    <div v-if="isLoading && products.length === 0" class="loading-state">
      <div class="dot-typing"><span></span><span></span><span></span></div>
      <p>Loading products...</p>
    </div>

    <!-- Empty -->
    <div v-else-if="filteredProducts.length === 0 && !isLoading" class="empty-state">
      <p>No products found. Add your first product!</p>
      <button class="btn-add-link" @click="openNewForm">Add Product</button>
    </div>

    <!-- Table -->
    <div v-else class="table-wrapper">
      <table class="product-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Category</th>
            <th>Price</th>
            <th>Stock</th>
            <th>Rx</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in filteredProducts" :key="product.id">
            <td class="col-id">#{{ product.id }}</td>
            <td>
              <div class="td-name">{{ product.name }}</div>
              <div class="td-sub">{{ product.manufacturer || '' }}</div>
            </td>
            <td>
              <span class="td-category">{{ categoryLabel(product.category) }}</span>
            </td>
            <td class="col-price">{{ formatPrice(product.price) }}</td>
            <td class="col-stock">{{ product.stockQuantity ?? 0 }}</td>
            <td>
              <span v-if="product.prescriptionRequired" class="badge-rx">Rx</span>
              <span v-else class="badge-otc">OTC</span>
            </td>
            <td>
              <div class="action-buttons">
                <button class="btn-action btn-edit" @click="openEditForm(product)" title="Edit">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                  </svg>
                </button>
                <button class="btn-action btn-delete" @click="confirmDelete(product)" title="Delete">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6" />
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                  </svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Product Form Modal -->
    <ProductForm
      v-if="showForm"
      :product="editingProduct"
      :categories="categories"
      @save="onFormSave"
      @close="onFormClose"
    />

    <!-- Delete Confirmation -->
    <div v-if="deleteConfirmId" class="dialog-overlay" @click.self="cancelDelete">
      <div class="confirm-card">
        <div class="confirm-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <h3 class="confirm-title">Delete Product</h3>
        <p class="confirm-text">Are you sure you want to delete this product? This action cannot be undone.</p>
        <div class="confirm-actions">
          <button class="btn-cancel-confirm" @click="cancelDelete">Cancel</button>
          <button class="btn-delete-confirm" @click="executeDelete">Delete</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'
</script>

<style scoped>
.admin-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: 100vh;
}

.admin-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}
.admin-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}
.admin-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.btn-add {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  background: var(--color-primary);
  color: #fff;
  transition: background 0.15s;
}
.btn-add:hover { background: var(--color-primary-dark); }

.error-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fef2f2;
  color: var(--color-danger);
  border: 1px solid #fecaca;
  border-radius: var(--radius-md);
  font-size: 13px;
  margin-bottom: 16px;
}
.btn-dismiss {
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  background: var(--color-danger);
  color: #fff;
  font-size: 12px;
}

.admin-search {
  position: relative;
  margin-bottom: 20px;
}
.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
}
.search-input {
  width: 100%;
  padding: 10px 14px 10px 40px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-surface);
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search-input:focus {
  outline: none;
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  color: var(--color-text-secondary);
}
.btn-add-link {
  margin-top: 12px;
  padding: 8px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

/* Table */
.table-wrapper {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.product-table {
  width: 100%;
  border-collapse: collapse;
}
.product-table th {
  text-align: left;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
}
.product-table td {
  padding: 12px 16px;
  font-size: 14px;
  color: var(--color-text);
  border-bottom: 1px solid var(--color-border);
}
.product-table tr:last-child td { border-bottom: none; }
.product-table tr:hover td { background: var(--color-primary-bg); }
.col-id { color: var(--color-text-muted); font-weight: 500; min-width: 60px; }
.td-name { font-weight: 600; }
.td-sub { font-size: 12px; color: var(--color-text-muted); }
.td-category {
  display: inline-block;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  background: var(--color-primary-bg);
  color: var(--color-primary);
}
.col-price { font-weight: 600; color: var(--color-primary); }
.col-stock { font-weight: 500; }
.badge-rx {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 700;
  background: #fef2f2;
  color: var(--color-danger);
}
.badge-otc {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 500;
  background: #ecfdf5;
  color: var(--color-success);
}

.action-buttons { display: flex; gap: 6px; }
.btn-action {
  padding: 6px;
  border-radius: var(--radius-sm);
  transition: background 0.15s, color 0.15s;
}
.btn-edit { color: var(--color-text-muted); }
.btn-edit:hover { color: var(--color-primary); background: var(--color-primary-bg); }
.btn-delete { color: var(--color-text-muted); }
.btn-delete:hover { color: var(--color-danger); background: #fef2f2; }

/* Delete confirmation */
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
}
.confirm-card {
  width: 400px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 32px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.confirm-icon { color: var(--color-danger); margin-bottom: 12px; }
.confirm-title { font-size: 18px; font-weight: 700; margin-bottom: 8px; }
.confirm-text { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 24px; }
.confirm-actions { display: flex; gap: 10px; justify-content: center; }
.btn-cancel-confirm {
  padding: 10px 24px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.btn-delete-confirm {
  padding: 10px 24px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  background: var(--color-danger);
  color: #fff;
}
</style>
