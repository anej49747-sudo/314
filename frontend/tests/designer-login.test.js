import test from 'node:test'
import assert from 'node:assert/strict'
import {
  designerLoginDestination,
  buildPasswordChangePayload,
  validatePasswordChange,
  passwordChangeFailureMessage,
} from '../src/designer/designerLoginRules.js'

test('designer with temporary credentials must change password', () => {
  assert.equal(designerLoginDestination({ role: 'INTERIOR_DESIGNER', passwordChangeRequired: true }), '/designer/change-password')
  assert.equal(designerLoginDestination({ role: 'INTERIOR_DESIGNER', passwordChangeRequired: false }), '/designer')
  assert.equal(designerLoginDestination({ role: 'CUSTOMER' }), null)
})

test('password change never includes client-supplied user identifiers', () => {
  assert.deepEqual(buildPasswordChangePayload({ newPassword: 'NewPassword1!', confirmPassword: 'NewPassword1!', currentPassword: 'OldPassword1!', userId: 15, role: 'USER_ADMIN' }), {
    newPassword: 'NewPassword1!',
  })
})

test('first-login password setup rejects weak passwords and mismatched confirmation', () => {
  assert.match(validatePasswordChange({ newPassword: 'short', confirmPassword: 'short' }), /10/)
  assert.match(validatePasswordChange({ newPassword: 'NewPassword1!', confirmPassword: 'Different1!' }), /match/i)
  assert.equal(validatePasswordChange({ newPassword: 'NewPassword1!', confirmPassword: 'NewPassword1!' }), null)
})

test('error messages avoid exposing backend internals', () => {
  assert.match(passwordChangeFailureMessage({ response: { status: 401 } }), /session|log in/i)
  assert.match(passwordChangeFailureMessage({ response: { status: 403 } }), /session|refresh/i)
  const error = passwordChangeFailureMessage({ response: { status: 500, data: { message: 'DB secret' } } })
  assert.doesNotMatch(error, /secret/i)
})
