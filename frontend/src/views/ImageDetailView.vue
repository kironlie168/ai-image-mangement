<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useImageStore } from '@/stores/imageStore'
import { useTagStore } from '@/stores/tagStore'
import { storeToRefs } from 'pinia'
import TagSelector from '@/components/TagSelector.vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Download, Document } from '@element-plus/icons-vue'
import { downloadImage, downloadWorkflow } from '@/api/imageApi'

const route = useRoute()
const router = useRouter()
const imageStore = useImageStore()
const tagStore = useTagStore()

const { currentImage } = storeToRefs(imageStore)
const { tags } = storeToRefs(tagStore)

const imageId = computed(() => Number(route.params.id))
const loading = ref(true)
const notFound = ref(false)
const editing = ref(false)
const editForm = ref({ prompt: '', negativePrompt: '', model: '', steps: 0, cfgScale: 0, seed: 0, sampler: '' })

onMounted(async () => {
  try {
    await imageStore.fetchImageDetail(imageId.value)
    if (!currentImage.value) { notFound.value = true; return }
    initEditForm()
  } catch { notFound.value = true }
  finally { loading.value = false }
  if (!tags.value.length) tagStore.fetchTags()
})

function initEditForm() {
  if (!currentImage.value) return
  editForm.value = {
    prompt: currentImage.value.prompt || '',
    negativePrompt: currentImage.value.negativePrompt || '',
    model: currentImage.value.model || '',
    steps: currentImage.value.steps || 0,
    cfgScale: currentImage.value.cfgScale || 0,
    seed: currentImage.value.seed || 0,
    sampler: currentImage.value.sampler || '',
  }
}

async function toggleFavorite() {
  if (currentImage.value) {
    await imageStore.toggleFavorite(currentImage.value.id)
  }
}

async function saveMetadata() {
  if (!currentImage.value) return
  await imageStore.updateImageMetadata(imageId.value, editForm.value)
  editing.value = false
  ElMessage.success('Metadata updated')
}

async function deleteImage() {
  try {
    await ElMessageBox.confirm('Delete this image?', 'Confirm', {
      confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning',
    })
    await imageStore.deleteImage(imageId.value)
    router.push('/')
  } catch { /* cancelled */ }
}

async function onTagsChange(tagIds: number[]) {
  if (!currentImage.value) return
  await imageStore.setImageTags(imageId.value, tagIds)
}

function goBack() { router.push('/') }

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).then(() => ElMessage.success('Copied!'))
}

async function handleDownloadImage() {
  if (!currentImage.value) return
  try {
    await downloadImage(currentImage.value.id)
    ElMessage.success('Download started')
  } catch {
    ElMessage.error('Download failed')
  }
}

async function handleDownloadWorkflow() {
  if (!currentImage.value) return
  try {
    await downloadWorkflow(currentImage.value.id)
    ElMessage.success('Workflow downloaded')
  } catch {
    ElMessage.error('Download failed')
  }
}

const metaFields = computed(() => {
  if (!currentImage.value) return []
  const img = currentImage.value
  return [
    { label: 'Model', value: img.model },
    { label: 'Steps', value: img.steps },
    { label: 'CFG Scale', value: img.cfgScale },
    { label: 'Seed', value: img.seed },
    { label: 'Sampler', value: img.sampler },
    { label: 'Dimensions', value: img.width && img.height ? `${img.width} x ${img.height}` : '' },
    { label: 'Format', value: img.format },
    { label: 'File Size', value: img.fileSize ? `${(img.fileSize / 1024 / 1024).toFixed(2)} MB` : '' },
    { label: 'Created', value: img.createdAt },
  ].filter((f) => f.value)
})
</script>

