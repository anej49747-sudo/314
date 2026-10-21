/** Images are supplied by the public project DTO in their display order. */
export function projectPhotos(project) {
  return Array.isArray(project?.images) ? project.images : []
}

export function photoAlt(projectTitle, index) {
  return `${projectTitle}, project photo ${index + 1}`
}

/** Circular navigation works for one image, too. */
export function shiftedPhotoIndex(current, direction, total) {
  if (!Number.isInteger(current) || total <= 0) return null
  return (current + direction + total) % total
}
