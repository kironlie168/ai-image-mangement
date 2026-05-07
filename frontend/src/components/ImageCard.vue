<script setup lang="ts">
import type { ImageDto } from '@/types/image'
import { formatFileSize } from '@/utils/format'

const props = defineProps<{
  image: ImageDto
  selected?: boolean
  selectable?: boolean
}>()

const emit = defineEmits<{
  click: []
  'toggle-select': []
  favorite: []
  delete: []
}>()
</script>

<template>
  <div
    class="image-card"
    :class="{ selected }"
    @click.self="emit('click')"
  >
    <el-checkbox
      v-if="selectable"
      class="select-checkbox"
      :model-value="selected"
      @click.stop="emit('toggle-select')"
    />
    <div class="card-thumb">
      <el-image
        :src="image.thumbnailUrl"
        :alt="image.originalFilename"
        fit="cover"
        loading="lazy"
        @click="emit('click')"
      >
        <template #error>
          <div class="img-error">?</div>
        </template>
      </el-image>
    </div>
    <div class="card-footer">
      <div class="card-filename" :title="image.originalFilename">
        {{ image.originalFilename }}
      </div>
      <div class="card-meta">
        <span v-if="image.width">{{ image.width }}x{{ image.height }}</span>
        <span v-if="image.fileSize">{{ formatFileSize(image.fileSize) }}</span>
      </div>
      <div class="card-actions">
        <el-button
          :type="image.isFavorite ? 'warning' : 'default'"
          :icon="image.isFavorite ? 'StarFilled' : 'Star'"
          size="small"
          circle
          @click.stop="emit('favorite')"
        />
        <el-button
          type="danger"
          icon="Delete"
          size="small"
          circle
          @click.stop="emit('delete')"
        />
      </div>
      <div v-if="image.tags.length" class="card-tags">
        <el-tag
          v-for="tag in image.tags.slice(0, 3)"
          :key="tag.id"
          :color="tag.color"
          size="small"
          effect="dark"
          style="font-size: 10px; height: 20px; line-height: 20px; padding: 0 4px"
        >
          {{ tag.name }}
        </el-tag>
        <span v-if="image.tags.length > 3" style="font-size: 10px; color: #999">
          +{{ image.tags.length - 3 }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.image-card {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  border: 2px solid transparent;
  transition: border-color 0.2s, box-shadow 0.2s;
  cursor: pointer;
}
.image-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.image-card.selected { border-color: #409eff; }
.select-checkbox { position: absolute; top: 8px; left: 8px; z-index: 2; }
.card-thumb { width: 100%; aspect-ratio: 1; overflow: hidden; background: #f0f0f0; }
.card-thumb :deep(.el-image) { width: 100%; height: 100%; }
.card-thumb :deep(.el-image__inner) { transition: transform 0.3s; }
.image-card:hover .card-thumb :deep(.el-image__inner) { transform: scale(1.05); }
.img-error { display: flex; align-items: center; justify-content: center; height: 100%; font-size: 32px; color: #ccc; }
.card-footer { padding: 8px 10px; }
.card-filename { font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-meta { font-size: 11px; color: #999; display: flex; gap: 8px; margin-top: 2px; }
.card-actions { display: flex; gap: 4px; margin-top: 6px; }
.card-tags { display: flex; flex-wrap: wrap; gap: 3px; margin-top: 4px; }
</style>
