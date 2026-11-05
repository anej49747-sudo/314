import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  budgetMoney, budgetRange, budgetRequest, defaultBudgetChoices,
  propertyLabel, validateFloorArea, validateWorks,
} from '../src/budget/budgetRules.js'

const src = file => readFileSync(new URL(file, import.meta.url), 'utf8')

test('default selections match the approved V3 mockup', () => {
  const choices = defaultBudgetChoices()
  assert.equal(choices.floorAreaSqm, '95')
  assert.equal(choices.propertyType, 'hdb')
  assert.equal(choices.propertyCondition, 'resale')
  assert.equal(choices.contingencyPercent, 10)
  assert.deepEqual(choices.workTypes, ['painting', 'flooring', 'carpentry', 'electrical'])
  assert.equal(choices.roomIntensities.kitchen, 'extensive')
})

test('area inputs and required renovation selections reject invalid values', () => {
  for (const text of ['', '9', '1000.01', '98.123', '-2', 'foo', '1e3']) {
    assert.ok(validateFloorArea(text), String(text))
  }
  for (const text of ['10', '95', '95.25', '1000']) assert.equal(validateFloorArea(text), '')
  assert.ok(validateWorks({}, ['painting']))
  assert.ok(validateWorks({ kitchen: 'standard' }, []))
  assert.equal(validateWorks({ kitchen: 'standard' }, ['painting']), '')
})

test('money formatting keeps V3 main result prominent and detailed results separate', () => {
  assert.equal(budgetMoney(48519.17, true), 'S$48,500')
  assert.equal(budgetRange(43673.71, 53364.62, true), 'S$43,700 – S$53,400')
  assert.equal(budgetRange(43673.71, 53364.62), 'S$43,674 – S$53,365')
  assert.equal(budgetMoney(null), 'S$—')
  assert.equal(propertyLabel('hdb'), 'HDB flat')
})

test('API request sends choices, not customer supplied cost rates or totals', () => {
  const payload = budgetRequest(defaultBudgetChoices())
  assert.equal(payload.floorAreaSqm, 95)
  assert.equal('baseLow' in payload, false)
  assert.equal('rateLow' in payload, false)
})

test('integration wiring adds only the approved customer route and shortcut', () => {
  assert.match(src('../src/App.jsx'), /path="customer\/budget" element=\{<BudgetEstimatorPage \/>\}/)
  assert.match(src('../src/pages/CustomerPage.jsx'), /to: '\/customer\/budget'/)
  assert.match(src('../src/pages/BudgetEstimatorPage.jsx'), /CustomerReturnButton/)
  assert.match(src('../src/pages/BudgetEstimatorPage.jsx'), /View cost breakdown and assumptions/)
  assert.match(src('../src/api/budget.js'), /csrfHeaders\(\)/)
})
