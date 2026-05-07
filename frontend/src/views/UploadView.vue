<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useImageStore } from '@/stores/imageStore'
import { ElMessage } from 'element-plus'
import { Upload, FolderOpened, Check, Close, ArrowDown } from '@element-plus/icons-vue'
import type { UploadUserFile } from 'element-plus'
import DirectoryPicker from '@/components/DirectoryPicker.vue'
import { previewMetadata } from '@/api/imageApi'
import { getStats, formatBytes } from '@/api/fileApi'
import type { StorageStats } from '@/api/fileApi'
import type { FileMetadataPreview } from '@/types/image'

const imageStore = useImageStore()

// Upload state
const fileList = ref<UploadUserFile[]>([])
const uploading = ref(false)
const uploadResults = ref<Array<{ originalFilename: string; status: string; errorMessage?: string }>>([])
const showResults = ref(false)

// Per-file metadata preview (auto-extracted after file selection)
const previewData = ref<FileMetadataPreview[]>([])
const analyzing = ref(false)

// Bulk fill metadata
const bulkPrompt = ref('')
const bulkModel = ref('')
const bulkSteps = ref(30)
const bulkCfg = ref(7)
const bulkSampler = ref('')
const metadataOpen = ref(false)

// Scan state
const scanPath = ref('')
const scanning = ref(false)
const showDirPicker = ref(false)

// Storage stats
const stats = ref<StorageStats | null>(null)

onMounted(() => loadStats())

async function loadStats() {
  try { stats.value = await getStats() } catch { /* ignore */ }
}

const hasFiles = computed(() => fileList.value.some(f => f.raw))
const fileCount = computed(() => fileList.value.filter(f => f.raw).length)
const totalSize = computed(() => {
  return fileList.value
    .filter(f => f.raw)
    .reduce((acc, f) => acc + (f.raw?.size || 0), 0)
})
const formattedSize = computed(() => {
  const bytes = totalSize.value
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
})

const resultSuccessCount = computed(() => uploadResults.value.filter(r => r.status === 'success').length)
const resultErrorCount = computed(() => uploadResults.value.filter(r => r.status === 'error').length)

// Auto-preview metadata when file selection changes
watch(fileList, async (newList) => {
  const raws = newList.filter(f => f.raw).map(f => f.raw as File)
  if (raws.length === 0) {
    previewData.value = []
    return
  }
  analyzing.value = true
  try {
    previewData.value = await previewMetadata(raws)
  } catch {
    // Silent fail — preview is best-effort
    previewData.value = []
  } finally {
    analyzing.value = false
  }
}, { deep: true })

function applyBulkToAll() {
  previewData.value = previewData.value.map(p => ({
    ...p,
    prompt: bulkPrompt.value || p.prompt,
    model: bulkModel.value || p.model,
    steps: bulkSteps.value || p.steps,
    cfgScale: bulkCfg.value || p.cfgScale,
    sampler: bulkSampler.value || p.sampler,
  }))
  ElMessage.success(`Applied bulk metadata to ${previewData.value.length} files`)
}

async function handleUpload() {
  if (!hasFiles.value) return
  uploading.value = true
  showResults.value = false

  const files = fileList.value
    .filter(f => f.raw)
    .map(f => f.raw as File)

  try {
    // Build per-file metadata list from preview data
    const metadataList = previewData.value.map(p => {
      const entry: Record<string, string | number | null> = {}
      if (p.prompt) entry.prompt = p.prompt
      if (p.negativePrompt) entry.negativePrompt = p.negativePrompt
      if (p.model) entry.model = p.model
      if (p.steps != null) entry.steps = p.steps
      if (p.cfgScale != null) entry.cfgScale = p.cfgScale
      if (p.seed != null) entry.seed = p.seed
      if (p.sampler) entry.sampler = p.sampler
      return entry
    })

    const results = await imageStore.uploadImages(files, metadataList)
    uploadResults.value = results as any[]
    showResults.value = true

    const successCount = results.filter((r: any) => r.status === 'success').length
    ElMessage.success(`Uploaded ${successCount} of ${results.length} images`)
    if (successCount > 0) loadStats()
  } catch (e: any) {
    ElMessage.error('Upload failed: ' + (e.message || 'Unknown error'))
  } finally {
    uploading.value = false
  }
}

