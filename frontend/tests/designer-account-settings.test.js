import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildDesignerAccountSettingsPayload,
  designerAccountSettingsChanged,
  designerAccountSettingsFailureMessage,
} from '../src/designer/designerAccountSettingsRules.js'

test('normalizes approved account fields only', () => {
  assert.deepEqual(buildDesignerAccountSettingsPayload({
    displayName: '  Avery Tan  ', email: ' AVERY@Example.COM ', role: 'USER_ADMIN', password: 'ignored',
  }), { displayName: 'Avery Tan', email: 'avery@example.com' })
})

test('requires a name and valid email of permitted length', () => {
  assert.throws(() => buildDesignerAccountSettingsPayload({ displayName: ' ', email: 'valid@example.com' }))
  assert.throws(() => buildDesignerAccountSettingsPayload({ displayName: 'x'.repeat(161), email: 'valid@example.com' }))
  assert.throws(() => buildDesignerAccountSettingsPayload({ displayName: 'Avery', email: 'invalid' }))
  assert.throws(() => buildDesignerAccountSettingsPayload({ displayName: 'Avery', email: 'x'.repeat(251) + '@example.com' }))
})

test('detects actual changes but treats email case and whitespace as equivalent', () => {
  const current = { displayName: 'Avery Tan', email: 'avery@example.com' }
  assert.equal(designerAccountSettingsChanged(null, current), false)
  assert.equal(designerAccountSettingsChanged(current, { displayName: ' Avery Tan ', email: 'AVERY@example.com' }), false)
  assert.equal(designerAccountSettingsChanged(current, { displayName: 'Avery Tan II', email: current.email }), true)
  assert.equal(designerAccountSettingsChanged(current, { displayName: 'Avery Tan', email: 'new@example.com' }), true)
})

test('explains duplicate, invalid, expired and unexpected failures', () => {
  assert.match(designerAccountSettingsFailureMessage({ response: { status: 409 } }), /already exists/i)
  assert.match(designerAccountSettingsFailureMessage({ response: { status: 400 } }), /correct/i)
  assert.match(designerAccountSettingsFailureMessage({ response: { status: 401 } }), /log in again/i)
  assert.match(designerAccountSettingsFailureMessage({ response: { status: 403 } }), /log in again/i)
  assert.match(designerAccountSettingsFailureMessage({}), /try again/i)
})
