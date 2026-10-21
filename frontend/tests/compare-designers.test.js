import test from 'node:test'
import assert from 'node:assert/strict'
import {
  comparisonText,
  MAX_COMPARE_DESIGNERS,
  MIN_COMPARE_DESIGNERS,
  normalizeComparisonIds,
  portfolioPreviewProjects,
  projectStatusCounts,
  toggleComparisonId,
} from '../src/designer/compareDesignersRules.js'

test('comparison permits two or three designers with distinct positive account IDs', () => {
  assert.equal(MIN_COMPARE_DESIGNERS, 2)
  assert.equal(MAX_COMPARE_DESIGNERS, 3)
  assert.deepEqual(normalizeComparisonIds([1, '2', '2', '3', '4']), ['1', '2', '3'])
  assert.deepEqual(normalizeComparisonIds(['0', '-4', 'abc', null, '5']), ['5'])
})

test('selection rejects designers absent from the active directory', () => {
  assert.deepEqual(normalizeComparisonIds(['1', '2', '3'], [1, 3]), ['1', '3'])
  assert.deepEqual(toggleComparisonId(['1'], '2', [1, 3]), { ids: ['1'], limitReached: false })
})

test('selection enforces maximum, allows removals and does not duplicate selections', () => {
  const available = [1, 2, 3, 4]
  assert.deepEqual(toggleComparisonId(['1', '2'], 3, available), {
    ids: ['1', '2', '3'], limitReached: false,
  })
  assert.deepEqual(toggleComparisonId(['1', '2', '3'], 4, available), {
    ids: ['1', '2', '3'], limitReached: true,
  })
  assert.deepEqual(toggleComparisonId(['1', '2', '3'], 2, available), {
    ids: ['1', '3'], limitReached: false,
  })
})

test('comparison safely handles missing display fields and blank strings', () => {
  assert.equal(comparisonText(null), 'Not provided')
  assert.equal(comparisonText(' '), 'Not provided')
  assert.equal(comparisonText('  Calm interiors  '), 'Calm interiors')
})

test('project counts use current and completed portfolio statuses independently', () => {
  const projects = [
    { id: 8, status: 'CURRENT' },
    { id: 9, status: 'COMPLETED' },
    { id: 10, status: 'COMPLETED' },
  ]
  assert.deepEqual(projectStatusCounts(projects), { total: 3, current: 1, completed: 2 })
  assert.deepEqual(projectStatusCounts([]), { total: 0, current: 0, completed: 0 })
  assert.deepEqual(portfolioPreviewProjects(projects), projects.slice(0, 2))
  assert.deepEqual(portfolioPreviewProjects(null), [])
})
