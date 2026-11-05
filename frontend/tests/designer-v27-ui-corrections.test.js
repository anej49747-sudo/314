import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const src = (name) => readFileSync(new URL(`../src/${name}`, import.meta.url), 'utf8')

test('designer portfolio create, update and delete confirmations share scoped light success styling', () => {
  const page = src('pages/DesignerPortfolioPage.jsx')
  assert.match(page, /import '\.\.\/ui\/designerV27\.css'/)
  const successAlerts = [...page.matchAll(/<Alert type="success"[\s\S]*?\/>/g)]
  assert.equal(successAlerts.length, 3)
  for (const alert of successAlerts) {
    assert.match(alert[0], /showIcon/)
    assert.match(alert[0], /role="status"/)
    assert.match(alert[0], /className="form-feedback portfolio-success-notice"/)
  }
  assert.match(page, /type="error" showIcon role="alert" message=\{error\} className="form-feedback"/)
  const css = src('ui/designerV27.css')
  assert.match(css, /\.portfolio-page \.portfolio-success-notice\.ant-alert-success/)
  assert.match(css, /background-color: #effaf2/)
  assert.match(css, /border: 1px solid #b8e8c4/)
  assert.match(css, /\.ant-alert-message/)
  assert.match(css, /\.ant-alert-icon/)
})

test('designer account name and email screens remove redundant right-side role chips', () => {
  const name = src('pages/DesignerAccountSettingsPage.jsx')
  const email = src('pages/ChangeAccountEmailPage.jsx')
  assert.doesNotMatch(name, /roleLabel=/)
  assert.doesNotMatch(email, /roleLabel=/)
  assert.match(name, /pageTitle="Change name"/)
  assert.match(name, /designerReturnButton/)
  assert.match(email, /pageTitle="Change email address"/)
  assert.match(email, /designerReturnButton=\{!customer\}/)
  assert.match(email, /customerReturnButton=\{customer\}/)
})
