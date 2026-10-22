export function moderationActionLabel(active) {
  return active ? 'Suspend' : 'Reactivate'
}

export function loginOutcomeLabel(successful) {
  return successful ? 'Successful' : 'Failed'
}

export function loginHistoryParams(outcome = 'all', page = 0) {
  return { outcome, page }
}

export function singaporeLoginTime(timestamp) {
  if (!timestamp) return 'Unknown time'
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return 'Unknown time'
  return new Intl.DateTimeFormat('en-SG', {
    dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Singapore', hour12: false,
  }).format(date)
}

export function moderationErrorMessage(error) {
  if (error.response?.status === 409) return 'Account status changed. Refresh the results and try again.'
  if (error.response?.status === 404) return 'Account no longer exists or cannot be managed.'
  if (error.response?.status === 400) return 'Please provide a valid suspension reason.'
  return 'Unable to update the account. Please try again.'
}

export function historyErrorMessage(error) {
  if (error.response?.status === 404) return 'This account is no longer available.'
  return 'Unable to load login history. Please try again.'
}
