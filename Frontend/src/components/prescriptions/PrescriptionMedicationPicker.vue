<script setup>
import { ref, watch } from 'vue'
import { searchProducts } from '../../services/product.js'

const props = defineProps({
  // v-model: array of { productId, productName, quantity, dosageInstructions }
  modelValue: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue'])

const searchTerm = ref('')
const results = ref([])
const searching = ref(false)
const searchError = ref('')
let searchTimer = null

watch(searchTerm, (val) => {
  clearTimeout(searchTimer)
  if (!val || val.trim().length < 2) {
    results.value = []
    searchError.value = ''
    return
  }
  searchTimer = setTimeout(doSearch, 300)
})

async function doSearch() {
  searching.value = true
  searchError.value = ''
  try {
    const data = await searchProducts({ keyword: searchTerm.value.trim(), size: 8 })
    results.value = data.content || data.items || data || []
  } catch (e) {
    searchError.value = 'Could not search products'
    results.value = []
  } finally {
    searching.value = false
  }
}

function isSelected(productId) {
  return props.modelValue.some((i) => i.productId === productId)
}

function addProduct(product) {
  if (isSelected(product.id)) return
  const next = [
    ...props.modelValue,
    {
      productId: product.id,
      productName: product.name,
      quantity: 1,
      dosageInstructions: '',
    },
  ]
  emit('update:modelValue', next)
}

function removeProduct(productId) {
  emit('update:modelValue', props.modelValue.filter((i) => i.productId !== productId))
}

function updateItem(productId, field, value) {
  const next = props.modelValue.map((i) =>
    i.productId === productId ? { ...i, [field]: value } : i,
  )
  emit('update:modelValue', next)
}

function formatPrice(price) {
  return price == null ? '$0.00' : '$' + Number(price).toFixed(2)
}
</script>

<template>
  <div class="med-picker">
    <label class="med-picker-label">Select medications for this prescription</label>

    <!-- Search box -->
    <div class="med-search">
      <input
        v-model="searchTerm"
        type="text"
        class="med-search-input"
        placeholder="Search the catalog by medication name..."
      />
      <span v-if="searching" class="med-search-spinner">...</span>
    </div>
    <p v-if="searchError" class="med-search-error">{{ searchError }}</p>

    <!-- Search results -->
    <div v-if="results.length" class="med-results">
      <div
        v-for="product in results"
        :key="product.id"
        class="med-result-item"
        :class="{ disabled: isSelected(product.id) }"
      >
        <div class="med-result-info">
          <span class="med-result-name">{{ product.name }}</span>
          <span class="med-result-meta">
            {{ formatPrice(product.price) }}
            <span v-if="product.prescriptionRequired" class="rx-tag">Rx</span>
          </span>
        </div>
        <button
          type="button"
          class="btn-add-med"
          :disabled="isSelected(product.id)"
          @click="addProduct(product)"
        >
          {{ isSelected(product.id) ? 'Added' : '+ Add' }}
        </button>
      </div>
    </div>

    <!-- Selected medications -->
    <div v-if="modelValue.length" class="med-selected">
      <h4 class="med-selected-title">Selected medications ({{ modelValue.length }})</h4>
      <div v-for="item in modelValue" :key="item.productId" class="med-selected-row">
        <div class="med-selected-main">
          <span class="med-selected-name">{{ item.productName }}</span>
          <div class="med-selected-controls">
            <label class="med-qty">
              Qty
              <input
                type="number"
                min="1"
                max="99"
                class="med-qty-input"
                :value="item.quantity"
                @input="updateItem(item.productId, 'quantity', Math.max(1, Number($event.target.value) || 1))"
              />
            </label>
            <input
              type="text"
              class="med-dosage-input"
              placeholder="Dosage / instructions (e.g. 1 tablet twice daily)"
              :value="item.dosageInstructions"
              @input="updateItem(item.productId, 'dosageInstructions', $event.target.value)"
            />
          </div>
        </div>
        <button type="button" class="btn-remove-med" @click="removeProduct(item.productId)">
          &times;
        </button>
      </div>
    </div>
    <p v-else class="med-empty-hint">
      No medications selected. The patient will not be able to order specific meds from this approval.
    </p>
  </div>
</template>
