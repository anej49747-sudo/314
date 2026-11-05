import assert from 'node:assert/strict'
import test from 'node:test'
import { freshReelProjects, reelPhotoIndex } from '../src/discovery/reelRules.js'

test('a reel batch excludes previously shown projects and duplicates within the batch', () => {
  const seen = new Set([1, 3])
  const batch = [{ id: 3 }, { id: 4 }, { id: 4 }, { id: 5 }, { id: 1 }]
  assert.deepEqual(freshReelProjects(seen, batch).map(project => project.id), [4, 5])
  assert.deepEqual([...seen], [1, 3], 'selection does not mutate session history')
})

test('photo gestures stay within a project gallery', () => {
  assert.equal(reelPhotoIndex(0, 1, 3), 1)
  assert.equal(reelPhotoIndex(2, 1, 3), 2)
  assert.equal(reelPhotoIndex(0, -1, 3), 0)
  assert.equal(reelPhotoIndex(4, 0, 0), 0)
})
