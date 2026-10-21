import { isValidDesignerId } from './designerProfileRules.js'

export const MAX_COMPARE_DESIGNERS = 3
export const MIN_COMPARE_DESIGNERS = 2

// Keep selection local to navigation state; no favourites or comparison records are created.
export function normalizeComparisonIds(ids, availableIds = null) {
  if (!Array.isArray(ids)) return []
  const available = availableIds == null ? null : new Set(availableIds.map(String))
  const result = []
  for (const value of ids) {
    const id = String(value)
    if (!isValidDesignerId(id) || (available && !available.has(id)) || result.includes(id)) continue
    result.push(id)
    if (result.length === MAX_COMPARE_DESIGNERS) break
  }
  return result
}

export function toggleComparisonId(ids, id, availableIds) {
  const selected = normalizeComparisonIds(ids, availableIds)
  const candidate = String(id)
  if (!isValidDesignerId(candidate) || !availableIds.map(String).includes(candidate)) {
    return { ids: selected, limitReached: false }
  }
  if (selected.includes(candidate)) {
    return { ids: selected.filter((value) => value !== candidate), limitReached: false }
  }
  if (selected.length >= MAX_COMPARE_DESIGNERS) {
    return { ids: selected, limitReached: true }
  }
  return { ids: [...selected, candidate], limitReached: false }
}

export function projectStatusCounts(projects) {
  const items = Array.isArray(projects) ? projects : []
  return {
    total: items.length,
    current: items.filter((project) => project.status === 'CURRENT').length,
    completed: items.filter((project) => project.status === 'COMPLETED').length,
  }
}

export function portfolioPreviewProjects(projects) {
  return Array.isArray(projects) ? projects.slice(0, 2) : []
}

export function comparisonText(value) {
  return typeof value === 'string' && value.trim() ? value.trim() : 'Not provided'
}
