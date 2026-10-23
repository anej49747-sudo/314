import test from 'node:test'
import assert from 'node:assert/strict'
import { categoryDeleteFailureMessage } from '../src/category/categoryRules.js'
import { categoryDisplayName, filterManagerProjects, managerProjectFailureMessage,
  managerProjectPayload } from '../src/manager/managerProjectRules.js'

const projects = [
  { id: 1, title: 'Bright Home', designerName: 'Atelier Works', categoryId: 3, categoryName: 'Residential', status: 'CURRENT' },
  { id: 2, title: 'Loft', designerName: 'Studio One', categoryId: null, categoryName: 'Uncategorised', status: 'COMPLETED' },
]

test('uncategorised projects remain displayable and filterable', () => {
  assert.equal(categoryDisplayName({ categoryName: null }), 'Uncategorised')
  assert.deepEqual(filterManagerProjects(projects, { category: 'uncategorised' }).map((p) => p.id), [2])
  assert.deepEqual(filterManagerProjects(projects, { category: '3' }).map((p) => p.id), [1])
})

test('manager can find published projects by title, designer, status', () => {
  assert.deepEqual(filterManagerProjects(projects, { query: 'ATELIER' }).map((p) => p.id), [1])
  assert.deepEqual(filterManagerProjects(projects, { query: 'loft', status: 'COMPLETED' }).map((p) => p.id), [2])
  assert.equal(filterManagerProjects(projects, { status: 'CURRENT' }).length, 1)
})

test('correction payload accepts null categories and only allowed fields', () => {
  assert.deepEqual(managerProjectPayload({ title: '  New title ', description: ' New details ',
    categoryId: 'uncategorised', status: 'COMPLETED', designerId: 999, imageIds: [1] }),
  { title: 'New title', description: 'New details', categoryId: null, status: 'COMPLETED' })
  assert.equal(managerProjectPayload({ title: 'A', description: 'B', status: 'CURRENT', categoryId: 3 }).categoryId, 3)
})

test('corrections validate title, description, category and status', () => {
  assert.throws(() => managerProjectPayload({ title: ' ', description: 'Valid', status: 'CURRENT' }))
  assert.throws(() => managerProjectPayload({ title: 'Valid', description: ' ', status: 'CURRENT' }))
  assert.throws(() => managerProjectPayload({ title: 'Valid', description: 'Valid', status: 'PENDING' }))
  assert.throws(() => managerProjectPayload({ title: 'Valid', description: 'Valid', status: 'CURRENT', categoryId: -1 }))
})

test('category deletion and correction errors are understandable', () => {
  assert.match(categoryDeleteFailureMessage({ response: { status: 404 } }), /no longer exists/)
  assert.match(managerProjectFailureMessage({ response: { status: 403 } }), /Platform Manager/)
})
