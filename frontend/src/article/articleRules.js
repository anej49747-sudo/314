export const MAX_COVER_BYTES = 5 * 1024 * 1024
export const ALLOWED_COVER_TYPES = ['image/jpeg', 'image/png', 'image/webp']

export function validateCover(file) {
  if (!file) return ''
  if (!ALLOWED_COVER_TYPES.includes(file.type)) return 'Choose a JPG, PNG or WebP photo.'
  if (file.size === 0 || file.size > MAX_COVER_BYTES) return 'Cover photo must be between 1 byte and 5 MB.'
  return ''
}

export function articlePayload(values) {
  const title = String(values.title ?? '').trim()
  const summary = String(values.summary ?? '').trim()
  const content = String(values.content ?? '').trim()
  if (title.length < 5 || title.length > 120) throw new Error('Article title must be 5–120 characters.')
  if (summary.length < 20 || summary.length > 240) throw new Error('Summary must be 20–240 characters.')
  if (content.length < 100 || content.length > 10000) throw new Error('Article content must be 100–10,000 characters.')
  return { title, summary, content }
}

export function filterArticles(articles, query) {
  const needle = String(query || '').trim().toLocaleLowerCase()
  if (!needle) return articles
  return articles.filter(({ title, summary, content }) =>
    [title, summary, content].some((value) => String(value || '').toLocaleLowerCase().includes(needle)))
}

export function articleParagraphs(content) {
  return String(content || '').trim().split(/\n\s*\n/).filter(Boolean).map((p) => p.trim())
}

export function articleDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('en-SG', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'Asia/Singapore' }).format(date)
}

export function articleFailureMessage(failure) {
  const status = failure.response?.status
  if (status === 401) return 'Your session has expired. Please sign in again.'
  if (status === 403) return 'Platform Manager access and a valid session are required.'
  if (status === 413) return 'Cover photo must be 5 MB or smaller.'
  if (status === 400) return failure.response?.data?.message || 'Please check the article and cover photo.'
  return 'Unable to publish the article. Please try again.'
}
