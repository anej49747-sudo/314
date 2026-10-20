import test from 'node:test'
import assert from 'node:assert/strict'
import { buildUpdatedProjectDetails, validateEditedImages,
  projectEditingFailureMessage } from '../src/designer/projectEditingRules.js'

test('keeps project details and explicitly chosen existing image IDs', () => {
  assert.deepEqual(buildUpdatedProjectDetails({ title: '  Updated room ', description: ' Details  ',
    categoryId: 3, status: 'COMPLETED' }, [5, 8]), {
    title: 'Updated room', description: 'Details', categoryId: 3, status: 'COMPLETED',
    keepImageIds: [5, 8],
  })
})

test('validates total image count, duplicate retained IDs and new files', () => {
  assert.equal(validateEditedImages([1, 2], [{ type: 'image/png', size: 12 }]), '')
  assert.match(validateEditedImages([1, 1], []), /Invalid/)
  assert.match(validateEditedImages([1, 2, 3, 4, 5, 6], [{ type: 'image/png', size: 12 }]), /6 photos/)
  assert.match(validateEditedImages([], [{ type: 'image/svg+xml', size: 20 }]), /JPG/)
})

test('uses safe messages for editing errors', () => {
  assert.match(projectEditingFailureMessage({ response: { status: 404 } }), /no longer available/)
  assert.match(projectEditingFailureMessage({ response: { status: 400, data: { message: 'Invalid' } } }), /Invalid/)
  assert.match(projectEditingFailureMessage({}), /Unable to save/)
})
