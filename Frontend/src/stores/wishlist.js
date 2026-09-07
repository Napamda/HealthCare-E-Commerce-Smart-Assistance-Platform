import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const STORAGE_KEY = 'healthcare_wishlist'

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveToStorage(ids) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(ids))
}

export const useWishlistStore = defineStore('wishlist', () => {
  const items = ref(loadFromStorage())

  const count = computed(() => items.value.length)

  function isWishlisted(productId) {
    return items.value.includes(Number(productId))
  }

  function addItem(productId) {
    const id = Number(productId)
    if (!items.value.includes(id)) {
      items.value.push(id)
      saveToStorage(items.value)
    }
  }

  function removeItem(productId) {
    const id = Number(productId)
    items.value = items.value.filter((i) => i !== id)
    saveToStorage(items.value)
  }

  async function toggleItem(productId) {
    if (isWishlisted(productId)) {
      removeItem(productId)
    } else {
      addItem(productId)
    }
  }

  function clearAll() {
    items.value = []
    saveToStorage(items.value)
  }

  return { items, count, isWishlisted, addItem, removeItem, toggleItem, clearAll }
})
