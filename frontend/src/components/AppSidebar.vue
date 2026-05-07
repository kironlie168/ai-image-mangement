<script setup lang="ts">
import { onMounted } from 'vue'
import { useImageStore } from '@/stores/imageStore'
import { useFolderStore } from '@/stores/folderStore'
import { useTagStore } from '@/stores/tagStore'
import { storeToRefs } from 'pinia'

const imageStore = useImageStore()
const folderStore = useFolderStore()
const tagStore = useTagStore()

const { folderTree } = storeToRefs(folderStore)
const { tags } = storeToRefs(tagStore)
const { filters } = storeToRefs(imageStore)

onMounted(() => {
  folderStore.fetchTree()
  tagStore.fetchTags()
})

function selectFolder(folderId: number | null) {
  imageStore.setFilters({ folderId })
}

function toggleTag(tagId: number) {
  const current = [...filters.value.tagIds]
  const idx = current.indexOf(tagId)
  if (idx >= 0) current.splice(idx, 1)
  else current.push(tagId)
  imageStore.setFilters({ tagIds: current })
}

function toggleFavoriteFilter() {
  imageStore.setFilters({ favorite: filters.value.favorite ? null : true })
}
</script>

<template>
  <div class="sidebar-content">
    <div class="sidebar-section">
      <h3 class="section-title">Folders</h3>
      <el-tree
        :data="folderTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        :highlight-current="true"
        @node-click="(data: any) => selectFolder(data.id)"
        :expand-on-click-node="true"
        class="folder-tree"
      />
    </div>

    <el-divider style="margin: 12px 0" />

    <div class="sidebar-section">
      <h3 class="section-title">Filters</h3>
      <el-button
        :type="filters.favorite ? 'warning' : 'default'"
        size="small"
        @click="toggleFavoriteFilter"
        style="width: 100%; margin-bottom: 8px"
      >
        {{ filters.favorite ? '★ Favorites' : '☆ Favorites' }}
      </el-button>
    </div>

    <el-divider style="margin: 12px 0" />

    <div class="sidebar-section">
      <h3 class="section-title">Tags</h3>
      <div class="tag-list">
        <el-tag
          v-for="tag in tags"
          :key="tag.id"
          :color="tag.color"
          :style="{
            cursor: 'pointer',
            opacity: filters.tagIds.includes(tag.id) ? 1 : 0.5,
          }"
          size="small"
          effect="dark"
          @click="toggleTag(tag.id)"
        >
          {{ tag.name }}
        </el-tag>
        <el-empty v-if="!tags.length" :image-size="40" description="No tags" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.sidebar-content { padding: 16px; }
.section-title { font-size: 13px; color: #909399; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.5px; }
.folder-tree { --el-tree-node-content-height: 32px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
</style>
