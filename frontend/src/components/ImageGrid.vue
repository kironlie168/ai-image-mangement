<script setup lang="ts">
import type { ImageDto } from '@/types/image'
import ImageCard from './ImageCard.vue'

defineProps<{
  images: ImageDto[]
  loading: boolean
  selectable?: boolean
  selectedIds?: Set<number>
}>()

const emit = defineEmits<{
  select: [id: number]
  'image-click': [id: number]
  favorite: [id: number]
  delete: [id: number]
}>()
</script>

<template>
  <div class="image-grid" v-if="!loading && images.length">
    <ImageCard
      v-for="img in images"
      :key="img.id"
      :image="img"
      :selected="selectedIds?.has(img.id)"
      :selectable="selectable"
      @click="emit('image-click', img.id)"
      @toggle-select="emit('select', img.id)"
      @favorite="emit('favorite', img.id)"
      @delete="emit('delete', img.id)"
    />
  </div>
  <div v-else-if="loading" class="grid-loading">
    <div v-for="i in 12" :key="i" class="skeleton-card">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="image" style="width: 100%; aspect-ratio: 1" />
          <div style="padding: 8px">
            <el-skeleton-item variant="text" style="width: 80%; height: 14px" />
            <el-skeleton-item variant="text" style="width: 50%; height: 12px; margin-top: 4px" />
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
  <div v-else class="grid-empty">
    <el-empty description="No images found">
      <el-button type="primary" @click="$router.push('/upload')">Upload Images</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.grid-loading {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.skeleton-card {
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.grid-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}
</style>
