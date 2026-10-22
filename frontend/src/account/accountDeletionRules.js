export const DELETE_CONFIRMATION = 'DELETE'

export function canPermanentlyDelete(confirmation) {
  return confirmation === DELETE_CONFIRMATION
}

export function accountDeletionError(error) {
  if (error.response?.status === 404) return 'This designer account no longer exists. Refresh the list.'
  if (error.response?.status === 401 || error.response?.status === 403) return 'Your session is no longer authorised. Sign in again.'
  return 'Unable to delete the account. No successful deletion was confirmed. Please try again.'
}
