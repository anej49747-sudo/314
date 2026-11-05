import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')

test('all three customer account-editing pages use a shared outlined top return button', () => {
  const name = read('pages/CustomerAccountSettingsPage.jsx')
  const email = read('pages/ChangeAccountEmailPage.jsx')
  const password = read('pages/CustomerChangePasswordPage.jsx')
  const shared = read('components/AccountSettingsFormPage.jsx')
  assert.match(name, /customerReturnButton/)
  assert.match(email, /customerReturnButton=\{customer\}/)
  assert.match(shared, /customerReturnButton && <CustomerReturnButton to=\{backPath\}/)
  assert.match(password, /<CustomerReturnButton to="\/customer\/security" confirmLeave=\{hasUnsavedPassword\}>/)
  assert.doesNotMatch(password, /ld-password-back-link/)
  assert.match(shared, /!customerReturnButton && !designerReturnButton && <Button onClick=/)
})

test('warning about unsaved edits only appears when the customer form has changes', () => {
  const button = read('components/CustomerReturnButton.jsx')
  const form = read('components/AccountSettingsFormPage.jsx')
  const password = read('pages/CustomerChangePasswordPage.jsx')
  assert.match(button, /if \(!confirmLeave \|\|/)
  assert.match(button, /Discard unsaved changes\?/)
  assert.match(button, /onOk: \(\) => navigate\(to\)/)
  assert.match(form, /confirmLeave=\{changed\}/)
  assert.match(password, /setHasUnsavedPassword\(Object.values\(values\)/)
})

test('designer account editing now uses the same top return pattern without redundant Cancel', () => {
  const form = read('components/AccountSettingsFormPage.jsx')
  const email = read('pages/ChangeAccountEmailPage.jsx')
  assert.match(form, /designerReturnButton && <DesignerReturnButton to=\{backPath\} confirmLeave=\{changed\}>/)
  assert.match(form, /!customerReturnButton && !designerReturnButton && <Button onClick=/)
  assert.match(email, /designerReturnButton=\{!customer\}/)
  assert.match(read('pages/DesignerAccountSettingsPage.jsx'), /designerReturnButton/)
})

test('delete account already uses the same top return button; password form validation remains', () => {
  assert.match(read('pages/CustomerDeleteAccountPage.jsx'), /<CustomerReturnButton to="\/customer\/security">/)
  const password = read('pages/CustomerChangePasswordPage.jsx')
  for (const name of ['currentPassword', 'newPassword', 'confirmPassword']) {
    assert.match(password, new RegExp(`name="${name}"`))
  }
  assert.match(password, /updateOwnAccountPassword/)
  assert.match(password, /Change password<\/Button>/)
})
