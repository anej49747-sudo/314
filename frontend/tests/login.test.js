import test from 'node:test'
import assert from 'node:assert/strict'
import { buildLoginPayload, loginFailureMessage } from '../src/auth/loginRules.js'

test('normalizes email but never changes password text', () => {
  assert.deepEqual(buildLoginPayload({ email: ' JANE@Example.COM ', password: ' PassWord1! ' }), {
    email: 'jane@example.com', password: ' PassWord1! ',
  })
})

test('does not send account IDs or client-controlled roles', () => {
  assert.deepEqual(buildLoginPayload({ email: 'a@example.com', password: 'PassWord1!', userId: 15, role: 'USER_ADMIN' }), {
    email: 'a@example.com', password: 'PassWord1!',
  })
})

test('returns identical errors for invalid email or password authentication failures', () => {
  assert.equal(loginFailureMessage({ response: { status: 401 } }), 'Invalid email or password.')
})

test('handles CSRF and server errors without leaking internal response details', () => {
  assert.match(loginFailureMessage({ response: { status: 403 } }), /refresh/i)
  assert.match(loginFailureMessage({ response: { status: 500, data: { message: 'DB password=secret' } } }), /try again/i)
  assert.doesNotMatch(loginFailureMessage({ response: { status: 500, data: { message: 'DB password=secret' } } }), /secret/i)
})
