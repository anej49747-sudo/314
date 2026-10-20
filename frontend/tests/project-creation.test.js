import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildProjectDetails, PROJECT_IMAGE_MAX_BYTES, validateProjectImages, projectCreationFailureMessage,
} from '../src/designer/projectCreationRules.js'

test('normalizes project details and retains category and status', () => {
  assert.deepEqual(buildProjectDetails({ title: '  Kitchen  ', description: '  New cabinets  ',
    categoryId: 3, status: 'CURRENT' }), {
    title: 'Kitchen', description: 'New cabinets', categoryId: 3, status: 'CURRENT',
  })
})

test('accepts optional images and common web image formats', () => {
  assert.equal(validateProjectImages([]), '')
  assert.equal(validateProjectImages([{ type: 'image/png', size: 234 }]), '')
  assert.equal(validateProjectImages([{ type: 'image/webp', size: PROJECT_IMAGE_MAX_BYTES }]), '')
})

test('limits image count, file types and size', () => {
  assert.match(validateProjectImages(Array(7).fill({ type: 'image/jpeg', size: 50 })), /6 photos/)
  assert.match(validateProjectImages([{ type: 'text/html', size: 40 }]), /JPG/)
  assert.match(validateProjectImages([{ type: 'image/png', size: 0 }]), /5 MB/)
  assert.match(validateProjectImages([{ type: 'image/png', size: PROJECT_IMAGE_MAX_BYTES + 1 }]), /5 MB/)
})

test('formats user-safe creation failures', () => {
  assert.match(projectCreationFailureMessage({ response: { status: 400, data: { message: 'Invalid category' } } }), /Invalid category/)
  assert.match(projectCreationFailureMessage({}), /Unable to create/)
})
