import test from 'node:test'
import assert from 'node:assert/strict'
import {
  isValidDesignerId,
  profileIntroduction,
  profileProjectCountLabel,
  publicProjectPath,
} from '../src/designer/designerProfileRules.js'

test('public profile route requires a positive integer account id', () => {
  assert.equal(isValidDesignerId('13'), true)
  assert.equal(isValidDesignerId('0'), false)
  assert.equal(isValidDesignerId('-1'), false)
  assert.equal(isValidDesignerId('1/other'), false)
  assert.equal(isValidDesignerId('abc'), false)
  assert.equal(isValidDesignerId('1.5'), false)
  assert.equal(isValidDesignerId('999999999999999999999'), false)
})

test('missing company introduction uses the approved empty state', () => {
  assert.equal(profileIntroduction(null), 'This designer has not added a company introduction yet.')
  assert.equal(profileIntroduction(' '), 'This designer has not added a company introduction yet.')
  assert.equal(profileIntroduction('  Boutique studio  '), 'Boutique studio')
})

test('portfolio count labels handle empty, one and multiple entries', () => {
  assert.equal(profileProjectCountLabel(0), '0 projects')
  assert.equal(profileProjectCountLabel(1), '1 project')
  assert.equal(profileProjectCountLabel(3), '3 projects')
})

test('project link keeps id and routes to the future Story #6 placeholder', () => {
  assert.equal(publicProjectPath(27), '/projects/27')
})