function resetUpload() {
  fileList.value = []
  previewData.value = []
  uploadResults.value = []
  showResults.value = false
  bulkPrompt.value = ''
  bulkModel.value = ''
  bulkSteps.value = 30
  bulkCfg.value = 7
  bulkSampler.value = ''
  metadataOpen.value = false
}

function onDirSelected(path: string) {
  scanPath.value = path
  showDirPicker.value = false
  handleScan()
}

async function handleScan() {
  if (!scanPath.value) return
  scanning.value = true
  try {
    const results = await imageStore.scanDirectory(scanPath.value) as any[]
    uploadResults.value = results
    showResults.value = true
    const successCount = results.filter((r: any) => r.status === 'success').length
    ElMessage.success(`Imported ${successCount} of ${results.length} images from directory`)
    if (successCount > 0) loadStats()
  } catch (e: any) {
    ElMessage.error('Scan failed: ' + (e.message || 'Unknown error'))
  } finally {
    scanning.value = false
  }
}
</script>

<template>
  <div class="upload-view">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-text">
        <h1 class="page-title">Upload Images</h1>
        <p class="page-subtitle">Import AI-generated images to your library</p>
      </div>
      <div class="header-accent" />
    </div>

    <div class="upload-layout">
      <!-- Left Column: Upload -->
      <div class="upload-column">
        <div class="card upload-card">
          <!-- File Selection -->
          <div class="card-section">
            <div class="section-title-row">
              <div class="section-title">
                <el-icon><Upload /></el-icon>
                <span>Select Files</span>
              </div>
              <span v-if="hasFiles" class="file-badge">{{ fileCount }} file{{ fileCount > 1 ? 's' : '' }}</span>
            </div>

            <el-upload
              v-model:file-list="fileList"
              drag
              multiple
              :auto-upload="false"
              list-type="picture-card"
              accept="image/png,image/jpeg,image/webp,image/gif,image/bmp"
              class="custom-upload"
            >
              <div class="upload-placeholder">
                <el-icon class="upload-icon"><Upload /></el-icon>
                <div class="upload-text">Drag images here or click to browse</div>
                <div class="upload-hint">PNG, JPG, WebP, GIF, BMP &mdash; up to 100MB each</div>
              </div>
            </el-upload>

            <div v-if="hasFiles" class="file-summary">
              <span>{{ formattedSize }} total</span>
            </div>
          </div>

          <!-- Metadata Preview (auto-extracted, per-file editable) -->
          <div v-if="hasFiles" class="card-section">
            <div class="section-title-row">
              <div class="section-title">
                <span>Metadata Preview</span>
                <el-tag v-if="analyzing" size="small" effect="plain" type="info" class="analyzing-tag">Analyzing...</el-tag>
                <el-tag v-else-if="previewData.length > 0" size="small" effect="plain" type="success">Auto-detected</el-tag>
              </div>
              <div class="preview-actions">
                <el-button size="small" text @click="metadataOpen = !metadataOpen">
                  {{ metadataOpen ? 'Hide' : 'Edit All' }}
                </el-button>
              </div>
            </div>

            <!-- Compact per-file metadata table -->
            <div v-if="previewData.length > 0" class="preview-table-wrapper">
              <!-- Bulk fill row -->
              <div v-show="metadataOpen" class="bulk-fill-row">
                <span class="bulk-label">Bulk Fill:</span>
                <el-input v-model="bulkPrompt" placeholder="Prompt" size="small" style="width:180px" clearable />
                <el-input v-model="bulkModel" placeholder="Model" size="small" style="width:130px" clearable />
                <el-input-number v-model="bulkSteps" :min="1" :max="150" size="small" style="width:110px" placeholder="Steps" />
                <el-input-number v-model="bulkCfg" :min="1" :max="30" :step="0.5" size="small" style="width:110px" placeholder="CFG" />
                <el-input v-model="bulkSampler" placeholder="Sampler" size="small" style="width:130px" clearable />
                <el-button size="small" type="primary" plain @click="applyBulkToAll">Apply to All</el-button>
              </div>

              <div class="preview-scroll">
                <table class="preview-table">
                  <thead>
                    <tr>
                      <th class="col-file">File</th>
                      <th class="col-dim">Dim</th>
                      <th class="col-prompt">Prompt</th>
                      <th class="col-model">Model</th>
                      <th class="col-steps">Steps</th>
                      <th class="col-cfg">CFG</th>
                      <th class="col-seed">Seed</th>
                      <th class="col-sampler">Sampler</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(p, idx) in previewData" :key="p.originalFilename">
                      <td class="col-file" :title="p.originalFilename">{{ p.originalFilename }}</td>
                      <td class="col-dim">{{ p.width && p.height ? `${p.width}x${p.height}` : '-' }}</td>
                      <td class="col-prompt">
                        <el-input v-model="p.prompt" size="small" placeholder="Auto" :title="p.prompt || ''" />
                      </td>
                      <td class="col-model">
                        <el-input v-model="p.model" size="small" placeholder="Auto" />
                      </td>
                      <td class="col-steps">
                        <el-input-number v-model="p.steps" :min="1" :max="150" size="small" controls-position="right" style="width:80px" placeholder="-" />
                      </td>
                      <td class="col-cfg">
                        <el-input-number v-model="p.cfgScale" :min="1" :max="30" :step="0.5" size="small" controls-position="right" style="width:80px" placeholder="-" />
                      </td>
                      <td class="col-seed">
                        <el-input v-model="p.seed" size="small" placeholder="-" />
                      </td>
                      <td class="col-sampler">
                        <el-input v-model="p.sampler" size="small" placeholder="Auto" />
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="card-section actions-section">
            <el-button
              type="primary"
              size="large"
              :loading="uploading"
              :disabled="!hasFiles"
              class="action-btn primary"
              @click="handleUpload"
            >
              {{ uploading ? 'Uploading...' : 'Upload All' }}
            </el-button>
            <el-button size="large" :disabled="!hasFiles && !showResults" @click="resetUpload">
              Reset
            </el-button>
          </div>
        </div>
      </div>

      <!-- Right Column -->
      <div class="side-column">
        <!-- Scan Directory -->
        <div class="card scan-card">
          <div class="scan-header">
            <el-icon class="scan-header-icon"><FolderOpened /></el-icon>
            <div class="scan-header-text">
              <h3>Scan Directory</h3>
              <p>Browse a local folder and import all supported images.</p>
            </div>
          </div>
          <div class="scan-row">
            <el-input
              v-model="scanPath"
              placeholder="Select a folder..."
              size="large"
              class="scan-input"
              readonly
            >
              <template #append>
                <el-button @click="showDirPicker = true" class="scan-btn">
                  Browse
                </el-button>
              </template>
            </el-input>
          </div>
          <div v-if="scanPath" class="scan-actions">
            <el-button
              type="primary"
              :loading="scanning"
              @click="handleScan"
            >
              {{ scanning ? 'Scanning...' : 'Import from This Folder' }}
            </el-button>
          </div>
        </div>

        <!-- Storage Stats -->
        <div v-if="stats" class="card stats-card">
          <h3 class="stats-title">Storage</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-value">{{ formatBytes(stats.usedBytes) }}</span>
              <span class="stat-label">Disk Used</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.imageCount }}</span>
              <span class="stat-label">Images</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.thumbnailCount }}</span>
              <span class="stat-label">Thumbnails</span>
            </div>
          </div>
        </div>

        <!-- Results -->
        <div v-if="showResults" class="card results-card">
          <div class="results-header">
            <h3>Results</h3>
            <div class="results-summary-badges">
              <span class="badge badge-success">{{ resultSuccessCount }} succeeded</span>
              <span v-if="resultErrorCount > 0" class="badge badge-error">{{ resultErrorCount }} failed</span>
            </div>
          </div>
          <div class="results-list">
            <div
              v-for="r in uploadResults"
              :key="r.originalFilename"
              class="result-item"
              :class="r.status"
            >
              <div class="result-icon">
                <el-icon v-if="r.status === 'success'" color="#10b981"><Check /></el-icon>
                <el-icon v-else color="#ef4444"><Close /></el-icon>
              </div>
              <span class="result-name">{{ r.originalFilename }}</span>
              <span v-if="r.status === 'error'" class="result-error-msg">{{ r.errorMessage }}</span>
            </div>
          </div>
          <div class="results-footer">
            <el-button size="small" text @click="resetUpload">Clear Results</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Directory Picker Dialog -->
    <el-dialog
      v-model="showDirPicker"
      title="Browse Directory"
      width="520px"
      :close-on-click-modal="false"
    >
      <DirectoryPicker @select="onDirSelected" />
      <template #footer>
        <el-button @click="showDirPicker = false">Cancel</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ============ Layout ============ */
