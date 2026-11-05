import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { isDesignerWorkspacePath } from '../src/auth/designerNavigationRules.js'

const read = (file) => readFileSync(new URL(`../src/${file}`, import.meta.url), 'utf8')

test('all nested designer routes highlight My dashboard but public routes do not', () => {
  for (const path of ['/designer', '/designer/portfolio', '/designer/portfolio/new',
    '/designer/portfolio/55/edit', '/designer/security', '/designer/messages',
    '/designer/project-questions', '/designer/company-information', '/designer/change-password']) {
    assert.equal(isDesignerWorkspacePath(path), true, path)
  }
  for (const path of ['/designers', '/designer-other', '/customer', '/explore']) {
    assert.equal(isDesignerWorkspacePath(path), false, path)
  }
  assert.match(read('components/AuthLayout.jsx'), /isDesignerWorkspacePath\(pathname\)/)
})

test('Designer account menu offers Log out only; account security remains on the dashboard', () => {
  const header = read('components/AuthLayout.jsx')
  const menu = header.split('const menuItems = account ? [')[1].split('  function navLink')[0]
  assert.match(menu, /key: 'logout'/)
  assert.doesNotMatch(menu, /Account security|security'|designer\/security/)
  const dashboard = read('pages/DesignerPage.jsx')
  assert.match(dashboard, /Portfolio overview<\/h2>/)
  assert.match(dashboard, /to: '\/designer\/security'/)
})

test('designer tools share one outlined return button and no duplicate bottom portfolio return', () => {
  assert.match(read('components/DesignerReturnButton.jsx'), /<CustomerReturnButton to=\{to\} confirmLeave=\{confirmLeave\}>/)
  for (const file of [
    'pages/DesignerPortfolioPage.jsx', 'pages/CompanyInformationPage.jsx',
    'pages/DesignerProjectQuestionsPage.jsx',
  ]) assert.match(read(file), /<DesignerReturnButton/)
  assert.match(read('pages/MessagesPage.jsx'), /<DesignerReturnButton \/>/)
  assert.doesNotMatch(read('pages/DesignerPortfolioPage.jsx'), /Back to designer workspace/)
  assert.doesNotMatch(read('pages/CompanyInformationPage.jsx'), /navigate\('\/designer'\)\}>Cancel/)
})

test('designer account security uses the customer-approved row layout with only supported actions', () => {
  const hub = read('pages/AccountSecurityPage.jsx')
  assert.match(hub, /<DesignerReturnButton \/>/)
  for (const route of ['account', 'change-email', 'change-password']) {
    assert.match(hub, new RegExp(`to="/designer/${route}"`))
  }
  assert.doesNotMatch(hub, /to="\/designer\/delete-account"/)
  const name = read('pages/DesignerAccountSettingsPage.jsx')
  assert.match(name, /backPath="\/designer\/security"/)
  assert.match(name, /designerReturnButton/)
})

test('new and edit project screens use Back to portfolio and protect unsaved edits', () => {
  for (const name of ['AddPortfolioProjectPage.jsx', 'EditPortfolioProjectPage.jsx']) {
    const page = read(`pages/${name}`)
    assert.match(page, /<DesignerReturnButton to="\/designer\/portfolio" confirmLeave=/)
    assert.match(page, /onValuesChange=\{\(\) => setDirty\(true\)\}/)
    assert.doesNotMatch(page, />Cancel<\/Button>/)
  }
})

test('voluntary designer password form can return to security; mandatory first login cannot bypass gate', () => {
  const page = read('pages/DesignerChangePasswordPage.jsx')
  assert.match(page, /!checking && !firstLogin && <DesignerReturnButton to="\/designer\/security"/)
  assert.match(page, /firstLogin && <Button block type="default"/)
  assert.match(page, /confirmLeave=\{hasUnsavedPassword\}/)
  for (const field of ['currentPassword', 'newPassword', 'confirmPassword']) {
    assert.match(page, new RegExp(`name="${field}"`))
  }
})
