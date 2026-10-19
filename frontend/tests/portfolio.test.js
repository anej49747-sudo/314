import test from 'node:test'
import assert from 'node:assert/strict'
import { filteredPortfolio, portfolioStatusLabel, portfolioSummary } from '../src/designer/portfolioRules.js'

const projects = [
  { id: 3, title: 'First', status: 'CURRENT' },
  { id: 2, title: 'Second', status: 'COMPLETED' },
  { id: 1, title: 'Third', status: 'CURRENT' },
]

test('summarizes all, current and completed projects', () => {
  assert.deepEqual(portfolioSummary(projects), { total: 3, current: 2, completed: 1 })
  assert.deepEqual(portfolioSummary([]), { total: 0, current: 0, completed: 0 })
})

test('filters a portfolio without modifying the original order', () => {
  assert.deepEqual(filteredPortfolio(projects, 'CURRENT').map((p) => p.id), [3, 1])
  assert.deepEqual(filteredPortfolio(projects, 'COMPLETED').map((p) => p.id), [2])
  assert.deepEqual(filteredPortfolio(projects, 'ALL').map((p) => p.id), [3, 2, 1])
  assert.equal(projects.length, 3)
})

test('provides user-facing project status labels', () => {
  assert.equal(portfolioStatusLabel('CURRENT'), 'Current')
  assert.equal(portfolioStatusLabel('COMPLETED'), 'Completed')
})
