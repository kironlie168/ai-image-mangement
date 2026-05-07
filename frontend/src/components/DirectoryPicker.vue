<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { browse, getDrives } from '@/api/fileApi'
import type { DirEntry } from '@/api/fileApi'
import { ElMessage } from 'element-plus'
import { FolderOpened, Document } from '@element-plus/icons-vue'

const emit = defineEmits<{
  select: [path: string]
}>()

const loading = ref(false)
const currentPath = ref('')
const entries = ref<DirEntry[]>([])
const pathHistory = ref<string[]>([])
const error = ref('')

onMounted(() => loadDrives())

async function loadDrives() {
  loading.value = true
  error.value = ''
  try {
    const drives = await getDrives()
    entries.value = drives.map((d) => ({
      name: d,
      path: d,
      isDirectory: true,
      imageCount: 0,
    }))
    currentPath.value = ''
    pathHistory.value = []
  } catch (e: any) {
    error.value = 'Failed to load drives'
    entries.value = []
  } finally {
    loading.value = false
  }
}

async function openDir(path: string) {
  loading.value = true
  error.value = ''
  try {
    const result = await browse(path)
    entries.value = result
    pathHistory.value.push(currentPath.value)
    currentPath.value = path
  } catch (e: any) {
    error.value = e?.response?.data?.message || 'Failed to open directory'
    entries.value = []
  } finally {
    loading.value = false
  }
}

function goBack() {
  const prev = pathHistory.value.pop()
  if (prev === '') {
    loadDrives()
  } else if (prev) {
    openDir(prev)
  }
}

function selectCurrent() {
  if (currentPath.value) {
    emit('select', currentPath.value)
  }
}

</script>

<template>
  <div class="dir-picker">
    <!-- Path bar -->
    <div class="path-bar">
      <el-button size="small" :disabled="!pathHistory.length" @click="goBack">
        Back
      </el-button>
      <el-breadcrumb separator="/" class="path-breadcrumb">
        <el-breadcrumb-item v-if="!currentPath">Drives</el-breadcrumb-item>
        <el-breadcrumb-item v-else>{{ currentPath }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- Directory listing -->
    <div class="dir-list" v-loading="loading">
      <div v-if="error" class="dir-error">
        <el-alert :title="error" type="error" show-icon :closable="false" />
      </div>

      <div
        v-for="entry in entries"
        :key="entry.path"
        class="dir-item"
        :class="{ directory: entry.isDirectory }"
        @click="entry.isDirectory ? openDir(entry.path) : undefined"
      >
        <el-icon class="dir-item-icon" :color="entry.isDirectory ? '#6366f1' : '#94a3b8'">
          <FolderOpened v-if="entry.isDirectory" />
          <Document v-else />
        </el-icon>
        <span class="dir-item-name">{{ entry.name }}</span>
        <span v-if="entry.isDirectory && entry.imageCount > 0" class="dir-item-count">
          {{ entry.imageCount }} images
        </span>
      </div>

      <el-empty v-if="!loading && !entries.length && !error" description="Empty directory" :image-size="40" />
    </div>

    <!-- Select button -->
    <div class="dir-footer">
      <span v-if="currentPath" class="dir-footer-path">{{ currentPath }}</span>
      <el-button
        type="primary"
        :disabled="!currentPath"
        @click="selectCurrent"
      >
        Select This Folder
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.dir-picker {
  display: flex;
  flex-direction: column;
  min-height: 360px;
}

.path-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.path-breadcrumb {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.dir-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  margin: 0 -4px;
  padding: 0 4px;
}

.dir-list::-webkit-scrollbar {
  width: 4px;
}

.dir-list::-webkit-scrollbar-thumb {
  background: #d0d5dd;
  border-radius: 4px;
}

.dir-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: default;
  transition: background 0.15s;
}

.dir-item.directory {
  cursor: pointer;
}

.dir-item.directory:hover {
  background: #f5f3ff;
}

.dir-item-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.dir-item-name {
  flex: 1;
  font-size: 13px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dir-item-count {
  font-size: 11px;
  color: #94a3b8;
  flex-shrink: 0;
}

.dir-error {
  margin-bottom: 12px;
}

.dir-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

.dir-footer-path {
  font-size: 12px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 12px;
}
</style>
