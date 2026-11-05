export const MIN_QUESTION_LENGTH = 10
export const MAX_QUESTION_LENGTH = 600
export const MAX_ANSWER_LENGTH = 2000
export const QA_PREVIEW_SIZE = 3
export const QA_BROWSE_SIZE = 6
export const QA_DESIGNER_SIZE = 5

export function questionValidationError(value) {
  const length = (value ?? '').trim().length
  if (length < MIN_QUESTION_LENGTH) return `Enter at least ${MIN_QUESTION_LENGTH} characters.`
  if (length > MAX_QUESTION_LENGTH) return `Keep your question within ${MAX_QUESTION_LENGTH} characters.`
  return ''
}

export function answerValidationError(value) {
  const length = (value ?? '').trim().length
  if (length === 0) return 'Enter an answer before publishing.'
  if (length > MAX_ANSWER_LENGTH) return `Keep your answer within ${MAX_ANSWER_LENGTH} characters.`
  return ''
}

export function questionDate(value) {
  if (!value) return ''
  const time = new Date(value)
  return Number.isNaN(time.getTime()) ? '' : time.toLocaleDateString(undefined, {
    day: 'numeric', month: 'short', year: 'numeric',
  })
}

export function pageLabel(page, totalPages) {
  return `Page ${page + 1} of ${Math.max(totalPages, 1)}`
}
