import test from 'node:test'
import assert from 'node:assert/strict'
import {
  isValidProjectId,
  projectStatusLabel,
  projectDescription,
  projectBackLink,
} from '../src/project/projectDetailsRules.js'

test('public project path IDs require positive safe integers', () => {
  assert.equal(isValidProjectId('27'), true)
  for (const id of ['0', '-2', 'abc', '1.5', '1/2', '999999999999999999999', '']) {
    assert.equal(isValidProjectId(id), false)
  }
})

test('project status displays matching approved badge labels', () => {
  assert.equal(projectStatusLabel('CURRENT'), 'Current')
  assert.equal(projectStatusLabel('COMPLETED'), 'Completed')
})

test('the full project description is preserved and blank descriptions are handled', () => {
  assert.equal(projectDescription('  Materials\n\nScope  '), 'Materials\n\nScope')
  assert.equal(projectDescription(' '), 'No project description is available.')
  assert.equal(projectDescription(null), 'No project description is available.')
})

test('project back navigation respects entry from a designer profile', () => {
  assert.deepEqual(projectBackLink({ designerId: 12 }), {
    to: '/designers/12', label: 'Back to designer profile',
  })
  assert.deepEqual(projectBackLink({ designerId: '8' }), {
    to: '/designers/8', label: 'Back to designer profile',
  })
  assert.deepEqual(projectBackLink(null), {
    to: '/projects', label: 'Back to projects',
  })
  assert.deepEqual(projectBackLink({ designerId: '../login' }), {
    to: '/projects', label: 'Back to projects',
  })
})
