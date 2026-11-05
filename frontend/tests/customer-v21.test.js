import test from 'node:test'
import assert from 'node:assert/strict'
import { homeFooterAction, isCustomerWorkspacePath } from '../src/auth/customerNavigationRules.js'

test('customer dashboard stays active on every nested customer page', () => {
  for (const path of ['/customer', '/customer/security', '/customer/change-email', '/customer/change-password',
    '/customer/account', '/customer/favourites', '/customer/compare', '/customer/messages', '/customer/preferences/onboarding']) {
    assert.equal(isCustomerWorkspacePath(path), true, path)
  }
})
test('public or similarly prefixed paths are not customer dashboard', () => {
  for (const path of ['/', '/projects/12', '/explore', '/customerish', '/customer-designer', '/designer/security']) {
    assert.equal(isCustomerWorkspacePath(path), false, path)
  }
})
test('only confirmed guests see account registration on home', () => {
  assert.deepEqual(homeFooterAction('guest', null), { to: '/register', label: 'Create your free account' })
  assert.deepEqual(homeFooterAction('authenticated', '/customer'), { to: '/customer', label: 'My dashboard' })
  assert.deepEqual(homeFooterAction('authenticated', '/designer'), { to: '/designer', label: 'My dashboard' })
  assert.deepEqual(homeFooterAction('authenticated', '/manager'), { to: '/manager', label: 'My workspace' })
  assert.deepEqual(homeFooterAction('loading', null), { to: '/projects', label: 'Browse projects' })
  assert.deepEqual(homeFooterAction('error', null), { to: '/projects', label: 'Browse projects' })
})
