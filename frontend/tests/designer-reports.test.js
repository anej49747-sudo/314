import test from 'node:test'
import assert from 'node:assert/strict'
import {
  REPORT_REASONS, reportReasonLabel, validateCustomerReport, validateReportDecision,
  reportTimestamp, reportErrorMessage,
} from '../src/dispute/reportRules.js'

test('report reasons match the four approved choices', () => {
  assert.deepEqual(REPORT_REASONS.map((choice) => choice.value), [
    'MISLEADING_INFORMATION', 'INAPPROPRIATE_COMMUNICATION', 'SUSPICIOUS_CONDUCT', 'OTHER',
  ])
  assert.equal(reportReasonLabel('OTHER'), 'Other concern')
})

test('customer report requires approved reason and a 20–2000 character description', () => {
  assert.match(validateCustomerReport('', 'x'.repeat(40)), /reason/)
  assert.match(validateCustomerReport('OTHER', ' short  '), /20/)
  assert.equal(validateCustomerReport('OTHER', 'x'.repeat(20)), '')
  assert.equal(validateCustomerReport('OTHER', 'x'.repeat(2000)), '')
  assert.match(validateCustomerReport('OTHER', 'x'.repeat(2001)), /2,000/)
  assert.match(validateCustomerReport('INVALID', 'x'.repeat(40)), /reason/)
})

test('resolution and dismissal require internal notes from 10–1000 characters', () => {
  assert.match(validateReportDecision('OPEN', 'A long enough note'), /outcome/)
  assert.match(validateReportDecision('RESOLVED', 'short'), /10/)
  assert.equal(validateReportDecision('DISMISSED', 'x'.repeat(10)), '')
  assert.equal(validateReportDecision('RESOLVED', 'x'.repeat(1000)), '')
  assert.match(validateReportDecision('DISMISSED', 'x'.repeat(1001)), /1,000/)
})

test('report timestamps use Singapore time and errors are safe to show', () => {
  assert.match(reportTimestamp('2026-10-22T00:00:00Z'), /08:00/)
  assert.equal(reportTimestamp('not-a-date'), 'Unknown date')
  assert.match(reportErrorMessage({ response: { status: 409 } }), /open report/)
  assert.match(reportErrorMessage({ response: { status: 409 } }, true), /closed/)
})
