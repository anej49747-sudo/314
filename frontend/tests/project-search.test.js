import test from 'node:test'
import assert from 'node:assert/strict'
import {
  normalizeProjectQuery,
  projectSearchCount,
  projectSearchDescription,
  projectSearchPath,
} from '../src/project/projectSearchRules.js'

test('title query is trimmed but preserves the visitor-entered case', () => {
  assert.equal(normalizeProjectQuery('  Modern Kitchen  '), 'Modern Kitchen')
  assert.equal(normalizeProjectQuery('   '), '')
  assert.equal(normalizeProjectQuery(null), '')
})

test('project search count handles no results and a single project', () => {
  assert.equal(projectSearchCount(0), '0 projects')
  assert.equal(projectSearchCount(1), '1 project')
  assert.equal(projectSearchCount(5), '5 projects')
})

test('project cards show descriptions safely and preserve project detail links', () => {
  assert.equal(projectSearchDescription('  Calm living room  '), 'Calm living room')
  assert.equal(projectSearchDescription('  '), 'No project description is available.')
  assert.equal(projectSearchPath(42), '/projects/42')
})
