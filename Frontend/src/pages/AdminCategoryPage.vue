<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useCategoryStore } from '../stores/category.js'
import { flattenCategoryTree } from '../services/category.js'

const store = useCategoryStore()
const { tree, all, loading, error, totalCount } = storeToRefs(store)

const showForm = ref(false)
const editingNode = ref(null)
const searchQuery = ref('')
const expanded = ref({})
const deleteConfirm = ref(null)
const busy = ref(false)

const form = reactive({
  name: '',
  slug: '',
  description: '',
  imageUrl: '',
  parentId: null,
  active: true,
  sortOrder: 0,
})

onMounted(async () => {
  await store.refresh()
})

function resetForm() {
  form.name = ''
  form.slug = ''
  form.description = ''
  form.imageUrl = ''
  form.parentId = null
  form.active = true
  form.sortOrder = 0
}

async function onSubmit() {
  if (!form.name.trim()) return
  const payload = {
    name: form.name.trim(),
    slug: form.slug.trim() || undefined,
    description: form.description.trim() || undefined,
    imageUrl: form.imageUrl.trim() || undefined,
    parentId: form.parentId || null,
    active: form.active,
    sortOrder: Number(form.sortOrder) || 0,
  }
  await onFormSave(payload)
}

const flatList = computed(() => flattenCategoryTree(tree.value))

const filteredList = computed(() => {
  if (!searchQuery.value) return flatList.value
  const q = searchQuery.value.toLowerCase()
  return flatList.value.filter(
    (n) =>
      n.name?.toLowerCase().includes(q) ||
      n.slug?.toLowerCase().includes(q) ||
      n.description?.toLowerCase().includes(q),
  )
})

function toggle(node) {
  expanded.value[node.id] = !expanded.value[node.id]
}

function openNewForm() {
  resetForm()
  editingNode.value = null
  showForm.value = true
}

function openSubForm(node) {
  resetForm()
  form.parentId = node.id
  editingNode.value = null
  showForm.value = true
}

function openEditForm(node) {
  editingNode.value = { ...node, parentId: node.parentId ?? null }
  form.name = node.name || ''
  form.slug = node.slug || ''
  form.description = node.description || ''
  form.imageUrl = node.imageUrl || ''
  form.parentId = node.parentId ?? null
  form.active = node.active !== false
  form.sortOrder = node.sortOrder ?? 0
  showForm.value = true
}

function onFormClose() {
  showForm.value = false
  editingNode.value = null
}

async function onFormSave(payload) {
  try {
    busy.value = true
    if (editingNode.value?.id) {
      await store.update(editingNode.value.id, payload)
    } else {
      await store.create(payload)
    }
    onFormClose()
  } catch {
    // error handled in store
  } finally {
    busy.value = false
  }
}

function confirmDelete(node) {
  deleteConfirm.value = node
}

async function executeDelete() {
  if (!deleteConfirm.value) return
  try {
    busy.value = true
    await store.remove(deleteConfirm.value.id)
  } catch {
    // error handled in store
  } finally {
    busy.value = false
    deleteConfirm.value = null
  }
}

function cancelDelete() {
  deleteConfirm.value = null
}

function childCount(node) {
  return node.children?.length || 0
}
</script>

