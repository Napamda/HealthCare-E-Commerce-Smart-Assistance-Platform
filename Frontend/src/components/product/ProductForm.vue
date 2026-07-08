<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  product: {
    type: Object,
    default: null,
  },
  categories: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['save', 'close'])

const isEditing = computed(() => !!props.product)

const form = ref({
  name: '',
  description: '',
  price: '',
  category: '',
  imageUrl: '',
  stockQuantity: 0,
  manufacturer: '',
  dosage: '',
  ingredients: '',
  prescriptionRequired: false,
  sideEffects: '',
})

const validationError = ref('')
const isSubmitting = ref(false)

watch(
  () => props.product,
  (p) => {
    if (p) {
      form.value = {
        name: p.name || '',
        description: p.description || '',
        price: p.price || '',
        category: p.category || '',
        imageUrl: p.imageUrl || '',
        stockQuantity: p.stockQuantity ?? 0,
        manufacturer: p.manufacturer || '',
        dosage: p.dosage || '',
        ingredients: p.ingredients || '',
        prescriptionRequired: p.prescriptionRequired ?? false,
        sideEffects: p.sideEffects || '',
      }
    } else {
      resetForm()
    }
  },
  { immediate: true },
)

function resetForm() {
  form.value = {
    name: '',
    description: '',
    price: '',
    category: '',
    imageUrl: '',
    stockQuantity: 0,
    manufacturer: '',
    dosage: '',
    ingredients: '',
    prescriptionRequired: false,
    sideEffects: '',
  }
}

function categoryLabel(cat) {
  const labels = {
    VITAMINS: 'Vitamins',
    PAIN_RELIEF: 'Pain Relief',
    SKIN_CARE: 'Skin Care',
    DIGESTIVE_HEALTH: 'Digestive Health',
    RESPIRATORY: 'Respiratory',
    HEART_HEALTH: 'Heart Health',
    DIABETES_CARE: 'Diabetes Care',
    FIRST_AID: 'First Aid',
    MEDICAL_DEVICES: 'Medical Devices',
    PERSONAL_CARE: 'Personal Care',
    WELLNESS: 'Wellness',
    BABY_CARE: 'Baby Care',
    ELDERLY_CARE: 'Elderly Care',
    OTHER: 'Other',
  }
  return labels[cat] || cat
}

function validate() {
  if (!form.value.name.trim()) {
    validationError.value = 'Product name is required.'
    return false
  }
  if (!form.value.price || Number(form.value.price) <= 0) {
    validationError.value = 'Price must be greater than zero.'
    return false
  }
  if (!form.value.category) {
    validationError.value = 'Category is required.'
    return false
  }
  validationError.value = ''
  return true
}

async function onSubmit() {
  if (!validate()) return

  isSubmitting.value = true
  try {
    const payload = {
      ...form.value,
      price: Number(form.value.price),
      stockQuantity: Number(form.value.stockQuantity),
    }
    emit('save', payload)
  } finally {
    isSubmitting.value = false
  }
}

function onCancel() {
  emit('close')
}
</script>

<template>
  <div class="dialog-overlay" @click.self="onCancel">
    <div class="dialog-card">
      <div class="dialog-header">
        <h3 class="dialog-title">{{ isEditing ? 'Edit Product' : 'Add New Product' }}</h3>
        <button class="btn-close" @click="onCancel" title="Close">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div class="dialog-body">
        <div v-if="validationError" class="validation-error">{{ validationError }}</div>

        <form @submit.prevent="onSubmit" class="product-form">
          <div class="form-row">
            <div class="form-group full-width">
              <label for="prod-name">Name *</label>
              <input id="prod-name" v-model="form.name" type="text" placeholder="Product name" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="prod-price">Price ($) *</label>
              <input id="prod-price" v-model="form.price" type="number" step="0.01" min="0" placeholder="0.00" />
            </div>
            <div class="form-group">
              <label for="prod-category">Category *</label>
              <select id="prod-category" v-model="form.category">
                <option value="" disabled>Select category</option>
                <option v-for="cat in categories" :key="cat" :value="cat">{{ categoryLabel(cat) }}</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="prod-manufacturer">Manufacturer</label>
              <input id="prod-manufacturer" v-model="form.manufacturer" type="text" placeholder="Manufacturer" />
            </div>
            <div class="form-group">
              <label for="prod-stock">Stock Quantity</label>
              <input id="prod-stock" v-model.number="form.stockQuantity" type="number" min="0" placeholder="0" />
            </div>
          </div>

          <div class="form-group">
            <label for="prod-image">Image URL</label>
            <input id="prod-image" v-model="form.imageUrl" type="text" placeholder="https://..." />
          </div>

          <div class="form-group">
            <label for="prod-description">Description</label>
            <textarea id="prod-description" v-model="form.description" rows="3" placeholder="Product description..." />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="prod-dosage">Dosage</label>
              <input id="prod-dosage" v-model="form.dosage" type="text" placeholder="e.g., 500mg daily" />
            </div>
            <div class="form-group">
              <label for="prod-ingredients">Ingredients</label>
              <input id="prod-ingredients" v-model="form.ingredients" type="text" placeholder="Key ingredients" />
            </div>
          </div>

          <div class="form-group">
            <label for="prod-side-effects">Side Effects</label>
            <textarea id="prod-side-effects" v-model="form.sideEffects" rows="2" placeholder="Possible side effects..." />
          </div>

          <div class="form-check">
            <input id="prod-rx" v-model="form.prescriptionRequired" type="checkbox" />
            <label for="prod-rx">Prescription Required</label>
          </div>
        </form>
      </div>

      <div class="dialog-footer">
        <button class="btn-cancel" :disabled="isSubmitting" @click="onCancel">Cancel</button>
        <button class="btn-save" :disabled="isSubmitting" @click="onSubmit">
          <span v-if="isSubmitting" class="spinner" />
          <span v-else>{{ isEditing ? 'Save Changes' : 'Create Product' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.15s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.dialog-card {
  width: 100%;
  max-width: 640px;
  max-height: 90vh;
  overflow-y: auto;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.2s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
}

.btn-close {
  padding: 6px;
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  transition: background 0.15s, color 0.15s;
}
.btn-close:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

.dialog-body {
  padding: 20px 24px;
}

.validation-error {
  background: #fef2f2;
  color: var(--color-danger);
  border: 1px solid #fecaca;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  margin-bottom: 16px;
}

.product-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-group.full-width {
  grid-column: span 2;
}
.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}
.form-group input,
.form-group textarea,
.form-group select {
  padding: 9px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  background: var(--color-bg);
  transition: border-color 0.15s, box-shadow 0.15s;
}
.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
.form-group textarea {
  resize: vertical;
  min-height: 70px;
}

.form-check {
  display: flex;
  align-items: center;
  gap: 8px;
}
.form-check input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
}
.form-check label {
  font-size: 14px;
  color: var(--color-text);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg);
}

.btn-cancel {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  transition: background 0.15s;
}
.btn-cancel:hover:not(:disabled) {
  background: var(--color-surface);
}
.btn-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-save {
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
.btn-save:hover:not(:disabled) {
  background: var(--color-primary-dark);
}
.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
