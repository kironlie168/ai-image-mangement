import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { TagDto } from '@/types/image'
import * as api from '@/api/tagApi'

export const useTagStore = defineStore('tag', () => {
  const tags = ref<TagDto[]>([])
  const loading = ref(false)

  async function fetchTags() {
    loading.value = true
    try {
      tags.value = await api.fetchTags()
    } finally {
      loading.value = false
    }
  }

  async function createTag(name: string, color: string) {
    const tag = await api.createTag(name, color)
    tags.value.push(tag)
    return tag
  }

  async function updateTag(id: number, name: string, color: string) {
    const updated = await api.updateTag(id, name, color)
    const idx = tags.value.findIndex((t) => t.id === id)
    if (idx >= 0) tags.value[idx] = updated
    return updated
  }

  async function deleteTag(id: number) {
    await api.deleteTag(id)
    tags.value = tags.value.filter((t) => t.id !== id)
  }

  return { tags, loading, fetchTags, createTag, updateTag, deleteTag }
})
