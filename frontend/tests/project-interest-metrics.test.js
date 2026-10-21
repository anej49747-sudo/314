import assert from 'node:assert/strict'
import test from 'node:test'
import { interestMetricCount } from '../src/designer/projectInterestRules.js'

test('shows valid project view and favourite totals without altering them', () => {
  assert.equal(interestMetricCount(0), 0)
  assert.equal(interestMetricCount(17), 17)
  assert.equal(interestMetricCount(2), 2)
})

test('missing and invalid metrics are displayed as zero', () => {
  for (const value of [undefined, null, -1, 1.5, NaN, Infinity, '3']) {
    assert.equal(interestMetricCount(value), 0)
  }
})
