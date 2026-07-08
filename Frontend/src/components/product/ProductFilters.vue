<script setup>
import { computed } from 'vue'

const props = defineProps({
  categories: {
    type: Array,
    default: () => [],
  },
  modelValue: {
    type: Object,
    default: () => ({
      keyword: '',
      category: '',
      minPrice: null,
      maxPrice: null,
    }),
  },
})

const emit = defineEmits(['update:modelValue', 'search', 'clear'])

const localFilters = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

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

function selectCategory(cat) {
  const newFilters = {
    ...localFilters.value,
    category: localFilters.value.category === cat ? '' : cat,
  }
  localFilters.value = newFilters
  emit('search', newFilters)
}

function updatePrice() {
  emit('search', { ...localFilters.value })
}

function onClear() {
  const cleared = { keyword: '', category: '', minPrice: null, maxPrice: null }
  localFilters.value = cleared
  emit('clear', cleared)
}
</script>

<template>
  <aside class="filters-panel">
    <h3 class="filters-title">Filters</h3>

    <!-- Categories -->
    <div class="filter-section">
      <h4 class="filter-label">Category</h4>
      <div class="category-chips">
        <button
          v-for="cat in categories"
          :key="cat"
          class="chip"
          :class="{ active: modelValue.category === cat }"
          @click="selectCategory(cat)"
        >
          {{ categoryLabel(cat) }}
        </button>
      </div>
    </div>

    <!-- Price Range -->
    <div class="filter-section">
      <h4 class="filter-label">Price Range</h4>
      <div class="price-inputs">
        <div class="price-field">
          <label for="minPrice">Min</label>
          <input
            id="minPrice"
            v-model.number="localFilters.minPrice"
            type="number"
            min="0"
            step="1"
            placeholder="$0"
            @change="updatePrice"
          />
        </div>
        <span class="price-separator">—</span>
        <div class="price-field">
          <label for="maxPrice">Max</label>
          <input
            id="maxPrice"
            v-model.number="localFilters.maxPrice"
            type="number"
            min="0"
            step="1"
            placeholder="$999"
            @change="updatePrice"
          />
        </div>
      </div>
    </div>

    <!-- Clear -->
    <button class="btn-clear-filters" @click="onClear">
      Clear All Filters
    </button>
  </aside>
</template>

<style scoped>
.filters-panel {
  width: 250px;
  flex-shrink: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  height: fit-content;
  position: sticky;
  top: 20px;
}

.filters-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.filter-section {
  margin-bottom: 20px;
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.category-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  padding: 6px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  transition: all 0.15s;
}
.chip:hover {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
}
.chip.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.price-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.price-field label {
  font-size: 11px;
  color: var(--color-text-muted);
  font-weight: 500;
}
.price-field input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  background: var(--color-bg);
  transition: border-color 0.15s;
}
.price-field input:focus {
  outline: none;
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.price-separator {
  color: var(--color-text-muted);
  font-size: 14px;
  margin-top: 18px;
}

.btn-clear-filters {
  width: 100%;
  padding: 10px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  transition: all 0.15s;
}
.btn-clear-filters:hover {
  color: var(--color-danger);
  border-color: var(--color-danger);
  background: #fef2f2;
}
</style>
