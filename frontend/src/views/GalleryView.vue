<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useImageStore } from '@/stores/imageStore'
import { storeToRefs } from 'pinia'
import ImageGrid from '@/components/ImageGrid.vue'
import SearchBar from '@/components/SearchBar.vue'
import BatchActions from '@/components/BatchActions.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const imageStore = useImageStore()
const { images, loading, selectedIds, pagination } = storeToRefs(imageStore)

const allSelected = computed(() =>
  images.value.length > 0 && images.value.every(i => selectedIds.value.has(i.id))
)

function toggleSelectAll() {
  if (allSelected.value) imageStore.clearSelection()
  else imageStore.selectAll()
}

onMounted(() => {
  imageStore.fetchImages()
})

function onImageClick(id: number) {
  router.push(`/image/${id}`)
}

async function onFavorite(id: number) {
  await imageStore.toggleFavorite(id)
}

async function onDelete(id: number) {
  try {
    await ElMessageBox.confirm('Delete this image?', 'Confirm', {
      confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning',
    })
  } catch {
    return // cancelled
  }
  try {
    await imageStore.deleteImage(id)
  } catch (e: any) {
    ElMessage.error('Delete failed: ' + (e.response?.data?.message || e.message || 'Unknown error'))
  }
}

function onPageChange(page: number) {
  imageStore.setPage(page - 1)
}

function onSortChange(sort: string) {
  imageStore.filters.sort = sort
  imageStore.fetchImages()
}
</script>

<template>
  <div class="gallery-view">
    <div class="toolbar">
      <SearchBar />
      <div class="toolbar-right">
        <el-checkbox
          v-if="images.length > 0"
          :model-value="allSelected"
          :indeterminate="selectedIds.size > 0 && !allSelected"
          @change="toggleSelectAll"
          size="default"
        >
          {{ selectedIds.size > 0 ? `${selectedIds.size} selected` : 'Select All' }}
        </el-checkbox>
        <el-select
          :model-value="imageStore.filters.sort"
          @change="onSortChange"
          size="small"
          style="width: 150px"
        >
          <el-option label="Newest first" value="createdAt,desc" />
          <el-option label="Oldest first" value="createdAt,asc" />
          <el-option label="Name A-Z" value="originalFilename,asc" />
          <el-option label="Name Z-A" value="originalFilename,desc" />
        </el-select>
      </div>
    </div>

    <BatchActions />

    <ImageGrid
      :images="images"
      :loading="loading"
      :selectable="true"
      :selected-ids="selectedIds"
      @image-click="onImageClick"
      @select="imageStore.toggleSelection"
      @favorite="onFavorite"
      @delete="onDelete"
    />

    <div class="pagination-wrapper" v-if="pagination.totalPages > 1">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="pagination.totalElements"
        :page-size="pagination.size"
        :current-page="pagination.page + 1"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.gallery-view { max-width: 1600px; margin: 0 auto; }
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}
.toolbar-right { display: flex; gap: 8px; align-items: center; }
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}
</style>
