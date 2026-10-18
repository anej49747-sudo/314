import test from 'node:test'
import assert from 'node:assert/strict'
import { buildDesignerPayload, designerCreationFailureMessage } from '../src/designer/designerAccountRules.js'

test('admin creation payload contains only identity fields, never role or passwords', () => {
  const payload = buildDesignerPayload({ salutation: 'Dr', name: '  Studio Co  ',
    email: '  studio@example.com ', role: 'USER_ADMIN', active: false, password: 'secret' })
  assert.deepEqual(payload, { salutation: 'Dr', name: 'Studio Co', email: 'studio@example.com' })
})

test('designer creation maps duplicate, validation, and session errors', () => {
  assert.match(designerCreationFailureMessage({ response: { status: 409 } }), /already exists/i)
  assert.match(designerCreationFailureMessage({ response: { status: 400 } }), /correct/i)
  assert.match(designerCreationFailureMessage({ response: { status: 403 } }), /access is denied/i)
})
