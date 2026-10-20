export function isValidDesignerId(id) {
  return typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))
}

export function profileIntroduction(description) {
  return description?.trim() || 'This designer has not added a company introduction yet.'
}

export function profileProjectCountLabel(count) {
  return `${count} project${count === 1 ? '' : 's'}`
}

export function publicProjectPath(id) {
  return `/projects/${encodeURIComponent(id)}`
}
