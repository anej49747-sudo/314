import test from 'node:test'
import assert from 'node:assert/strict'
import { customerLoginDestination } from '../src/project/favouriteRules.js'
import { savedProjectCount, withoutSavedProject } from '../src/project/savedProjectsRules.js'

test('saved-project counts use singular and plural correctly', () => {
  assert.equal(savedProjectCount(0), '0 saved projects')
  assert.equal(savedProjectCount(1), '1 saved project')
  assert.equal(savedProjectCount(3), '3 saved projects')
})

test('removing one favourite keeps the other projects and their original order', () => {
  const projects = [{ id: 8 }, { id: 6 }, { id: 2 }]
  assert.deepEqual(withoutSavedProject(projects, 6), [{ id: 8 }, { id: 2 }])
  assert.deepEqual(withoutSavedProject(projects, 999), projects)
  assert.deepEqual(withoutSavedProject(projects, '8'), [{ id: 6 }, { id: 2 }])
})

test('customer login can return to the saved-project page but rejects unsafe paths', () => {
  assert.equal(customerLoginDestination({ returnTo: '/customer/favourites' }), '/customer/favourites')
  assert.equal(customerLoginDestination({ returnTo: '/customer/favourites/../../admin' }), '/customer')
  assert.equal(customerLoginDestination({ returnTo: '//example.com' }), '/customer')
})
