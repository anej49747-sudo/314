export const MAX_MESSAGE_LENGTH = 2000

export function validMessageBody(body) {
  return typeof body === 'string' && body.trim().length > 0 && body.length <= MAX_MESSAGE_LENGTH
}

export function validDesignerId(id) {
  return typeof id === 'string' && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))
}

export function messageErrorMessage(failure) {
  switch (failure?.response?.status) {
    case 401: return 'Your session has expired. Please log in again.'
    case 403: return 'You do not have permission to use this conversation.'
    case 404: return 'This conversation or designer is no longer available.'
    case 409: return 'The recipient is currently unavailable.'
    case 400: return 'Please enter a message of up to 2,000 characters.'
    default: return 'Unable to load or send messages. Please try again.'
  }
}

export function initials(name) {
  const words = typeof name === 'string' ? name.trim().split(/\s+/).filter(Boolean) : []
  return words.slice(0, 2).map((word) => word.charAt(0).toUpperCase()).join('') || '?'
}
