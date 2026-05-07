import http from './http'

export interface DirEntry {
  name: string
  path: string
  isDirectory: boolean
  imageCount: number
}

export interface StorageStats {
  usedBytes: number
  imageCount: number
  thumbnailCount: number
}

export async function getDrives() {
  const res = await http.get<string[]>('/files/drives')
  return res.data
}

export async function browse(path: string) {
  const res = await http.get<DirEntry[]>('/files/browse', { params: { path } })
  return res.data
}

export async function getStats() {
  const res = await http.get<StorageStats>('/files/stats')
  return res.data
}

