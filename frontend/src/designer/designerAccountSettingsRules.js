const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function buildDesignerAccountSettingsPayload(values) {
  const displayName = values.displayName?.trim() ?? ''
  const email = values.email?.trim().toLowerCase() ?? ''
  if (!displayName || displayName.length > 160) {
    throw new Error('Enter a display name with 1–160 characters.')
  }
  if (!EMAIL_PATTERN.test(email) || email.length > 254) {
    throw new Error('Enter a valid email address (254 characters or fewer).')
  }
  return { displayName, email }
}

export function designerAccountSettingsChanged(current, values) {
  if (!current) return false
  return (values.displayName?.trim() ?? '') !== current.displayName ||
    (values.email?.trim().toLowerCase() ?? '') !== current.email.toLowerCase()
}

export function designerAccountSettingsFailureMessage(error) {
  const status = error?.response?.status
  if (status === 409) return 'An account with this email already exists.'
  if (status === 400) return 'Please correct the highlighted fields.'
  if (status === 401 || status === 403) return 'Your session has expired or your account cannot be updated. Please log in again.'
  return 'Unable to update account details. Please try again.'
}