<template>
  <div class="detail-view">
    <el-button size="small" @click="goBack" style="margin-bottom: 16px">← Back to Gallery</el-button>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <div style="display: flex; gap: 24px;">
          <el-skeleton-item variant="image" style="width: 600px; height: 600px" />
          <div style="flex: 1">
            <el-skeleton-item variant="h1" style="width: 60%; height: 24px" />
            <el-skeleton-item variant="text" v-for="i in 8" :key="i" style="margin-top: 12px" />
          </div>
        </div>
      </template>

      <template #default>
        <div v-if="notFound" class="not-found">
          <el-result icon="warning" title="Image not found" sub-title="The image you're looking for doesn't exist.">
            <template #extra><el-button @click="goBack">Back to Gallery</el-button></template>
          </el-result>
        </div>

        <div v-else-if="currentImage" class="detail-content">
          <div class="detail-image">
            <el-image
              :src="currentImage.imageUrl"
              :alt="currentImage.originalFilename"
              fit="contain"
              style="width: 100%; height: 100%"
              :preview-src-list="[currentImage.imageUrl]"
              :hide-on-click-modal="true"
            />
          </div>

          <div class="detail-panel">
            <div class="panel-header">
              <h2>{{ currentImage.originalFilename }}</h2>
              <div class="panel-actions">
                <el-button
                  :type="currentImage.isFavorite ? 'warning' : 'default'"
                  @click="toggleFavorite"
                >
                  {{ currentImage.isFavorite ? '★ Favorited' : '☆ Favorite' }}
                </el-button>
                <el-button type="primary" @click="editing = !editing">
                  {{ editing ? 'Cancel' : 'Edit' }}
                </el-button>
                <el-button type="danger" @click="deleteImage">Delete</el-button>
                <el-divider style="margin: 8px 0" />
                <div style="display: flex; gap: 8px; flex-wrap: wrap">
                  <el-button @click="handleDownloadImage">
                    <el-icon><Download /></el-icon> Image
                  </el-button>
                  <el-button
                    v-if="currentImage.workflowJson"
                    @click="handleDownloadWorkflow"
                  >
                    <el-icon><Document /></el-icon> Workflow
                  </el-button>
                </div>
              </div>
            </div>

            <el-divider />

            <div v-if="editing" class="edit-form">
              <el-form label-position="top" size="small">
                <el-form-item label="Prompt">
                  <el-input v-model="editForm.prompt" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item label="Negative Prompt">
                  <el-input v-model="editForm.negativePrompt" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="Model">
                  <el-input v-model="editForm.model" />
                </el-form-item>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
                  <el-form-item label="Steps"><el-input-number v-model="editForm.steps" :min="0" style="width: 100%" /></el-form-item>
                  <el-form-item label="CFG Scale"><el-input-number v-model="editForm.cfgScale" :min="0" :step="0.5" style="width: 100%" /></el-form-item>
                  <el-form-item label="Seed"><el-input-number v-model="editForm.seed" :min="0" style="width: 100%" /></el-form-item>
                  <el-form-item label="Sampler"><el-input v-model="editForm.sampler" /></el-form-item>
                </div>
                <el-button type="primary" @click="saveMetadata">Save</el-button>
              </el-form>
            </div>

            <div v-else>
              <div class="meta-section">
                <h3>Prompt</h3>
                <p class="meta-text" @click="copyToClipboard(currentImage.prompt || '')">
                  {{ currentImage.prompt || '-' }}
                  <el-icon style="margin-left: 4px; cursor: pointer"><CopyDocument /></el-icon>
                </p>
              </div>

              <div v-if="currentImage.negativePrompt" class="meta-section">
                <h3>Negative Prompt</h3>
                <p class="meta-text" @click="copyToClipboard(currentImage.negativePrompt)">
                  {{ currentImage.negativePrompt }}
                </p>
              </div>

              <div class="meta-section">
                <h3>ComfyUI Workflow</h3>
                <el-tag v-if="currentImage.workflowJson" type="success" effect="dark">Embedded ✓</el-tag>
                <el-tag v-else type="info" effect="plain">Not available</el-tag>
              </div>

              <div class="meta-grid">
                <div v-for="field in metaFields" :key="field.label" class="meta-item">
                  <span class="meta-label">{{ field.label }}</span>
                  <span class="meta-value">{{ field.value }}</span>
                </div>
              </div>
            </div>

            <el-divider />

            <div class="tags-section">
              <h3>Tags</h3>
              <div class="current-tags" v-if="currentImage.tags.length">
                <el-tag
                  v-for="tag in currentImage.tags"
                  :key="tag.id"
                  :color="tag.color"
                  effect="dark"
                  closable
                  @close="onTagsChange(currentImage.tags.filter((t) => t.id !== tag.id).map((t) => t.id))"
                >
                  {{ tag.name }}
                </el-tag>
              </div>
              <TagSelector
                :model-value="currentImage.tags.map((t) => t.id)"
                @update:model-value="onTagsChange"
              />
            </div>
          </div>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped>
.detail-view {
  max-width: 1600px;
  margin: 0 auto;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding-bottom: 16px;
  box-sizing: border-box;
}
.detail-content {
  display: flex;
  gap: 24px;
  align-items: stretch;
  flex: 1;
  min-height: 0;
}
.detail-image {
  flex: 1;
  min-width: 0;
  min-height: 0;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.detail-image :deep(.el-image) {
  width: 100%;
  height: 100%;
  display: flex;
}
.detail-image :deep(.el-image__inner) {
  object-fit: contain;
  width: 100%;
  height: 100%;
}
.detail-panel {
  width: 420px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  overflow-y: auto;
}
.panel-header { display: flex; flex-direction: column; gap: 8px; }
.panel-header h2 { font-size: 16px; word-break: break-all; }
.panel-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.meta-section { margin-bottom: 16px; }
.meta-section h3 { font-size: 12px; color: #909399; margin-bottom: 4px; text-transform: uppercase; }
.meta-text { font-size: 13px; line-height: 1.5; cursor: pointer; word-break: break-word; }
.meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.meta-item { display: flex; flex-direction: column; gap: 2px; }
.meta-label { font-size: 11px; color: #909399; }
.meta-value { font-size: 13px; }
.tags-section h3 { font-size: 12px; color: #909399; margin-bottom: 8px; text-transform: uppercase; }
.current-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 8px; }
.not-found { display: flex; justify-content: center; padding: 80px 0; }
</style>
