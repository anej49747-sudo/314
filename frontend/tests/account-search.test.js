import test from 'node:test'
import assert from 'node:assert/strict'
import {
  INITIAL_ACCOUNT_SEARCH, buildAccountSearchParams, accountRoleLabel,
  accountSearchFailureMessage, accountStatusLabel,
} from '../src/account/accountSearchRules.js'

test('initial search requests first page without filters', () => {
  assert.deepEqual(buildAccountSearchParams(INITIAL_ACCOUNT_SEARCH), { page: 0 })
})

test('search matches submitted name or email with combined role/status filters', () => {
  const criteria = { query: '  ALICE@example.com  ', role: 'CUSTOMER', status: 'inactive' }
  assert.deepEqual(buildAccountSearchParams(criteria, 2), {
    page: 2, q: 'ALICE@example.com', role: 'CUSTOMER', status: 'inactive',
  })
  assert.equal(criteria.query, '  ALICE@example.com  ')
})

test('safe display labels match account type and existing active boolean', () => {
  assert.equal(accountRoleLabel('CUSTOMER'), 'Renovation Customer')
  assert.equal(accountRoleLabel('INTERIOR_DESIGNER'), 'Interior Designer')
  assert.equal(accountStatusLabel(true), 'Active')
  assert.equal(accountStatusLabel(false), 'Inactive')
  assert.equal(accountRoleLabel('USER_ADMIN'), 'Unknown role')
})

test('invalid search input and server errors have safe, actionable messages', () => {
  assert.match(accountSearchFailureMessage({ response: { status: 400 } }), /Invalid search criteria/)
  assert.match(accountSearchFailureMessage({ response: { status: 500 } }), /Unable to load accounts/)
})
