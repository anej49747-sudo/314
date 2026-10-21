import test from 'node:test'
import assert from 'node:assert/strict'
import {
  customerLoginDestination,
  favouriteAccessState,
  favouriteButtonLabel,
  favouriteResultMessage,
} from '../src/project/favouriteRules.js'

test('login from Save prompt returns only to a safe local project-details path', () => {
  assert.equal(customerLoginDestination({ returnTo: '/projects/27' }), '/projects/27')
  for (const path of ['https://example.net', '//example.net', '/designer', '/projects/0',
    '/projects/abc', '/projects/1/../admin', '/projects/9999999999999999999999']) {
    assert.equal(customerLoginDestination({ returnTo: path }), '/customer')
  }
  assert.equal(customerLoginDestination(null), '/customer')
})

test('logged-out and non-customer accounts see login prompt, other failures need retry', () => {
  assert.equal(favouriteAccessState(401), 'visitor')
  assert.equal(favouriteAccessState(403), 'visitor')
  assert.equal(favouriteAccessState(500), 'unavailable')
  assert.equal(favouriteAccessState(undefined), 'unavailable')
})

test('saving and removing have distinct clear button and feedback labels', () => {
  assert.equal(favouriteButtonLabel(false), 'Save to Favourites')
  assert.equal(favouriteButtonLabel(true), 'Saved to Favourites')
  assert.equal(favouriteResultMessage(true), 'Project saved to favourites.')
  assert.equal(favouriteResultMessage(false), 'Project removed from favourites.')
})
