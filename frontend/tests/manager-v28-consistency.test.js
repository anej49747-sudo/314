import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

const src = (path) => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')

const managerPages = [
  'ManagerClassificationsPage.jsx',
  'ManagerProjectsPage.jsx',
  'ManagerArticlesPage.jsx',
  'ManagerRecognitionsPage.jsx',
  'ActivityReportsPage.jsx',
]

test('manager subpages all use the same outlined return button', () => {
  const component = src('components/ManagerReturnButton.jsx')
  assert.match(component, /CustomerReturnButton/)
  assert.match(component, /to="\/manager"/)
  assert.match(component, /Back to management dashboard/)
  for (const page of managerPages) {
    const content = src(`pages/${page}`)
    assert.match(content, /import ManagerReturnButton/)
    assert.match(content, /<ManagerReturnButton\s*\/>/)
  }
  assert.doesNotMatch(src('pages/ManagerClassificationsPage.jsx'), />Back to management<\/Link>/)
})

test('management dashboard terminology and classifications are accurate', () => {
  const dashboard = src('pages/ManagerDashboardPage.jsx')
  const header = src('components/AuthLayout.jsx')
  assert.match(header, /navLink\('\/manager', 'Management dashboard', pathname\.startsWith\('\/manager'\)\)/)
  assert.match(dashboard, /<h1 id="manager-dashboard-title">Management dashboard<\/h1>/)
  assert.match(dashboard, /title: 'Project classifications'/)
  assert.match(dashboard, /renovation categories, design styles, property types and floor-area ranges/)
  assert.doesNotMatch(dashboard, /Manage design-style categories/)
})

test('success alerts have one global pale-green semantic treatment', () => {
  const css = src('ui/successNotifications.css')
  const app = src('App.jsx')
  assert.match(app, /import '\.\/ui\/successNotifications\.css'/)
  assert.match(css, /\.ant-alert\.ant-alert-success/)
  assert.match(css, /#effaf2/i)
  assert.match(css, /#b8e8c4/i)
  assert.match(css, /#14532d/i)
  assert.match(css, /#16803c/i)
  assert.match(css, /\.ant-message-notice-content:has\(\.ant-message-success\)/)
  assert.doesNotMatch(css, /\.ant-alert-error\b/)
  assert.doesNotMatch(css, /\.ant-alert-warning\b/)
})
