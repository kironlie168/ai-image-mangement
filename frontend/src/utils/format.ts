export function formatFileSize(bytes: number): string {
  if ((!bytes && bytes !== 0) || Number.isNaN(bytes)) return ''
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return i === 3 ? `${size.toFixed(2)} ${units[i]}` : `${size.toFixed(1)} ${units[i]}`
}
