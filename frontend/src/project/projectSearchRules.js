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
