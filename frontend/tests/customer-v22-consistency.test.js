import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (file) => readFileSync(new URL(`../src/${file}`, import.meta.url), 'utf8')

test('dashboard and favourites use the same saved-project card component', () => {
  const dashboard = read('pages/CustomerPage.jsx')
  const favourites = read('pages/CustomerFavouritesPage.jsx')
  assert.match(dashboard, /import SavedProjectCard from/)
  assert.match(favourites, /import SavedProjectCard from/)
  assert.match(dashboard, /<SavedProjectCard key=\{project.id\}/)
  assert.match(favourites, /<SavedProjectCard key=\{project.id\}/)
  assert.doesNotMatch(dashboard, /<ImagePreviewCard/)
})

test('shared saved-project cards display classification and support favourite removal', () => {
  const card = read('components/SavedProjectCard.jsx')
  assert.match(card, /<ProjectCardLayout project=\{project\}/)
  assert.match(read('components/ProjectCardLayout.jsx'), /<ProjectAttributes project=\{project\}/)
  assert.match(card, /onClick=\{\(\) => onRemove\(project.id\)\}/)
  assert.match(card, /project\.designer\?\.name/)
})

test('account security has four separate customer actions without redundant subtitles', () => {
  const hub = read('pages/AccountSecurityPage.jsx')
  for (const route of ['account', 'change-email', 'change-password', 'delete-account']) {
    assert.ok(hub.includes(`to="/customer/${route}"`))
  }
  assert.doesNotMatch(hub, /<small>/)
})

test('name editor remains separate from delete account; customer email/name use shared top return button', () => {
  const name = read('pages/CustomerAccountSettingsPage.jsx')
  const email = read('pages/ChangeAccountEmailPage.jsx')
  const form = read('components/AccountSettingsFormPage.jsx')
  assert.doesNotMatch(name, /CustomerDeleteAccountSection/)
  assert.match(name, /customerReturnButton/)
  assert.match(email, /customerReturnButton=\{customer\}/)
  assert.match(form, /customerReturnButton && <CustomerReturnButton/)
  assert.match(form, /!customerReturnButton && !designerReturnButton && <Button onClick=/)
})

test('destructive account deletion is on a distinct guarded customer route', () => {
  const routes = read('App.jsx')
  const deletePage = read('pages/CustomerDeleteAccountPage.jsx')
  assert.match(routes, /path="customer\/delete-account"/)
  assert.match(deletePage, /currentCustomer\(\)/)
  assert.match(deletePage, /verified && !error && <CustomerDeleteAccountSection/)
})
