<script setup lang="ts">
import { ref, watch } from 'vue'
import { useImageStore } from '@/stores/imageStore'
import { storeToRefs } from 'pinia'

const imageStore = useImageStore()
const { filters } = storeToRefs(imageStore)

const searchText = ref(filters.value.keyword)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(searchText, (val) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    imageStore.setFilters({ keyword: val })
  }, 300)
})

function clearFilters() {
  imageStore.setFilters({ keyword: '', folderId: null, favorite: null, tagIds: [] })
}

const hasActiveFilters = () => filters.value.keyword || filters.value.folderId || filters.value.favorite || filters.value.tagIds.length
</script>

<template>
  <div class="search-bar">
    <el-input
      v-model="searchText"
      placeholder="Search by prompt, model, filename..."
      clearable
      prefix-icon="Search"
      size="large"
      style="max-width: 500px"
    />
    <el-button
      v-if="hasActiveFilters()"
      size="small"
      @click="clearFilters"
    >
      Clear Filters
    </el-button>
  </div>
</template>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