.upload-view {
  max-width: 1280px;
  margin: 0 auto;
  padding: 8px 0;
}

/* ============ Header ============ */
.page-header {
  margin-bottom: 28px;
}

.header-text {
  margin-bottom: 14px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.02em;
  line-height: 1.2;
  margin-bottom: 6px;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.header-accent {
  height: 3px;
  width: 72px;
  border-radius: 4px;
  background: linear-gradient(135deg, #6366f1, #a855f7);
}

/* ============ Two-column layout ============ */
.upload-layout {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 24px;
  align-items: start;
}

/* ============ Card base ============ */
.card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8ecf1;
  padding: 24px;
}

.card + .card {
  margin-top: 20px;
}

.card-section {
  padding-bottom: 20px;
}

.card-section + .card-section {
  border-top: 1px solid #f0f2f5;
  padding-top: 20px;
}

.actions-section {
  border-top: 1px solid #f0f2f5;
  padding-top: 20px;
  padding-bottom: 0;
  display: flex;
  gap: 12px;
}

/* ============ Section title ============ */
.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.section-title .el-icon {
  font-size: 18px;
  color: #6366f1;
}

.file-badge {
  font-size: 12px;
  font-weight: 500;
  color: #6366f1;
  background: #eef2ff;
  padding: 2px 10px;
  border-radius: 20px;
}

/* ============ Upload zone ============ */
:deep(.custom-upload .el-upload--picture-card) {
  width: 100%;
  height: auto;
  min-height: 180px;
  border: 2px dashed #d0d5dd;
  border-radius: 12px;
  background: #fafbfc;
  transition: all 0.25s ease;
  margin-bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.custom-upload .el-upload--picture-card:hover) {
  border-color: #6366f1;
  background: #f5f3ff;
}

:deep(.custom-upload .el-upload-dragger) {
  border: none;
  background: transparent;
  border-radius: 12px;
  width: 100%;
  padding: 32px 24px;
  height: auto;
}

:deep(.custom-upload .el-upload-dragger:hover) {
  border: none;
}

:deep(.custom-upload .el-upload-dragger.is-dragover) {
  background: #eef2ff;
}

:deep(.custom-upload .el-upload-list--picture-card) {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  margin-top: 16px;
}

:deep(.custom-upload .el-upload-list__item) {
  width: 100% !important;
  height: 120px !important;
  border-radius: 10px !important;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
}

:deep(.custom-upload .el-upload-list__item:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
}

:deep(.custom-upload .el-upload-list__item-thumbnail) {
  object-fit: cover;
}

/* Upload placeholder content */
.upload-placeholder {
  text-align: center;
  padding: 8px 0;
}

.upload-icon {
  font-size: 40px !important;
  color: #94a3b8;
  margin-bottom: 10px;
}

.upload-text {
  font-size: 15px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 6px;
}

.upload-hint {
  font-size: 12px;
  color: #94a3b8;
}

/* File summary */
.file-summary {
  margin-top: 14px;
  font-size: 13px;
  color: #64748b;
  text-align: right;
}

/* ============ Metadata section ============ */
.metadata-card-section {
  cursor: pointer;
  user-select: none;
}

.metadata-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.metadata-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.metadata-badge {
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
  background: #f1f5f9;
  padding: 1px 8px;
  border-radius: 10px;
}

.chevron {
  font-size: 18px;
  color: #94a3b8;
  transition: transform 0.3s ease;
}

.chevron.rotated {
  transform: rotate(180deg);
}

.metadata-body {
  margin-top: 16px;
  overflow: hidden;
}

/* ============ Metadata Preview Table ============ */
.analyzing-tag {
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.bulk-fill-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 8px 10px;
  background: #f8fafc;
  border-radius: 6px;
  flex-wrap: wrap;
}

.bulk-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
}

