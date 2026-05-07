import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { FolderDto } from '@/types/image'
import * as api from '@/api/folderApi'

export const useFolderStore = defineStore('folder', () => {
  const folderTree = ref<FolderDto[]>([])
  const loading = ref(false)

  async function fetchTree() {
    loading.value = true
    try {
      folderTree.value = await api.fetchFolderTree()
    } finally {
      loading.value = false
    }
  }

  async function createFolder(name: string, parentId?: number | null, description?: string) {
    const folder = await api.createFolder(name, parentId, description)
    await fetchTree()
    return folder
  }

  async function updateFolder(id: number, name: string, parentId?: number | null, description?: string) {
    const folder = await api.updateFolder(id, name, parentId, description)
    await fetchTree()
    return folder
  }

  async function deleteFolder(id: number) {
    await api.deleteFolder(id)
    await fetchTree()
  }

  return { folderTree, loading, fetchTree, createFolder, updateFolder, deleteFolder }
})
