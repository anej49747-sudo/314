import test from 'node:test'
import assert from 'node:assert/strict'
import { accountAreaPath, headerSessionFailure } from '../src/auth/headerSessionRules.js'

test('header navigates to the correct area for each authenticated role', () => {
  assert.equal(accountAreaPath({ role: 'CUSTOMER' }), '/customer')
  assert.equal(accountAreaPath({ role: 'INTERIOR_DESIGNER', passwordChangeRequired: false }), '/designer')
  assert.equal(accountAreaPath({ role: 'INTERIOR_DESIGNER', passwordChangeRequired: true }), '/designer/change-password')
  assert.equal(accountAreaPath({ role: 'USER_ADMIN' }), '/admin/accounts')
  assert.equal(accountAreaPath({ role: 'PLATFORM_MANAGER' }), '/manager')
  assert.equal(accountAreaPath({ role: 'INVALID' }), null)
  assert.equal(accountAreaPath(null), null)
})

test('session errors do not falsely display logged-out links', () => {
  assert.equal(headerSessionFailure(401), 'guest')
  assert.equal(headerSessionFailure(403), 'error')
  assert.equal(headerSessionFailure(500), 'error')
  assert.equal(headerSessionFailure(undefined), 'error')
})
