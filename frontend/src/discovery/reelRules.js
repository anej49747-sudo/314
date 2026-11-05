// Keep a reel session free of repeated project posts, even across batches.
export function freshReelProjects(seenIds, incoming) {
  const found = new Set(seenIds)
  return incoming.filter(project => {
    if (found.has(project.id)) return false
    found.add(project.id)
    return true
  })
}

// Horizontal photo navigation cannot move beyond a project's own gallery.
export function reelPhotoIndex(current, delta, photoCount) {
  if (photoCount <= 0) return 0
  return Math.max(0, Math.min(photoCount - 1, current + delta))
}
