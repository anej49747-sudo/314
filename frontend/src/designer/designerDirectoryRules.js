export function directoryDescription(description) {
  return description?.trim() || 'Company information is not available yet.'
}

export function directoryAddress(address) {
  return address?.trim() || ''
}

export function directoryCountLabel(count) {
  return `${count} ${count === 1 ? 'designer' : 'designers'}`
}

export function designerProfilePath(id) {
  return `/designers/${encodeURIComponent(id)}`
}
