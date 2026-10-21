export function publicReviewCountLabel(count) {
  const total = Number.isInteger(count) && count >= 0 ? count : 0
  return `${total} review${total === 1 ? '' : 's'}`
}

export function publicReviewStars(rating) {
  const stars = Number.isInteger(rating) && rating >= 1 && rating <= 5 ? rating : 0
  return { filled: '★'.repeat(stars), empty: '☆'.repeat(5 - stars) }
}

export function publicReviewDate(value) {
  if (!value) return 'Date unavailable'
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return 'Date unavailable'
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC',
  }).format(date)
}

export function publicReviewDistribution(summary) {
  const total = Number.isInteger(summary?.totalReviews) && summary.totalReviews > 0
    ? summary.totalReviews : 0
  return [5, 4, 3, 2, 1].map((rating) => {
    const raw = summary?.ratingDistribution?.[rating] ?? 0
    const count = Number.isInteger(raw) && raw >= 0 ? raw : 0
    return { rating, count, percent: total ? Math.min(100, (count / total) * 100) : 0 }
  })
}

export function publicReviewRemainingText(total, shown) {
  const remaining = Math.max(0, total - shown)
  return `Show more reviews (${remaining} remaining)`
}
