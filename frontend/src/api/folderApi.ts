import http from './http'
import type { FolderDto } from '@/types/image'

export async function fetchFolderTree() {
  const res = await http.get<FolderDto[]>('/folders/tree')
  return res.data
}

export async function createFolder(name: string, parentId?: number | null, description?: string) {
  const res = await http.post<FolderDto>('/folders', { name, parentId, description })
  return res.data
}

export async function updateFolder(id: number, name: string, parentId?: number | null, description?: string) {
  const res = await http.put<FolderDto>(`/folders/${id}`, { name, parentId, description })
  return res.data
}

export async function deleteFolder(id: number) {
  await http.delete(`/folders/${id}`)
}
