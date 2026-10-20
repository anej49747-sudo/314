export function normalizeProjectQuery(value) {
  return value?.trim() || ''
}

export function projectSearchCount(count) {
  return `${count} ${count === 1 ? 'project' : 'projects'}`
}

export function projectSearchPath(id) {
  return `/projects/${encodeURIComponent(id)}`
}

export function projectSearchDescription(description) {
  return description?.trim() || 'No project description is available.'
}

// Only the last submitted title query is passed to the API; category changes
// can trigger a separate fetch without submitting the current input text.
export function projectSearchParams(titleQuery, categoryId = null) {
  const query = normalizeProjectQuery(titleQuery)
  return {
    ...(query ? { q: query } : {}),
    ...(categoryId == null || categoryId === 'all' ? {} : { categoryId }),
  }
}

export function projectSearchHeading(query, categoryName) {
  if (query && categoryName) return 'Filtered projects'
  if (query) return 'Search results'
  if (categoryName) return `${categoryName} projects`
  return 'All projects'
}
