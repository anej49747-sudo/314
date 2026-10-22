import test from 'node:test'
import assert from 'node:assert/strict'
import { canPermanentlyDelete, accountDeletionError } from '../src/account/accountDeletionRules.js'

test('delete requires exact DELETE confirmation', () => {
  assert.equal(canPermanentlyDelete('DELETE'), true)
  assert.equal(canPermanentlyDelete('delete'), false)
  assert.equal(canPermanentlyDelete('DELETE '), false)
  assert.equal(canPermanentlyDelete(''), false)
})

test('account deletion errors do not falsely claim success', () => {
  assert.match(accountDeletionError({ response: { status: 404 } }), /no longer exists/)
  assert.match(accountDeletionError({ response: { status: 403 } }), /no longer authorised/)
  assert.match(accountDeletionError({}), /No successful deletion/)
})
