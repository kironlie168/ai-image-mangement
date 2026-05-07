<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useTagStore } from '@/stores/tagStore'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { textColor, hexToRgb } from '@/utils/color'

const tagStore = useTagStore()
const { tags, loading } = storeToRefs(tagStore)

const searchQuery = ref('')
const dialogVisible = ref(false)
const editingTag = ref<{ id?: number; name: string; color: string }>({ name: '', color: '#6366f1' })
const isEdit = ref(false)

const filteredTags = computed(() => {
  if (!searchQuery.value.trim()) return tags.value
  const q = searchQuery.value.trim().toLowerCase()
  return tags.value.filter(t => t.name.toLowerCase().includes(q))
})

onMounted(() => tagStore.fetchTags())

function openCreate() {
  isEdit.value = false
  editingTag.value = { name: '', color: '#6366f1' }
  dialogVisible.value = true
}

function openEdit(tag: { id: number; name: string; color: string }) {
  isEdit.value = true
  editingTag.value = { ...tag }
  dialogVisible.value = true
}

async function saveTag() {
  if (!editingTag.value.name.trim()) {
    ElMessage.warning('Tag name is required')
    return
  }
  try {
    if (isEdit.value && editingTag.value.id) {
      await tagStore.updateTag(editingTag.value.id, editingTag.value.name, editingTag.value.color)
      ElMessage.success('Tag updated')
    } else {
      await tagStore.createTag(editingTag.value.name, editingTag.value.color)
      ElMessage.success('Tag created')
    }
    dialogVisible.value = false
  } catch { /* handled by interceptor */ }
}

async function deleteTag(id: number, name: string) {
  try {
    await ElMessageBox.confirm(`Delete tag "${name}"?`, 'Confirm', {
      confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning',
    })
    await tagStore.deleteTag(id)
    ElMessage.success('Tag deleted')
  } catch { /* cancelled */ }
}

</script>

<template>
  <div class="tag-view">
    <PageHeader title="Tag Management" subtitle="Organize your images with labels and colors" />

    <!-- Toolbar -->
    <div class="toolbar">
      <el-input
        v-model="searchQuery"
        placeholder="Search tags..."
        :prefix-icon="Search"
        class="search-input"
        clearable
      />
      <el-button type="primary" size="large" :icon="Plus" @click="openCreate">
        Create Tag
      </el-button>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="loading-grid">
      <div v-for="n in 6" :key="n" class="card-skeleton">
        <div class="skeleton-bar skeleton-color" />
        <div class="skeleton-bar skeleton-name" />
        <div class="skeleton-bar skeleton-hex" />
      </div>
    </div>

    <!-- Tag grid -->
    <div v-else-if="filteredTags.length" class="tag-grid">
      <div
        v-for="tag in filteredTags"
        :key="tag.id"
        class="tag-card"
        :style="{
          '--tag-color': tag.color,
          '--tag-color-rgb': hexToRgb(tag.color),
        }"
      >
        <div class="tag-card-accent" :style="{ background: tag.color }" />
        <div class="tag-card-body">
          <div class="tag-card-top">
            <span class="tag-card-name" :style="{ color: tag.color }">{{ tag.name }}</span>
            <div class="tag-card-actions">
              <el-tooltip content="Edit" placement="top">
                <el-button text size="small" :icon="Edit" @click="openEdit(tag)" />
              </el-tooltip>
              <el-tooltip content="Delete" placement="top">
                <el-button text size="small" :icon="Delete" @click="deleteTag(tag.id, tag.name)" />
              </el-tooltip>
            </div>
          </div>
          <div class="tag-card-color-row">
            <span class="tag-card-hex">{{ tag.color }}</span>
            <span class="tag-card-swatch" :style="{ background: tag.color }" />
          </div>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <el-empty v-else-if="!loading" :description="searchQuery ? 'No tags match your search' : 'No tags yet'">
      <el-button v-if="!searchQuery" type="primary" :icon="Plus" @click="openCreate">
        Create First Tag
      </el-button>
      <el-button v-else text @click="searchQuery = ''">Clear Search</el-button>
    </el-empty>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'Edit Tag' : 'Create Tag'"
      width="420px"
      :close-on-click-modal="false"
    >
      <div class="dialog-preview" v-if="editingTag.color">
        <span class="dialog-preview-tag" :style="{ background: editingTag.color, color: textColor(editingTag.color) }">
          {{ editingTag.name || 'Tag Preview' }}
        </span>
      </div>

      <el-form label-position="top" class="dialog-form">
        <el-form-item label="Name">
          <el-input
            v-model="editingTag.name"
            placeholder="Enter tag name"
            maxlength="50"
            show-word-limit
            clearable
          />
        </el-form-item>
        <el-form-item label="Color">
          <div class="color-picker-row">
            <el-color-picker
              v-model="editingTag.color"
              show-alpha
              :predefine="[
                '#6366f1', '#a855f7', '#ec4899', '#f43f5e', '#f97316',
                '#f59e0b', '#84cc16', '#10b981', '#0ea5e9', '#8b5cf6',
                '#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399',
              ]"
              size="large"
            />
            <span class="color-hex-label">{{ editingTag.color }}</span>
          </div>
          <div class="color-presets">
            <button
              v-for="c in ['#6366f1', '#a855f7', '#ec4899', '#f43f5e', '#f97316', '#f59e0b', '#84cc16', '#10b981', '#0ea5e9', '#8b5cf6']"
              :key="c"
              class="color-dot"
              :class="{ active: editingTag.color === c }"
              :style="{ background: c }"
              @click="editingTag.color = c"
            />
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="saveTag">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ============ Layout ============ */
.tag-view {
  max-width: 960px;
  margin: 0 auto;
  padding: 8px 0;
}

/* ============ Toolbar ============ */
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  align-items: center;
}

.search-input {
  flex: 1;
  max-width: 320px;
}

/* ============ Loading skeleton ============ */
.loading-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.card-skeleton {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8ecf1;
  padding: 24px;
}

.skeleton-bar {
  height: 14px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 6px;
}

.skeleton-color {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  margin-bottom: 16px;
}

.skeleton-name {
  width: 60%;
  margin-bottom: 10px;
}

.skeleton-hex {
  width: 35%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ============ Tag Grid ============ */
.tag-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.tag-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8ecf1;
  display: flex;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.tag-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.07);
}

.tag-card-accent {
  width: 5px;
  flex-shrink: 0;
}

.tag-card-body {
  flex: 1;
  padding: 18px 20px;
  min-width: 0;
}

.tag-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.tag-card-name {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.tag-card-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.tag-card:hover .tag-card-actions {
  opacity: 1;
}

.tag-card-color-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-card-hex {
  font-size: 12px;
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
  color: #94a3b8;
  text-transform: uppercase;
}

.tag-card-swatch {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

/* ============ Dialog ============ */
.dialog-preview {
  text-align: center;
  margin-bottom: 20px;
}

.dialog-preview-tag {
  display: inline-block;
  padding: 6px 18px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.dialog-form {
  padding: 0;
}

.color-picker-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-hex-label {
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
  color: #64748b;
}

.color-presets {
  display: flex;
  gap: 6px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.color-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
  padding: 0;
  outline: none;
}

.color-dot:hover {
  transform: scale(1.15);
}

.color-dot.active {
  border-color: #1e293b;
  box-shadow: 0 0 0 2px #fff, 0 0 0 4px currentColor;
}

/* ============ Responsive ============ */
@media (max-width: 640px) {
  .tag-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    max-width: 100%;
  }
}
</style>
