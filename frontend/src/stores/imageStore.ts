import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import type { ImageDto, ImageFilters } from '@/types/image'
import * as api from '@/api/imageApi'

export const useImageStore = defineStore('image', () => {
  const images = ref<ImageDto[]>([])
  const currentImage = ref<ImageDto | null>(null)
  const loading = ref(false)
  const selectedIds = ref<Set<number>>(new Set())

  const pagination = reactive({
    page: 0,
    size: 48,
    totalElements: 0,
    totalPages: 0,
  })

  const filters = reactive<ImageFilters>({
    keyword: '',
    folderId: null,
    favorite: null,
    tagIds: [],
    sort: 'createdAt,desc',
  })

  async function fetchImages() {
    loading.value = true
    try {
      const result = await api.fetchImages(filters, pagination.page, pagination.size, filters.sort)
      images.value = result.content
      pagination.page = result.page
      pagination.size = result.size
      pagination.totalElements = result.totalElements
      pagination.totalPages = result.totalPages
    } finally {
      loading.value = false
    }
  }

  async function fetchImageDetail(id: number) {
    currentImage.value = await api.fetchImage(id)
  }

  async function uploadImages(files: File[], metadataList?: Record<string, string | number | null>[]) {
    return await api.uploadImages(files, metadataList)
  }

  async function scanDirectory(path: string) {
    return await api.scanDirectory(path)
  }

  async function deleteImage(id: number) {
    await api.deleteImage(id)
    selectedIds.value.delete(id)
    await fetchImages()
  }

  async function batchDelete() {
    const ids = Array.from(selectedIds.value)
    if (!ids.length) return
    await api.batchDelete(ids)
    selectedIds.value.clear()
    await fetchImages()
  }

  async function toggleFavorite(id: number) {
    const updated = await api.toggleFavorite(id)
    const idx = images.value.findIndex((i) => i.id === id)
    if (idx >= 0) images.value[idx] = updated
    if (currentImage.value?.id === id) currentImage.value = updated
  }

  async function setFilters(newFilters: Partial<ImageFilters>) {
    Object.assign(filters, newFilters)
    pagination.page = 0
    await fetchImages()
  }

  function setPage(page: number) {
    pagination.page = page
    fetchImages()
  }

  function toggleSelection(id: number) {
    if (selectedIds.value.has(id)) selectedIds.value.delete(id)
    else selectedIds.value.add(id)
  }

  function selectAll() {
    images.value.forEach((i) => selectedIds.value.add(i.id))
  }

  function clearSelection() {
    selectedIds.value.clear()
  }

  async function batchTagImages(tagIds: number[]) {
    const ids = Array.from(selectedIds.value)
    if (!ids.length) return
    await api.batchTag(ids, tagIds)
    await fetchImages()
  }

  async function batchRemoveTagsImages(tagIds: number[]) {
    const ids = Array.from(selectedIds.value)
    if (!ids.length) return
    await api.batchRemoveTags(ids, tagIds)
    await fetchImages()
  }

  async function setImageTags(id: number, tagIds: number[]) {
    const updated = await api.setTags(id, tagIds)
    if (currentImage.value?.id === id) currentImage.value = updated
    const idx = images.value.findIndex((i) => i.id === id)
    if (idx >= 0) images.value[idx] = updated
  }

  async function updateImageMetadata(id: number, data: Record<string, string | number | null>) {
    const updated = await api.updateMetadata(id, data)
    if (currentImage.value?.id === id) currentImage.value = updated
    const idx = images.value.findIndex((i) => i.id === id)
    if (idx >= 0) images.value[idx] = updated
  }

  return {
    images, currentImage, loading, selectedIds, pagination, filters,
    fetchImages, fetchImageDetail, uploadImages, scanDirectory,
    deleteImage, batchDelete, toggleFavorite,
    setFilters, setPage, toggleSelection, selectAll, clearSelection, batchTagImages, batchRemoveTagsImages,
    setImageTags, updateImageMetadata,
  }
})
