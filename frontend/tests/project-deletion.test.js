import test from 'node:test'
import assert from 'node:assert/strict'
import { projectDeletionFailureMessage, withoutDeletedProject } from '../src/designer/projectDeletionRules.js'

test('deleting one portfolio project keeps all other projects', () => {
  const projects = [{ id: 1, title: 'First' }, { id: 2, title: 'Second' }]
  assert.deepEqual(withoutDeletedProject(projects, 1), [{ id: 2, title: 'Second' }])
  assert.equal(projects.length, 2)
})

test('deleting an unknown project does not change the list', () => {
  const projects = [{ id: 1 }]
  assert.deepEqual(withoutDeletedProject(projects, 99), projects)
  assert.deepEqual(withoutDeletedProject([], 1), [])
})

test('deletion failure messages do not expose server details', () => {
  assert.match(projectDeletionFailureMessage({ response: { status: 404 } }), /no longer available/)
  assert.match(projectDeletionFailureMessage({ response: { status: 403 } }), /sign in again/)
  assert.match(projectDeletionFailureMessage({ response: { status: 500, data: { message: 'SQL password' } } }), /Unable to delete/)
})
