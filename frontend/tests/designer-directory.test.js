import test from 'node:test'
import assert from 'node:assert/strict'
import {
  directoryAddress,
  directoryCountLabel,
  directoryDescription,
  designerProfilePath,
} from '../src/designer/designerDirectoryRules.js'

test('directory description falls back when company information is missing', () => {
  assert.equal(directoryDescription(null), 'Company information is not available yet.')
  assert.equal(directoryDescription('  '), 'Company information is not available yet.')
  assert.equal(directoryDescription('  Contemporary projects  '), 'Contemporary projects')
})

test('optional address is shown only when provided', () => {
  assert.equal(directoryAddress(null), '')
  assert.equal(directoryAddress(' '), '')
  assert.equal(directoryAddress('  Singapore  '), 'Singapore')
})

test('count label handles empty and single-entry directories', () => {
  assert.equal(directoryCountLabel(0), '0 designers')
  assert.equal(directoryCountLabel(1), '1 designer')
  assert.equal(directoryCountLabel(8), '8 designers')
})

test('profile navigation keeps the selected stable account ID', () => {
  assert.equal(designerProfilePath(12), '/designers/12')
})
