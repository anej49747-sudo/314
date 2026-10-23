import test from 'node:test'
import assert from 'node:assert/strict'
import { articleDate, articleFailureMessage, articleParagraphs, articlePayload,
  filterArticles, validateCover, MAX_COVER_BYTES } from '../src/article/articleRules.js'

const rows = [
  { id: 1, title: 'Budget planning', summary: 'Contingency and costs', content: 'Get clear quotations' },
  { id: 2, title: 'Storage planning', summary: 'Floor plan guidance', content: 'Use vertical storage' },
]

test('visitor article search covers title, summary, body and case-insensitive terms', () => {
  assert.deepEqual(filterArticles(rows, 'BUDGET').map(({ id }) => id), [1])
  assert.deepEqual(filterArticles(rows, 'floor plan').map(({ id }) => id), [2])
  assert.deepEqual(filterArticles(rows, 'quotations').map(({ id }) => id), [1])
  assert.equal(filterArticles(rows, 'not present').length, 0)
  assert.equal(filterArticles(rows, '').length, 2)
})

test('manager publish payload enforces mockup length rules and trims content', () => {
  const valid = { title: '  Renovation guide  ', summary: '  A practical guide for your project.  ',
    content: '   ' + 'Helpful renovations. '.repeat(8) + '   ' }
  assert.deepEqual(articlePayload(valid), { title: 'Renovation guide',
    summary: 'A practical guide for your project.', content: 'Helpful renovations. '.repeat(8).trim() })
  assert.throws(() => articlePayload({ ...valid, title: 'No' }))
  assert.throws(() => articlePayload({ ...valid, summary: 'Short' }))
  assert.throws(() => articlePayload({ ...valid, content: 'Short' }))
  assert.throws(() => articlePayload({ ...valid, title: ' '.repeat(121) }))
})

test('optional cover accepts image types and rejects oversized or invalid images', () => {
  assert.equal(validateCover(null), '')
  assert.equal(validateCover({ type: 'image/png', size: MAX_COVER_BYTES }), '')
  assert.equal(validateCover({ type: 'image/webp', size: 200 }), '')
  assert.equal(validateCover({ type: 'image/jpeg', size: 12 }), '')
  assert.match(validateCover({ type: 'image/svg+xml', size: 10 }), /JPG, PNG or WebP/)
  assert.match(validateCover({ type: 'image/png', size: MAX_COVER_BYTES + 1 }), /5 MB/)
  assert.match(validateCover({ type: 'image/png', size: 0 }), /1 byte/)
})

test('article paragraphs remain plain text without interpreting HTML', () => {
  assert.deepEqual(articleParagraphs(' First part.\n\nSecond part. '), ['First part.', 'Second part.'])
  assert.deepEqual(articleParagraphs('<script>bad()</script>'), ['<script>bad()</script>'])
})

test('dates and publish errors have usable labels', () => {
  assert.match(articleDate('2026-10-20T03:00:00Z'), /20 Oct 2026/)
  assert.match(articleFailureMessage({ response: { status: 403 } }), /Platform Manager/)
  assert.match(articleFailureMessage({ response: { status: 413 } }), /5 MB/)
})
