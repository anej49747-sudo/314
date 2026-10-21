export const MAX_REVIEW_COMMENT_LENGTH = 2000

export function validReviewRating(value) {
  return Number.isInteger(value) && value >= 1 && value <= 5
}

export function buildReviewPayload(rating, comment) {
  if (!validReviewRating(rating)) return null
  const text = String(comment ?? '').trim()
  if (text.length > MAX_REVIEW_COMMENT_LENGTH) return null
  return { rating, comment: text || null }
}

export function reviewSubmitError(status, serverMessage) {
  if (status === 404) return 'This interior designer is no longer available.'
  if (status === 400) return serverMessage || 'Please check your rating and comment.'
  if (status === 403) return 'Only active Renovation Customers can submit reviews. Please check your account.'
  if (status === 401) return 'Please log in with a Renovation Customer account to submit a review.'
  return 'Unable to submit your review. Please try again.'
}

export function reviewRatingLabel(rating) {
  const labels = [null, 'Poor', 'Fair', 'Good', 'Very good', 'Excellent']
  return labels[rating] ?? 'Select a rating from 1 to 5 stars.'
}
