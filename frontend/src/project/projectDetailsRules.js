export function isValidProjectId(value) {
  return typeof value === 'string' && /^[1-9]\d*$/.test(value) && Number.isSafeInteger(Number(value))
}

export function projectStatusLabel(status) {
  return status === 'COMPLETED' ? 'Completed' : 'Current'
}

export function projectDescription(description) {
  return description?.trim() || 'No project description is available.'
}

export function projectBackLink(state) {
  const id = state?.designerId
  if (typeof id === 'number' && Number.isSafeInteger(id) && id > 0) {
    return { to: `/designers/${id}`, label: 'Back to designer profile' }
  }
  if (isValidProjectId(id)) {
    return { to: `/designers/${id}`, label: 'Back to designer profile' }
  }
  return { to: '/projects', label: 'Back to projects' }
}
