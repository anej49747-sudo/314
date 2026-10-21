import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildAccountSettingsPayload,
  accountSettingsChanged,
  accountSettingsFailureMessage,
} from '../src/account/accountSettingsRules.js'

test('customer account changes contain only normalized name/email', () => {
  assert.deepEqual(buildAccountSettingsPayload({
    displayName: '  Mei Lim ', email: ' MEI@Example.COM ', role: 'USER_ADMIN', password: 'ignored',
  }), { displayName: 'Mei Lim', email: 'mei@example.com' })
})

test('customer account validation rejects blank, oversized or invalid fields', () => {
  assert.throws(() => buildAccountSettingsPayload({ displayName: ' ', email: 'mei@example.com' }))
  assert.throws(() => buildAccountSettingsPayload({ displayName: 'x'.repeat(161), email: 'mei@example.com' }))
  assert.throws(() => buildAccountSettingsPayload({ displayName: 'Mei', email: 'wrong' }))
  assert.throws(() => buildAccountSettingsPayload({ displayName: 'Mei', email: 'x'.repeat(250) + '@example.com' }))
})

test('case/whitespace-only differences do not count as changed email', () => {
  const current = { displayName: 'Mei Lim', email: 'mei@example.com' }
  assert.equal(accountSettingsChanged(null, current), false)
  assert.equal(accountSettingsChanged(current, { displayName: ' Mei Lim ', email: 'MEI@example.com' }), false)
  assert.equal(accountSettingsChanged(current, { displayName: 'Mei New', email: 'mei@example.com' }), true)
  assert.equal(accountSettingsChanged(current, { displayName: 'Mei Lim', email: 'new@example.com' }), true)
})

test('customer settings shows safe errors for duplicate emails and expired sessions', () => {
  assert.match(accountSettingsFailureMessage({ response: { status: 409 } }), /already exists/i)
  assert.match(accountSettingsFailureMessage({ response: { status: 400 } }), /correct/i)
  assert.match(accountSettingsFailureMessage({ response: { status: 401 } }), /log in again/i)
  assert.match(accountSettingsFailureMessage({ response: { status: 403 } }), /log in again/i)
  assert.match(accountSettingsFailureMessage({}), /try again/i)
})
