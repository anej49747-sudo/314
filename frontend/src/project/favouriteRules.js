import { isValidProjectId } from './projectDetailsRules.js'

// Only allow a local project-details return path after customer login.
export function customerLoginDestination(state) {
  const path = state?.returnTo
  if (path === '/customer/favourites') return path
  const match = typeof path === 'string' ? /^\/projects\/([1-9]\d*)$/.exec(path) : null
  return match && isValidProjectId(match[1]) ? path : '/customer'
}

export function favouriteAccessState(status) {
  return status === 401 || status === 403 ? 'visitor' : 'unavailable'
}

export function favouriteButtonLabel(saved) {
  return saved ? 'Saved to Favourites' : 'Save to Favourites'
}

export function favouriteResultMessage(saved) {
  return saved ? 'Project saved to favourites.' : 'Project removed from favourites.'
}
