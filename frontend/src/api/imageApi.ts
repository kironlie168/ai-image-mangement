import http from './http'
import type { ImageDto, ImageFilters, FileMetadataPreview, PageResponse } from '@/types/image'

export async function fetchImages(filters: ImageFilters, page: number, size: number, sort: string) {
  const params: Record<string, string | number | boolean> = { page, size, sort }
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.folderId) params.folderId = filters.folderId
  if (filters.favorite) params.favorite = true
  if (filters.tagIds.length) params.tagIds = filters.tagIds.join(',')
  const res = await http.get<PageResponse<ImageDto>>('/images', { params })
  return res.data
}

export async function fetchImage(id: number) {
  const res = await http.get<ImageDto>(`/images/${id}`)
  return res.data
}

export async function uploadImages(files: File[], metadataList?: Record<string, string | number | null>[]) {
  const form = new FormData()
  files.forEach((f) => form.append('files', f))
  if (metadataList && metadataList.length > 0) {
    // Strip null values to keep payload small
    const clean = metadataList.map(m => {
      const entry: Record<string, string | number> = {}
      for (const [k, v] of Object.entries(m)) {
        if (v !== null && v !== undefined) entry[k] = v
      }
      return entry
    })
    form.append('metadataList', JSON.stringify(clean))
  }
  const res = await http.post('/images/upload', form)
  return res.data as Array<{ id: number; originalFilename: string; status: string; errorMessage?: string }>
}

export async function previewMetadata(files: File[]) {
  const form = new FormData()
  files.forEach((f) => form.append('files', f))
  const res = await http.post('/images/preview-metadata', form)
  return res.data as FileMetadataPreview[]
}

export async function scanDirectory(path: string) {
  const res = await http.post('/images/scan', { directoryPath: path })
  return res.data as Array<{ id: number; originalFilename: string; status: string }>
}

export async function deleteImage(id: number) {
  await http.delete(`/images/${id}`)
}

export async function batchDelete(ids: number[]) {
  await http.delete('/images/batch', { data: ids })
}

export async function toggleFavorite(id: number) {
  const res = await http.put<ImageDto>(`/images/${id}/favorite`)
  return res.data
}

export async function updateMetadata(id: number, data: Record<string, string | number | null>) {
  const res = await http.put<ImageDto>(`/images/${id}/metadata`, data)
  return res.data
}

export async function setTags(id: number, tagIds: number[]) {
  const res = await http.put<ImageDto>(`/images/${id}/tags`, tagIds)
  return res.data
}

export async function batchTag(imageIds: number[], tagIds: number[]) {
  await http.post('/images/batch/tags', { imageIds, tagIds })
}

export async function appendTags(id: number, tagIds: number[]) {
  const res = await http.post<ImageDto>(`/images/${id}/tags`, tagIds)
  return res.data
}

export async function removeTagsFromImage(id: number, tagIds: number[]) {
  const res = await http.delete<ImageDto>(`/images/${id}/tags`, { data: tagIds })
  return res.data
}

export async function batchRemoveTags(imageIds: number[], tagIds: number[]) {
  await http.delete('/images/batch/tags', { data: { imageIds, tagIds } })
}

export async function downloadImage(id: number) {
  const res = await http.get(`/images/${id}/download`, { responseType: 'blob' })
  const disposition = res.headers['content-disposition']
  let filename = `image_${id}.png`
  if (disposition) {
    const match = disposition.match(/filename\*?=(?:UTF-\d'')?([^;\s]+)/i)
    if (match) filename = decodeURIComponent(match[1])
  }
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

export async function downloadWorkflow(id: number) {
  const res = await http.get(`/images/${id}/workflow`, { responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = `workflow_${id}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
