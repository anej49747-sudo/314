import test from 'node:test'
import assert from 'node:assert/strict'
import {
  REPORT_METRICS, reportCsv, selectedReportPeriod, todayInSingapore,
} from '../src/report/activityReportRules.js'

test('Story #32: exactly seven approved user activity metrics', () => {
  assert.deepEqual(REPORT_METRICS.map(({ key }) => key), [
    'logins', 'uniqueUsers', 'newAccounts', 'projectViews',
    'favouritesAdded', 'favouritesRemoved', 'reviewsSubmitted',
  ])
})

test('Story #32: weekly Monday to Sunday even across year boundary', () => {
  assert.deepEqual(selectedReportPeriod('weekly', '2027-01-01'), {
    startDate: '2026-12-28', endDate: '2027-01-03',
  })
})

test('Story #32: leap month and daily boundaries', () => {
  assert.deepEqual(selectedReportPeriod('monthly', '2028-02-29'), {
    startDate: '2028-02-01', endDate: '2028-02-29',
  })
  assert.deepEqual(selectedReportPeriod('daily', '2026-10-22'), {
    startDate: '2026-10-22', endDate: '2026-10-22',
  })
  assert.equal(selectedReportPeriod('daily', '2026-02-30'), null)
})

test('Story #32: Singapore date not machine local date', () => {
  assert.equal(todayInSingapore(new Date('2026-10-21T18:00:00Z')), '2026-10-22')
})

test('Story #32: CSV includes partial data warning and genuine period unique users', () => {
  const d = { date: '2026-10-21', logins: 3, uniqueUsers: 2, newAccounts: 1,
    projectViews: 1, favouritesAdded: 1, favouritesRemoved: 0,
    reviewsSubmitted: 1, engagementComplete: false }
  const data = { ...d, days: [d], frequency: 'daily',
    startDate: '2026-10-21', endDate: '2026-10-21' }
  const csv = reportCsv(data)
  assert.match(csv, /"Period total","3","2"/)
  assert.match(csv, /"1 \(partial\)"/)
  assert.match(csv, /"Incomplete"/)
})