.preview-scroll {
  overflow-x: auto;
  margin: 0 -4px;
  padding: 0 4px;
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  min-width: 780px;
}

.preview-table th {
  text-align: left;
  padding: 6px 4px;
  font-weight: 600;
  color: #64748b;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  border-bottom: 2px solid #e8ecf1;
  white-space: nowrap;
}

.preview-table td {
  padding: 4px 4px;
  border-bottom: 1px solid #f0f2f5;
  vertical-align: middle;
}

.preview-table tr:hover td {
  background: #f8fafc;
}

.col-file {
  min-width: 100px;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-dim {
  width: 70px;
  white-space: nowrap;
  color: #64748b;
}

.col-prompt {
  min-width: 160px;
}

.col-model {
  width: 110px;
}

.col-steps {
  width: 80px;
}

.col-cfg {
  width: 80px;
}

.col-seed {
  width: 100px;
}

.col-sampler {
  width: 110px;
}

.preview-table :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e8ecf1 inset !important;
  background: #fafbfc;
}

.preview-table :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c4c9d1 inset !important;
}

.preview-table :deep(.el-input-number.is-controls-right .el-input-number__increase) {
  border-left: none;
}

/* ============ Actions ============ */
.action-btn {
  min-width: 150px;
}

/* ============ Scan card ============ */
.scan-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.scan-header-icon {
  font-size: 28px;
  color: #6366f1;
  flex-shrink: 0;
  margin-top: 2px;
}

