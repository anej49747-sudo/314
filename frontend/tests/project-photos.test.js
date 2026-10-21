import test from 'node:test'
import assert from 'node:assert/strict'
import {
  projectPhotos,
  photoAlt,
  shiftedPhotoIndex,
} from '../src/project/photoGalleryRules.js'

test('gallery uses the public DTO photo ordering, not designer-only links', () => {
  const photos = [
    { id: 17, url: '/api/projects/5/images/17' },
    { id: 11, url: '/api/projects/5/images/11' },
  ]
  assert.deepEqual(projectPhotos({ images: photos }), photos)
  assert.deepEqual(projectPhotos({ images: [] }), [])
  assert.deepEqual(projectPhotos({}), [])
  assert.deepEqual(projectPhotos(null), [])
})

test('photo alt text describes project and its position without inventing captions', () => {
  assert.equal(photoAlt('Kitchen renewal', 0), 'Kitchen renewal, project photo 1')
  assert.equal(photoAlt('Kitchen renewal', 2), 'Kitchen renewal, project photo 3')
})

test('enlarged viewer cycles both directions and handles an empty or single-photo gallery', () => {
  assert.equal(shiftedPhotoIndex(0, 1, 3), 1)
  assert.equal(shiftedPhotoIndex(0, -1, 3), 2)
  assert.equal(shiftedPhotoIndex(2, 1, 3), 0)
  assert.equal(shiftedPhotoIndex(0, 1, 1), 0)
  assert.equal(shiftedPhotoIndex(0, -1, 0), null)
  assert.equal(shiftedPhotoIndex(null, 1, 3), null)
})
