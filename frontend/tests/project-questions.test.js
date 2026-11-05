import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import {
  answerValidationError, MAX_ANSWER_LENGTH, MAX_QUESTION_LENGTH,
  pageLabel, QA_BROWSE_SIZE, QA_DESIGNER_SIZE, QA_PREVIEW_SIZE,
  questionValidationError,
} from '../src/qa/questionRules.js'

test('question validation trims whitespace and enforces 10–600 character bounds', () => {
  assert.ok(questionValidationError('   '))
  assert.ok(questionValidationError('too short'))
  assert.equal(questionValidationError('  What kind of wood is this?  '), '')
  assert.equal(questionValidationError('x'.repeat(MAX_QUESTION_LENGTH)), '')
  assert.ok(questionValidationError('x'.repeat(MAX_QUESTION_LENGTH + 1)))
})

test('answer validation rejects blank and longer than 2000 characters', () => {
  assert.ok(answerValidationError(' '))
  assert.equal(answerValidationError('  We used quartz. '), '')
  assert.equal(answerValidationError('x'.repeat(MAX_ANSWER_LENGTH)), '')
  assert.ok(answerValidationError('x'.repeat(MAX_ANSWER_LENGTH + 1)))
})

test('preview, browse, designer paging follow approved Q&A design', () => {
  assert.equal(QA_PREVIEW_SIZE, 3)
  assert.equal(QA_BROWSE_SIZE, 6)
  assert.equal(QA_DESIGNER_SIZE, 5)
  assert.equal(pageLabel(0, 0), 'Page 1 of 1')
  assert.equal(pageLabel(2, 4), 'Page 3 of 4')
})

// A populated queue must be able to render its project link. Without a Link
// import React throws a ReferenceError only after the API returns questions.
test('designer question queue imports Link for populated question items', () => {
  const component = readFileSync(
    new URL('../src/pages/DesignerProjectQuestionsPage.jsx', import.meta.url), 'utf8',
  )
  assert.match(component, /import\s*\{[^}]*\bLink\b[^}]*\}\s*from\s*['"]react-router-dom['"]/)
  assert.match(component, /results\.items\.map\(\(item\)\s*=>[\s\S]*?<Link\s+to=\{`\/projects\/\$\{item\.projectId\}`\}/)
})
