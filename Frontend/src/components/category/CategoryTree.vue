<script setup>
import { ref } from 'vue'
import { categoryToFilterValue } from '../../services/category.js'

const props = defineProps({
  categories: { type: Array, default: () => [] },
  // currently selected filter value ('' = all)
  value: { type: String, default: '' },
  showAll: { type: Boolean, default: true },
  // icon map keyed by enum-style name, e.g. { VITAMINS: { label, color } }
  icons: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['select'])

const expanded = ref({})

function toggle(node) {
  if (!node.children || !node.children.length) return
  expanded.value[node.id] = !expanded.value[node.id]
}

function isExpanded(node) {
  return expanded.value[node.id] === true || descendantSelected(node)
}

// True when the active filter value lives in this node's subtree
// (so parents stay open so the selected child stays visible).
function descendantSelected(node) {
  if (!props.value) return false
  if (props.value === filterValue(node)) return true
  return (node.children || []).some(descendantSelected)
}

function filterValue(node) {
  return categoryToFilterValue(node.name)
}

function isActive(node) {
  return props.value === filterValue(node)
}

function select(node) {
  emit('select', filterValue(node))
}

function selectAll() {
  emit('select', '')
}

function iconFor(node) {
  return props.icons[categoryToFilterValue(node.name)] || props.icons[node.name?.toUpperCase()]
}
</script>

<template>
  <ul class="category-tree">
    <li v-if="showAll" class="category-node">
      <button
        class="category-node__row category-node__row--all"
        :class="{ 'category-node__row--active': value === '' }"
        @click="selectAll"
      >
        <span class="category-node__chevron category-node__chevron--all" />
        <span class="category-node__name">All Categories</span>
        <span class="category-node__count">{{ categories.length }} top</span>
      </button>
    </li>

    <li v-for="node in categories" :key="node.id" class="category-node">
      <button
        class="category-node__row"
        :class="{ 'category-node__row--active': isActive(node) }"
        @click="select(node)"
      >
        <span
          v-if="node.children && node.children.length"
          class="category-node__chevron"
          :class="{ 'category-node__chevron--open': isExpanded(node) }"
          @click.stop="toggle(node)"
        >
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </span>
        <span v-else class="category-node__chevron category-node__chevron--empty" />

        <span v-if="iconFor(node)" class="category-node__icon" :style="{ color: iconFor(node).color }">
          {{ iconFor(node).label || '' }}
        </span>

        <span class="category-node__name">{{ node.name }}</span>
        <span class="category-node__count">{{ node.productCount ?? 0 }}</span>
      </button>

      <ul v-if="node.children && node.children.length && isExpanded(node)" class="category-tree category-tree--nested">
        <CategoryTree
          :categories="node.children"
          :value="value"
          :show-all="false"
          :icons="icons"
          @select="(v) => emit('select', v)"
        />
      </ul>
    </li>
  </ul>
</template>

<style scoped>
.category-tree {
  list-style: none;
  margin: 0;
  padding: 0;
}

.category-tree--nested {
  padding-left: 22px;
}

.category-node__row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 9px 12px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-color, #2d3436);
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease;
}

.category-node__row:hover {
  background: rgba(var(--color-primary-rgb), 0.08);
}

.category-node__row--active {
  background: var(--primary-color, #2e86c1);
  color: #fff;
  font-weight: 600;
}

.category-node__row--all {
  color: var(--primary-color, #2e86c1);
  font-weight: 600;
}

.category-node__chevron {
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  transition: transform 0.15s ease;
}

.category-node__chevron--open {
  transform: rotate(90deg);
}

.category-node__chevron--empty,
.category-node__chevron--all {
  background: transparent;
}

.category-node__icon {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 700;
  background: rgba(var(--color-primary-rgb), 0.1);
}

.category-node__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-node__count {
  flex-shrink: 0;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(var(--color-primary-rgb), 0.12);
  color: var(--primary-color, #2e86c1);
}

.category-node__row--active .category-node__count {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
}
</style>
