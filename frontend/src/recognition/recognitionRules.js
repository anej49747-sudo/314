export function singaporeMonth(date = new Date()) {
  // YYYY-MM from the same calendar zone used by the backend.
  const parts = new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Singapore', year: 'numeric',
    month: '2-digit' }).formatToParts(date)
  const year = parts.find((part) => part.type === 'year').value
  const month = parts.find((part) => part.type === 'month').value
  return `${year}-${month}`
}

export function validFeatureMonth(month) {
  return typeof month === 'string' && /^\d{4}-(0[1-9]|1[0-2])$/.test(month)
}

export function normalizeAwardTitle(value) {
  const title = typeof value === 'string' ? value.trim().replace(/\s+/g, ' ') : ''
  if (title.length < 3 || title.length > 70) throw new Error('Award title must be 3–70 characters.')
  return title
}

export function designerAwards(awards, designerId) {
  return (Array.isArray(awards) ? awards : []).filter((award) => String(award.designerId) === String(designerId))
}

export function compactDesignerAwards(awards, designerId) {
  const all = designerAwards(awards, designerId)
  return { displayed: all.slice(0, 2), overflow: Math.max(0, all.length - 2) }
}

export function isFeaturedProject(featured, projectId) {
  return featured != null && String(featured.projectId) === String(projectId)
}

export function recognitionError(failure) {
  const code = failure?.response?.status
  if (code === 409) return 'This designer already has an award with that title, or the selection changed. Refresh and try again.'
  if (code === 404) return 'The selected project, designer or recognition no longer exists.'
  if (code === 401 || code === 403) return 'Platform Manager access is required.'
  if (code === 400) return failure?.response?.data?.message || 'Please check the entered details.'
  return 'Unable to update recognition. Please try again.'
}
