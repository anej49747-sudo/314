import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')

test('customer and designer account dropdowns both have Log out only', () => {
  const header = read('components/AuthLayout.jsx')
  const menu = header.split('const menuItems = account ? [')[1].split('  function navLink')[0]
  assert.doesNotMatch(menu, /isCustomer|customer\/preferences|customer\/security/)
  assert.doesNotMatch(menu, /isDesigner|designer\/security/)
  assert.match(menu, /key: 'logout'/)
  assert.match(header, /isCustomerWorkspacePath\(pathname\)/)
})

test('all customer top-level tools use the same accessible button-shaped return link', () => {
  const button = read('components/CustomerReturnButton.jsx')
  assert.match(button, /to = '\/customer'/)
  assert.match(button, /children = 'Back to dashboard'/)
  assert.match(button, /<Link className="ld-customer-return-button" to=\{to\} onClick=\{handleClick\}>/)
  for (const page of [
    'pages/PreferencesPage.jsx',
    'pages/CustomerFavouritesPage.jsx',
    'pages/CompareDesignersPage.jsx',
    'pages/AccountSecurityPage.jsx',
    'pages/MessagesPage.jsx',
  ]) {
    assert.match(read(page), /<CustomerReturnButton\s*\/>/, `${page} must use shared button`)
  }
})

test('both customer and designer messages receive their appropriate return navigation', () => {
  const messages = read('pages/MessagesPage.jsx')
  assert.match(messages, /isCustomer \? <CustomerReturnButton\s*\/> : <DesignerReturnButton\s*\/>/)
  assert.match(messages, /isCustomer \? 'My messages' : 'Customer messages'/)
})

test('preferences place return button at top, but onboarding still uses Skip for now', () => {
  const preferences = read('pages/PreferencesPage.jsx')
  assert.match(preferences, /!onboarding && <CustomerReturnButton\s*\/>/)
  assert.match(preferences, /onboarding && <Button disabled=\{busy\} onClick=\{skip\}>Skip for now<\/Button>/)
  assert.doesNotMatch(preferences, /'Back to dashboard'<\/Button>/)
})

test('delete-account uses the shared return button; customer account editors now use it instead of Cancel', () => {
  const deletion = read('pages/CustomerDeleteAccountPage.jsx')
  assert.match(deletion, /<CustomerReturnButton to="\/customer\/security">Back to account security<\/CustomerReturnButton>/)
  assert.match(deletion, /verified && !error && <CustomerDeleteAccountSection/)
  const editor = read('components/AccountSettingsFormPage.jsx')
  assert.match(editor, /customerReturnButton && <CustomerReturnButton/)
  assert.match(editor, /!customerReturnButton && !designerReturnButton && <Button onClick=/)
  assert.match(read('pages/CustomerAccountSettingsPage.jsx'), /customerReturnButton/)
  assert.match(read('pages/ChangeAccountEmailPage.jsx'), /customerReturnButton=\{customer\}/)
})
