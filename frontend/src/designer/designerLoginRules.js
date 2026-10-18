import { validatePassword } from '../registration/registrationRules.js'

export function designerLoginDestination(profile) {
  if (profile?.role !== 'INTERIOR_DESIGNER') return null
  return profile.passwordChangeRequired ? '/designer/change-password' : '/designer'
}

export function validatePasswordChange(values) {
  const invalid = validatePassword(values.newPassword)
  if (invalid) return invalid
  if (values.newPassword !== values.confirmPassword) return 'New passwords do not match.'
  return null
}

export function buildPasswordChangePayload(values) {
  return { newPassword: values.newPassword }
}

export function passwordChangeFailureMessage(failure) {
  const code = failure?.response?.status
  if (code === 401) return 'Your session has expired. Please log in again.'
  if (code === 400) return 'Check your new password and try again.'
  if (code === 403) return 'Password setup is unavailable for this account or your session has expired. Please log in again.'
  return 'Unable to change your password right now. Please try again.'
}
