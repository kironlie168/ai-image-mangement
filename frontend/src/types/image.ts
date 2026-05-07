export interface TagDto {
  id: number
  name: string
  color: string
}

export interface ImageDto {
  id: number
  originalFilename: string
  thumbnailUrl: string
  imageUrl: string
  fileSize: number
  width: number
  height: number
  format: string
  prompt: string
  negativePrompt: string
  model: string
  steps: number
  cfgScale: number
  seed: number
  sampler: string
  workflowJson: string | null
  comfyuiPromptJson: string | null
  isFavorite: boolean
  folderId: number | null
  folderName: string
  tags: TagDto[]
  createdAt: string
  updatedAt: string
}

export interface FolderDto {
  id: number
  name: string
  parentId: number | null
  description: string
  imageCount: number
  children: FolderDto[]
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface FileMetadataPreview {
  originalFilename: string
  fileSize: number
  width: number | null
  height: number | null
  format: string
  prompt: string | null
  negativePrompt: string | null
  model: string | null
  steps: number | null
  cfgScale: number | null
  seed: number | null
  sampler: string | null
  workflowJson: string | null
  comfyuiPromptJson: string | null
}

export interface ImageFilters {
  keyword: string
  folderId: number | null
  favorite: boolean | null
  tagIds: number[]
  sort: string
}
