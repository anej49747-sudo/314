import assert from 'node:assert/strict'
import test from 'node:test'
import {
  SALUTATIONS,
  validatePassword,
  buildRegistrationPayload,
} from '../src/registration/registrationRules.js'

test('salutation options contain only the agreed seven titles', () => {
  assert.deepEqual(SALUTATIONS.map(({ value }) => value), ['Mr', 'Mrs', 'Ms', 'Mdm', 'Mx', 'Dr', 'Prof'])
})

test('password policy accepts a valid 10-character password', () => {
  assert.equal(validatePassword('Welcome1!a'), null)
})

test('password rules reject short, missing uppercase, lowercase, number or symbol', () => {
  assert.match(validatePassword('Aa1!short'), /10–128/)
  assert.match(validatePassword('welcome123!'), /uppercase/)
  assert.match(validatePassword('WELCOME123!'), /lowercase/)
  assert.match(validatePassword('Welcome!!!'), /number/)
  assert.match(validatePassword('Welcome123'), /special/)
})

test('password permits Unicode and spaces and is not trimmed', () => {
  assert.equal(validatePassword('Valid Pass1!'), null)
  const payload = buildRegistrationPayload({ salutation: 'Dr', name: '  Jane Tan  ', email: '  JANE@EXAMPLE.COM  ', password: '  Valid Pass1!  ', confirmPassword: '  Valid Pass1!  ' })
  assert.deepEqual(payload, { salutation: 'Dr', name: 'Jane Tan', email: 'JANE@EXAMPLE.COM', password: '  Valid Pass1!  ' })
  assert.equal(validatePassword('ÉclairTest1!'), null)
})

test('omitted salutation stays null; confirmation and role are never sent', () => {
  const payload = buildRegistrationPayload({ name: 'Studio X', email: 'studio@example.com', password: 'ValidPass1!', confirmPassword: 'ValidPass1!', role: 'USER_ADMIN' })
  assert.deepEqual(payload, { salutation: null, name: 'Studio X', email: 'studio@example.com', password: 'ValidPass1!' })
})
