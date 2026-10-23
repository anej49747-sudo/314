import test from 'node:test'
import assert from 'node:assert/strict'
import {
  moderationActionLabel, loginOutcomeLabel, loginHistoryParams,
  singaporeLoginTime, moderationErrorMessage, historyErrorMessage,
} from '../src/account/accountManagementRules.js'

test('active and inactive accounts expose correct moderation actions', () => {
  assert.equal(moderationActionLabel(true), 'Suspend')
  assert.equal(moderationActionLabel(false), 'Reactivate')
})

test('login outcome uses visible non-secret result text', () => {
  assert.equal(loginOutcomeLabel(true), 'Successful')
  assert.equal(loginOutcomeLabel(false), 'Failed')
  assert.deepEqual(loginHistoryParams('failed', 2), { outcome: 'failed', page: 2 })
})

test('timestamps are consistently displayed in Singapore time', () => {
  const displayed = singaporeLoginTime('2026-10-22T10:00:00Z')
  assert.match(displayed, /18:00/)
  assert.equal(singaporeLoginTime('invalid'), 'Unknown time')
})

test('moderation conflicts and login history errors have safe messages', () => {
  assert.match(moderationErrorMessage({ response: { status: 409 } }), /status changed/i)
  assert.match(moderationErrorMessage({ response: { status: 404 } }), /no longer exists/i)
  assert.match(historyErrorMessage({ response: { status: 404 } }), /no longer available/i)
})
