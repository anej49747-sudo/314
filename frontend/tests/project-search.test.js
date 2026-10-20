import test from 'node:test'
import assert from 'node:assert/strict'
import {
  normalizeProjectQuery,
  projectSearchCount,
  projectSearchDescription,
  projectSearchHeading,
  projectSearchParams,
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

test('category selection is combined with the last submitted title query', () => {
  assert.deepEqual(projectSearchParams('', null), {})
  assert.deepEqual(projectSearchParams(' Modern ', null), { q: 'Modern' })
  assert.deepEqual(projectSearchParams('', 12), { categoryId: 12 })
  assert.deepEqual(projectSearchParams(' Modern ', 12), { q: 'Modern', categoryId: 12 })
  assert.deepEqual(projectSearchParams(' ', 'all'), {})
})

test('results heading reflects title-only, category-only, and combined filters', () => {
  assert.equal(projectSearchHeading('', ''), 'All projects')
  assert.equal(projectSearchHeading('Modern', ''), 'Search results')
  assert.equal(projectSearchHeading('', 'Kitchen'), 'Kitchen projects')
  assert.equal(projectSearchHeading('Modern', 'Kitchen'), 'Filtered projects')
})
