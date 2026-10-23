import test from 'node:test'
import assert from 'node:assert/strict'
import { compactDesignerAwards, designerAwards, isFeaturedProject, normalizeAwardTitle,
  recognitionError, singaporeMonth, validFeatureMonth } from '../src/recognition/recognitionRules.js'

const awards = [
  { id: 5, designerId: 7, title: 'Best Space Planning' },
  { id: 4, designerId: 7, title: 'Client Choice 2026' },
  { id: 3, designerId: 8, title: 'Best Kitchen' },
  { id: 2, designerId: 7, title: 'Sustainable Design' },
]

test('directory renders only two awards and a precise overflow count', () => {
  assert.deepEqual(compactDesignerAwards(awards, 7).displayed.map((item) => item.id), [5, 4])
  assert.equal(compactDesignerAwards(awards, 7).overflow, 1)
  assert.equal(compactDesignerAwards(awards, 8).overflow, 0)
  assert.deepEqual(compactDesignerAwards(awards, 999), { displayed: [], overflow: 0 })
})

test('profiles show every award for the selected designer only', () => {
  assert.deepEqual(designerAwards(awards, '7').map((a) => a.title),
    ['Best Space Planning', 'Client Choice 2026', 'Sustainable Design'])
})

test('award title is only required field and normalizes spaces', () => {
  assert.equal(normalizeAwardTitle('  Best   Space  Planning '), 'Best Space Planning')
  assert.equal(normalizeAwardTitle('X'.repeat(70)).length, 70)
  assert.throws(() => normalizeAwardTitle('ab'))
  assert.throws(() => normalizeAwardTitle('x'.repeat(71)))
  assert.throws(() => normalizeAwardTitle(null))
})

test('valid month and Singapore boundary are stable', () => {
  assert.equal(validFeatureMonth('2026-10'), true)
  assert.equal(validFeatureMonth('2026-13'), false)
  assert.equal(validFeatureMonth('2026-1'), false)
  assert.equal(singaporeMonth(new Date('2026-10-31T16:30:00Z')), '2026-11')
})

test('current project badge only belongs to the featured project', () => {
  assert.equal(isFeaturedProject({ projectId: 12 }, '12'), true)
  assert.equal(isFeaturedProject({ projectId: 12 }, '13'), false)
  assert.equal(isFeaturedProject(null, 12), false)
})

test('recognition failure reasons are actionable', () => {
  assert.match(recognitionError({ response: { status: 409 } }), /already has an award/)
  assert.match(recognitionError({ response: { status: 403 } }), /Platform Manager/)
  assert.match(recognitionError({ response: { status: 404 } }), /no longer exists/)
})
