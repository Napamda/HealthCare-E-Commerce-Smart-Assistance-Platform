<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  // Array of { id, url, thumbnailUrl, primary, sortOrder }
  images: { type: Array, default: () => [] },
  // Fallback shown when there are no uploaded images
  fallbackUrl: { type: String, default: '' },
  alt: { type: String, default: 'Product image' },
})

const activeIndex = ref(0)

// Primary first, then by sortOrder, then by id
const sortedImages = computed(() =>
  [...props.images].sort((a, b) => {
    if (!!a.primary !== !!b.primary) return a.primary ? -1 : 1
    return (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || (a.id ?? 0) - (b.id ?? 0)
  }),
)

const activeImage = computed(() => sortedImages.value[activeIndex.value] || null)

watch(
  () => props.images.length,
  () => {
    activeIndex.value = 0
  },
)

function selectIndex(i) {
  activeIndex.value = i
}
</script>

<template>
  <div class="gallery">
    <!-- Main image -->
    <div class="gallery-main">
      <img
        v-if="activeImage"
        :src="activeImage.url"
        :alt="alt"
        class="gallery-main__img"
      />
      <img
        v-else-if="fallbackUrl"
        :src="fallbackUrl"
        :alt="alt"
        class="gallery-main__img"
      />
      <div v-else class="gallery-main__placeholder">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
      </div>
    </div>

    <!-- Thumbnails -->
    <div v-if="sortedImages.length > 1" class="gallery-thumbs">
      <button
        v-for="(img, i) in sortedImages"
        :key="img.id ?? i"
        class="gallery-thumb"
        :class="{ 'gallery-thumb--active': i === activeIndex }"
        :title="img.primary ? 'Primary image' : ''"
        @click="selectIndex(i)"
      >
        <img :src="img.thumbnailUrl || img.url" :alt="`${alt} ${i + 1}`" />
        <span v-if="img.primary" class="gallery-thumb__badge">★</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.gallery {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.gallery-main {
  width: 100%;
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.gallery-main__img {
  width: 100%;
  height: 100%;
  max-height: 420px;
  object-fit: contain;
}

.gallery-main__placeholder {
  color: var(--color-text-muted);
  opacity: 0.5;
  padding: 80px 0;
}

.gallery-thumbs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.gallery-thumb {
  position: relative;
  width: 64px;
  height: 64px;
  padding: 0;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  background: var(--color-surface);
  transition: border-color 0.15s, transform 0.15s;
}

.gallery-thumb:hover {
  border-color: var(--color-primary-light);
  transform: translateY(-1px);
}

.gallery-thumb--active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.gallery-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery-thumb__badge {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}
</style>
