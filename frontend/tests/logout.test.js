import test from 'node:test'
import assert from 'node:assert/strict'
import { logoutFailureMessage } from '../src/auth/logoutRules.js'

test('logout failure uses a safe generic message for network and server errors', () => {
  const privateError = { response: { status: 500, data: { message: 'DB password=secret' } } }
  assert.equal(logoutFailureMessage(privateError), 'Unable to log out. Please try again.')
  assert.equal(logoutFailureMessage(null), 'Unable to log out. Please try again.')
  assert.doesNotMatch(logoutFailureMessage(privateError), /secret/)
})

test('logout with an invalid CSRF token suggests refreshing the page', () => {
  assert.match(logoutFailureMessage({ response: { status: 403 } }), /refresh/i)
})
