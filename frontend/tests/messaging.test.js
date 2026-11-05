import test from 'node:test'
import assert from 'node:assert/strict'
import { initials, MAX_MESSAGE_LENGTH, messageErrorMessage, validDesignerId, validMessageBody } from '../src/messaging/messageRules.js'
import { customerLoginDestination } from '../src/project/favouriteRules.js'

test('validates trimmed text messages and 2000 character limit', () => {
  assert.equal(validMessageBody('  Hello designer  '), true)
  assert.equal(validMessageBody('  '), false)
  assert.equal(validMessageBody(''), false)
  assert.equal(validMessageBody(null), false)
  assert.equal(validMessageBody('a'.repeat(MAX_MESSAGE_LENGTH)), true)
  assert.equal(validMessageBody('a'.repeat(MAX_MESSAGE_LENGTH + 1)), false)
})

test('validates target designer IDs and shows safe initials', () => {
  assert.equal(validDesignerId('21'), true)
  assert.equal(validDesignerId('0'), false)
  assert.equal(validDesignerId('1/2'), false)
  assert.equal(validDesignerId('9007199254740993'), false)
  assert.equal(initials('Northline Interiors'), 'NI')
  assert.equal(initials(' '), '?')
})

test('customer login return path is restricted to a local designer message target', () => {
  assert.equal(customerLoginDestination({ returnTo: '/customer/messages?designerId=42' }), '/customer/messages?designerId=42')
  assert.equal(customerLoginDestination({ returnTo: '/customer/messages?designerId=0' }), '/customer')
  assert.equal(customerLoginDestination({ returnTo: '//example.com' }), '/customer')
  assert.equal(customerLoginDestination({ returnTo: '/customer/messages?designerId=5&next=evil' }), '/customer')
})

test('message API failures have a sensible user-facing explanation', () => {
  assert.match(messageErrorMessage({ response: { status: 404 } }), /no longer available/i)
  assert.match(messageErrorMessage({ response: { status: 403 } }), /permission/i)
})
