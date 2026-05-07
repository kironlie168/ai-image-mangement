import http from './http'
import type { TagDto } from '@/types/image'

export async function fetchTags() {
  const res = await http.get<TagDto[]>('/tags')
  return res.data
}

export async function createTag(name: string, color: string) {
  const res = await http.post<TagDto>('/tags', { name, color })
  return res.data
}

export async function updateTag(id: number, name: string, color: string) {
  const res = await http.put<TagDto>(`/tags/${id}`, { name, color })
  return res.data
}

export async function deleteTag(id: number) {
  await http.delete(`/tags/${id}`)
}
