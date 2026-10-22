export const INITIAL_ACCOUNT_SEARCH = Object.freeze({ query: '', role: '', status: '' })

export function buildAccountSearchParams(filters, page = 0) {
  const query = (filters.query ?? '').trim()
  const params = { page }
  if (query) params.q = query
  if (filters.role) params.role = filters.role
  if (filters.status) params.status = filters.status
  return params
}

export function accountRoleLabel(role) {
  if (role === 'CUSTOMER') return 'Renovation Customer'
  if (role === 'INTERIOR_DESIGNER') return 'Interior Designer'
  return 'Unknown role'
}

export function accountStatusLabel(active) {
  return active ? 'Active' : 'Inactive'
}

export function accountSearchFailureMessage(failure) {
  if (failure.response?.status === 400) return 'Invalid search criteria. Please reset the filters and try again.'
  return 'Unable to load accounts. Please try again.'
}