<template>
  <div class="admin-page">
    <!-- Header -->
    <div class="admin-header">
      <div>
        <h1 class="admin-title">Category Management</h1>
        <p class="admin-subtitle">{{ totalCount }} categories • {{ flatList.length }} in active tree</p>
      </div>
      <button class="btn-add" @click="openNewForm">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        Add Category
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
        placeholder="Search by name, slug or description..."
      />
    </div>

    <!-- Loading -->
    <div v-if="loading && flatList.length === 0" class="loading-state">
      <div class="dot-typing"><span></span><span></span><span></span></div>
      <p>Loading categories...</p>
    </div>

    <!-- Empty -->
    <div v-else-if="filteredList.length === 0 && !loading" class="empty-state">
      <p>No categories found. Create your first category!</p>
      <button class="btn-add-link" @click="openNewForm">Add Category</button>
    </div>

    <!-- Tree list -->
    <div v-else class="table-wrapper">
      <table class="category-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Slug</th>
            <th>Products</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="node in filteredList" :key="node.id">
            <td>
              <div class="td-name-wrap" :style="{ paddingLeft: node.depth * 24 + 'px' }">
                <button
                  v-if="childCount(node) > 0"
                  class="tree-toggle"
                  :class="{ 'tree-toggle--open': expanded[node.id] }"
                  @click="toggle(node)"
                >
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="9 18 15 12 9 6" />
                  </svg>
                </button>
                <span v-else class="tree-toggle tree-toggle--empty" />
                <span class="td-name">{{ node.name }}</span>
              </div>
            </td>
            <td><span class="td-slug">{{ node.slug || '—' }}</span></td>
            <td class="col-count">{{ node.productCount ?? 0 }}</td>
            <td>
              <span v-if="node.active !== false" class="badge-active">Active</span>
              <span v-else class="badge-inactive">Inactive</span>
            </td>
            <td>
              <div class="action-buttons">
                <button class="btn-action btn-edit" @click="openSubForm(node)" title="Add Sub-category">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                </button>
                <button class="btn-action btn-edit" @click="openEditForm(node)" title="Edit">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                    stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                  </svg>
                </button>
                <button class="btn-action btn-delete" @click="confirmDelete(node)" title="Delete">
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

    <!-- Category Form Modal -->
    <div v-if="showForm" class="dialog-overlay" @click.self="onFormClose">
      <div class="form-card">
        <div class="form-header">
          <h3 class="form-title">{{ editingNode?.id ? 'Edit Category' : 'New Category' }}</h3>
          <button class="form-close" @click="onFormClose">&times;</button>
        </div>

        <form class="form-body" @submit.prevent="onSubmit">
          <div class="form-grid">
            <label class="form-field">
              <span class="form-label">Name <em>*</em></span>
              <input v-model.trim="form.name" type="text" class="form-input" placeholder="e.g. Vitamins" required />
            </label>

            <label class="form-field">
              <span class="form-label">Slug</span>
              <input v-model.trim="form.slug" type="text" class="form-input" placeholder="e.g. vitamins (auto if blank)" />
            </label>

            <label class="form-field">
              <span class="form-label">Parent Category</span>
              <select v-model="form.parentId" class="form-input">
                <option :value="null">— None (root) —</option>
                <option
                  v-for="node in flatList.filter((n) => n.id !== editingNode?.id)"
                  :key="node.id"
                  :value="node.id"
                >
                  {{ '— '.repeat(node.depth) }}{{ node.name }}
                </option>
              </select>
            </label>

            <label class="form-field">
              <span class="form-label">Sort Order</span>
              <input v-model.number="form.sortOrder" type="number" min="0" class="form-input" placeholder="0" />
            </label>

            <label class="form-field form-field--full">
              <span class="form-label">Description</span>
              <textarea v-model.trim="form.description" class="form-input form-textarea"
                placeholder="Short description shown to shoppers..." rows="2"></textarea>
            </label>

            <label class="form-field form-field--full">
              <span class="form-label">Image URL</span>
              <input v-model.trim="form.imageUrl" type="url" class="form-input" placeholder="https://..." />
            </label>

            <label class="form-check">
              <input v-model="form.active" type="checkbox" class="form-checkbox" />
              <span>Active (visible on the storefront)</span>
            </label>
          </div>

          <div v-if="store.error" class="error-banner">
            <span>{{ store.error }}</span>
          </div>

          <div class="form-actions">
            <button type="button" class="btn-cancel-confirm" @click="onFormClose">Cancel</button>
            <button type="submit" class="btn-save-confirm" :disabled="busy">
              {{ busy ? 'Saving...' : editingNode?.id ? 'Save Changes' : 'Create Category' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete Confirmation -->
    <div v-if="deleteConfirm" class="dialog-overlay" @click.self="cancelDelete">
      <div class="confirm-card">
        <div class="confirm-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <h3 class="confirm-title">Delete Category</h3>
        <p class="confirm-text">
          Delete "{{ deleteConfirm.name }}"? Sub-categories must be moved or deleted first.
          This action cannot be undone.
        </p>
        <div class="confirm-actions">
          <button class="btn-cancel-confirm" @click="cancelDelete">Cancel</button>
          <button class="btn-delete-confirm" @click="executeDelete" :disabled="busy">
            {{ busy ? 'Deleting...' : 'Delete' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

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
  cursor: pointer;
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
  cursor: pointer;
}

.admin-search {
  position: relative;
  margin-bottom: 20px;
}
.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-secondary);
}
.search-input {
  width: 100%;
  padding: 10px 14px 10px 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-surface);
  color: var(--color-text);
}
.search-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(46, 134, 193, 0.15);
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}
.dot-typing {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}
.dot-typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: dotPulse 1.2s infinite ease-in-out;
}
.dot-typing span:nth-child(2) { animation-delay: 0.15s; }
.dot-typing span:nth-child(3) { animation-delay: 0.3s; }
@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.25; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}
.btn-add-link {
  margin-top: 12px;
  color: var(--color-primary);
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
}