.scan-header-text h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.scan-header-text p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  line-height: 1.4;
}

.scan-input {
  --el-input-focus-border-color: #6366f1;
}

.scan-btn {
  --el-button-hover-bg-color: #eef2ff;
  --el-button-hover-text-color: #6366f1;
}

.scan-row {
  margin-bottom: 12px;
}

.scan-actions {
  display: flex;
  justify-content: flex-end;
}

/* ============ Storage Stats ============ */
.stats-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 14px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.stat-item {
  text-align: center;
  padding: 10px 6px;
  background: #f8fafc;
  border-radius: 8px;
}

.stat-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #6366f1;
  line-height: 1.2;
}

.stat-label {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
  text-transform: uppercase;
}

/* ============ Results ============ */
.results-card {
  animation: slideUp 0.35s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.results-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.results-summary-badges {
  display: flex;
  gap: 8px;
}

.badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 12px;
}

.badge-success {
  color: #065f46;
  background: #d1fae5;
}

.badge-error {
  color: #991b1b;
  background: #fee2e2;
}

.results-list {
  max-height: 340px;
  overflow-y: auto;
  margin: 0 -4px;
  padding: 0 4px;
}

.results-list::-webkit-scrollbar {
  width: 4px;
}

.results-list::-webkit-scrollbar-thumb {
  background: #d0d5dd;
  border-radius: 4px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  font-size: 13px;
  border-bottom: 1px solid #f0f2f5;
}

.result-item:last-child {
  border-bottom: none;
}

.result-icon {
  flex-shrink: 0;
  display: flex;
  font-size: 16px;
}

.result-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #334155;
  font-weight: 500;
}

.result-item.error .result-name {
  color: #dc2626;
}

.result-error-msg {
  font-size: 11px;
  color: #94a3b8;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.results-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
  display: flex;
  justify-content: flex-end;
}

/* ============ Responsive ============ */
@media (max-width: 900px) {
  .upload-layout {
    grid-template-columns: 1fr;
  }
}
</style>
