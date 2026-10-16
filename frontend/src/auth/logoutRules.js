export function logoutFailureMessage(failure) {
  if (failure?.response?.status === 403) {
    return 'Your security session has expired. Refresh the page and try again.'
  }
  return 'Unable to log out. Please try again.'
}
