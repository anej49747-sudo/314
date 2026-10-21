import test from 'node:test'
import assert from 'node:assert/strict'
import {
  publicReviewCountLabel,
  publicReviewStars,
  publicReviewDate,
  publicReviewDistribution,
  publicReviewRemainingText,
} from '../src/designer/designerReviewListRules.js'

test('review labels handle zero, one, and multiple reviews', () => {
  assert.equal(publicReviewCountLabel(0), '0 reviews')
  assert.equal(publicReviewCountLabel(1), '1 review')
  assert.equal(publicReviewCountLabel(12), '12 reviews')
})

test('star display distinguishes active and inactive stars', () => {
  assert.deepEqual(publicReviewStars(3), { filled: '★★★', empty: '☆☆' })
  assert.deepEqual(publicReviewStars(5), { filled: '★★★★★', empty: '' })
  assert.deepEqual(publicReviewStars(0), { filled: '', empty: '☆☆☆☆☆' })
})

test('submission timestamps display a predictable readable date', () => {
  assert.equal(publicReviewDate('2026-10-21T08:00:00Z'), '21 Oct 2026')
  assert.equal(publicReviewDate('invalid'), 'Date unavailable')
  assert.equal(publicReviewDate(null), 'Date unavailable')
})

test('rating distribution presents all five ratings, including missing counts', () => {
  assert.deepEqual(publicReviewDistribution({ totalReviews: 4,
    ratingDistribution: { 5: 2, 3: 1, 1: 1 },
  }), [
    { rating: 5, count: 2, percent: 50 },
    { rating: 4, count: 0, percent: 0 },
    { rating: 3, count: 1, percent: 25 },
    { rating: 2, count: 0, percent: 0 },
    { rating: 1, count: 1, percent: 25 },
  ])
  assert.equal(publicReviewDistribution({ totalReviews: 0 })[0].percent, 0)
})

test('show-more label reflects remaining reviews without negative counts', () => {
  assert.equal(publicReviewRemainingText(7, 3), 'Show more reviews (4 remaining)')
  assert.equal(publicReviewRemainingText(3, 3), 'Show more reviews (0 remaining)')
})
