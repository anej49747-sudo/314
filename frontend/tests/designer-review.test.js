import test from 'node:test'
import assert from 'node:assert/strict'
import {
  MAX_REVIEW_COMMENT_LENGTH,
  buildReviewPayload,
  reviewRatingLabel,
  reviewSubmitError,
  validReviewRating,
} from '../src/designer/designerReviewRules.js'

test('accepts ratings 1 to 5 only, without coercing strings or decimals', () => {
  for (let rating = 1; rating <= 5; rating++) assert.equal(validReviewRating(rating), true)
  for (const value of [0, 6, null, undefined, '5', 3.5, NaN]) {
    assert.equal(validReviewRating(value), false)
  }
})

test('rating alone is valid and optional whitespace comment becomes null', () => {
  assert.deepEqual(buildReviewPayload(5, undefined), { rating: 5, comment: null })
  assert.deepEqual(buildReviewPayload(2, '   '), { rating: 2, comment: null })
  assert.deepEqual(buildReviewPayload(4, '  Good communication  '), {
    rating: 4, comment: 'Good communication',
  })
})

test('invalid rating and oversized comment never generate a payload', () => {
  assert.equal(buildReviewPayload(0, 'comment'), null)
  assert.equal(buildReviewPayload(6, 'comment'), null)
  assert.equal(buildReviewPayload(4, 'x'.repeat(MAX_REVIEW_COMMENT_LENGTH + 1)), null)
  assert.equal(buildReviewPayload(4, 'x'.repeat(MAX_REVIEW_COMMENT_LENGTH)).comment.length,
    MAX_REVIEW_COMMENT_LENGTH)
})

test('feedback differentiates unavailable designers, access and server failures', () => {
  assert.match(reviewSubmitError(404), /no longer available/i)
  assert.match(reviewSubmitError(403), /customers/i)
  assert.match(reviewSubmitError(401), /log in/i)
  assert.match(reviewSubmitError(400, 'Please correct the highlighted fields'), /highlighted fields/)
  assert.match(reviewSubmitError(500), /try again/i)
})

test('rating labels are human-readable', () => {
  assert.equal(reviewRatingLabel(5), 'Excellent')
  assert.match(reviewRatingLabel(0), /Select a rating/)
})
