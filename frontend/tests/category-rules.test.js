import test from 'node:test'
import assert from 'node:assert/strict'
import { buildCategoryPayload, categoryCreationFailureMessage } from '../src/category/categoryRules.js'

test('trims and normalizes category names', () => {
  assert.deepEqual(buildCategoryPayload({ name: '  Modern    Living  ' }), { name: 'Modern    Living' })
  assert.deepEqual(buildCategoryPayload({ name: '  Ｍodern  ' }), { name: 'Modern' })
})

test('rejects blank and oversized names', () => {
  assert.throws(() => buildCategoryPayload({ name: '  ' }))
  assert.throws(() => buildCategoryPayload({ name: 'x'.repeat(101) }))
})

test('explains duplicate name conflicts', () => {
  assert.match(categoryCreationFailureMessage({ response: { status: 409 } }), /already exists/)
})