.table-wrapper {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-surface);
}
.category-table {
  width: 100%;
  border-collapse: collapse;
}
.category-table th {
  text-align: left;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--color-text-secondary);
  background: var(--color-surface-alt, #f8f9fa);
  border-bottom: 1px solid var(--color-border);
}
.category-table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  font-size: 14px;
  color: var(--color-text);
  vertical-align: middle;
}
.category-table tbody tr:last-child td { border-bottom: none; }
.category-table tbody tr:hover { background: var(--color-surface-alt, #f8f9fa); }

.td-name-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tree-toggle {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-radius: 4px;
  transition: transform 0.15s ease;
}
.tree-toggle:hover { background: rgba(46, 134, 193, 0.1); }
.tree-toggle--open { transform: rotate(90deg); }
.tree-toggle--empty { cursor: default; }
.td-name { font-weight: 600; }
.td-slug { color: var(--color-text-secondary); font-family: monospace; font-size: 13px; }
.col-count { text-align: center; }
.badge-active {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: #e8f8f0;
  color: #1f9d55;
}
.badge-inactive {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: #f1f2f3;
  color: var(--color-text-secondary);
}

.action-buttons { display: flex; gap: 6px; }
.btn-action {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  cursor: pointer;
  transition: all 0.15s;
}
.btn-edit { color: var(--color-primary); }
.btn-edit:hover { background: rgba(46, 134, 193, 0.1); border-color: var(--color-primary); }
.btn-delete { color: var(--color-danger); }
.btn-delete:hover { background: rgba(231, 76, 60, 0.08); border-color: var(--color-danger); }

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 100;
}
.form-card {
  width: 100%;
  max-width: 560px;
  background: var(--color-surface, #fff);
  border-radius: var(--radius-lg, 12px);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}
.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid var(--color-border);
}
.form-title { font-size: 17px; font-weight: 700; color: var(--color-text); }
.form-close {
  font-size: 22px;
  line-height: 1;
  color: var(--color-text-secondary);
  background: none;
  border: none;
  cursor: pointer;
}
.form-body { padding: 20px 24px 24px; }
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-field--full { grid-column: 1 / -1; }
.form-label { font-size: 13px; font-weight: 600; color: var(--color-text-secondary); }
.form-label em { color: var(--color-danger); font-style: normal; }
.form-input {
  padding: 9px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-surface, #fff);
  color: var(--color-text);
  width: 100%;
}
.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(46, 134, 193, 0.15);
}
.form-textarea { resize: vertical; }
.form-check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-text);
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
.btn-cancel-confirm {
  padding: 9px 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 14px;
  cursor: pointer;
}
.btn-save-confirm {
  padding: 9px 18px;
  border-radius: var(--radius-md);
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.btn-save-confirm:disabled { opacity: 0.6; cursor: not-allowed; }

.confirm-card {
  width: 100%;
  max-width: 420px;
  background: var(--color-surface, #fff);
  border-radius: var(--radius-lg, 12px);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
  padding: 28px 28px 24px;
  text-align: center;
}
.confirm-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fef2f2;
  color: var(--color-danger);
}
.confirm-title { font-size: 17px; font-weight: 700; color: var(--color-text); margin-bottom: 8px; }
.confirm-text { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 20px; }
.confirm-actions { display: flex; gap: 10px; justify-content: center; }
.btn-delete-confirm {
  padding: 9px 18px;
  border-radius: var(--radius-md);
  border: none;
  background: var(--color-danger);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.btn-delete-confirm:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
