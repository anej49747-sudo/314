export const REPORT_REASONS = [
  { value: 'MISLEADING_INFORMATION', label: 'Misleading profile or project information' },
  { value: 'INAPPROPRIATE_COMMUNICATION', label: 'Inappropriate communication' },
  { value: 'SUSPICIOUS_CONDUCT', label: 'Suspicious or misleading business conduct' },
  { value: 'OTHER', label: 'Other concern' },
]
export const REPORT_STATUSES = ['OPEN', 'RESOLVED', 'DISMISSED']

export function reportReasonLabel(value) {
  return REPORT_REASONS.find((entry) => entry.value === value)?.label ?? 'Unknown reason'
}

export function validateCustomerReport(reason, description) {
  if (!REPORT_REASONS.some((entry) => entry.value === reason)) return 'Select a reason for reporting.'
  const length = description.trim().length
  if (length < 20 || length > 2000) return 'Enter a description between 20 and 2,000 characters.'
  return ''
}

export function validateReportDecision(outcome, note) {
  if (!['RESOLVED', 'DISMISSED'].includes(outcome)) return 'Choose a resolution outcome.'
  const length = note.trim().length
  if (length < 10 || length > 1000) return 'Enter an internal note between 10 and 1,000 characters.'
  return ''
}

export function reportTimestamp(timestamp) {
  if (!timestamp || Number.isNaN(new Date(timestamp).getTime())) return 'Unknown date'
  return new Intl.DateTimeFormat('en-SG', {
    timeZone: 'Asia/Singapore', dateStyle: 'medium', timeStyle: 'short', hour12: false,
  }).format(new Date(timestamp))
}

export function reportErrorMessage(error, admin = false) {
  if (error.response?.status === 409) return admin
    ? 'This report has already been closed. Refresh the list.'
    : 'You already have an open report about this designer.'
  if (error.response?.status === 404) return 'The requested designer or report is no longer available.'
  if (error.response?.status === 400) return 'Please check the required report fields.'
  return 'Unable to process the report. Please try again.'
}
