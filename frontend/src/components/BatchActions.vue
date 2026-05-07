<script setup lang="ts">
import { ref, computed } from 'vue'
import { useImageStore } from '@/stores/imageStore'
import { storeToRefs } from 'pinia'
import TagSelector from './TagSelector.vue'
import { ElMessageBox, ElMessage } from 'element-plus'

const imageStore = useImageStore()
const { selectedIds, images } = storeToRefs(imageStore)

const showTagDialog = ref(false)
const newTagIds = ref<number[]>([])
const processing = ref(false)

// Compute common tags across all selected images
const commonTags = computed(() => {
  const ids = Array.from(selectedIds.value)
  if (!ids.length) return []
  const selectedImages = images.value.filter((i) => ids.includes(i.id))
  if (!selectedImages.length) return []
  const tagSets = selectedImages.map((i) => new Set(i.tags.map((t) => t.id)))
  const common = tagSets.reduce((acc, set) => {
    return new Set([...acc].filter((id) => set.has(id)))
  })
  return selectedImages[0].tags.filter((t) => common.has(t.id))
})

async function confirmDelete() {
  try {
    await ElMessageBox.confirm(
      `Delete ${selectedIds.value.size} images? This cannot be undone.`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await imageStore.batchDelete()
  } catch { /* cancelled */ }
}

async function applyBatchTag() {
  if (!newTagIds.value.length) return
  processing.value = true
  try {
    await imageStore.batchTagImages(newTagIds.value)
    newTagIds.value = []
    ElMessage.success('Tags added')
  } finally {
    processing.value = false
  }
}

async function removeTag(tagId: number) {
  processing.value = true
  try {
    await imageStore.batchRemoveTagsImages([tagId])
    ElMessage.success('Tag removed')
  } finally {
    processing.value = false
  }
}

function openTagDialog() {
  newTagIds.value = []
  showTagDialog.value = true
}

function closeDialog() {
  showTagDialog.value = false
  newTagIds.value = []
}
</script>

<template>
  <div v-if="selectedIds.size > 0" class="batch-bar">
    <span class="batch-count">{{ selectedIds.size }} selected</span>
    <el-button size="small" @click="imageStore.clearSelection()">Deselect All</el-button>
    <el-button size="small" type="primary" @click="openTagDialog">Manage Tags</el-button>
    <el-button size="small" type="danger" @click="confirmDelete">Delete Selected</el-button>
  </div>

  <el-dialog
    v-model="showTagDialog"
    title="Manage Tags"
    width="420px"
    :close-on-click-modal="false"
    @open="openTagDialog"
  >
    <!-- Common tags on selected images -->
    <div v-if="commonTags.length" class="dialog-section">
      <label class="section-label">Current Tags</label>
      <div class="tag-list">
        <el-tag
          v-for="tag in commonTags"
          :key="tag.id"
          :color="tag.color"
          effect="dark"
          closable
          :disabled="processing"
          @close="removeTag(tag.id)"
        >
          {{ tag.name }}
        </el-tag>
      </div>
    </div>
    <el-empty v-else description="No common tags across selected images" :image-size="40" />

    <!-- Add new tags -->
    <div class="dialog-section">
      <label class="section-label">Add Tags</label>
      <TagSelector v-model="newTagIds" />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <span class="selected-info">{{ selectedIds.size }} images selected</span>
        <div>
          <el-button @click="closeDialog">Done</el-button>
          <el-button
            type="primary"
            @click="applyBatchTag"
            :disabled="!newTagIds.length"
            :loading="processing"
          >
            Apply
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #ecf5ff;
  border-radius: 8px;
  margin-bottom: 12px;
}
.batch-count { font-size: 14px; font-weight: 600; color: #409eff; }
.dialog-section { margin-bottom: 16px; }
.section-label {
  display: block;
  font-size: 12px;
  color: #909399;
  text-transform: uppercase;
  margin-bottom: 8px;
}
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.selected-info { font-size: 13px; color: #909399; }
</style>
