import assert from 'node:assert/strict'
import test from 'node:test'
import { activeProjectFilters, projectAttributes, sizeRangeLabel } from '../src/discovery/presentationRules.js'

test('manager configured square-metre bounds are visible, including open boundaries', () => {
  assert.equal(sizeRangeLabel({ name: 'Small', min_sqm: '0', max_sqm: '80.00' }), 'Small — Under 80 m²')
  assert.equal(sizeRangeLabel({ name: 'Medium', min_sqm: 80, max_sqm: 120 }), 'Medium — 80 m² to under 120 m²')
  assert.equal(sizeRangeLabel({ name: 'Large', min_sqm: 120, max_sqm: null }), 'Large — 120 m² and above')
  assert.equal(sizeRangeLabel({ name: 'Tiny', max_sqm: 40 }), 'Tiny — Under 40 m²')
  assert.equal(sizeRangeLabel({ name: 'Any' }), 'Any — Any floor area')
})

test('all project classifications have explicit field labels and missing metadata stays omitted', () => {
  const project = { categoryName: 'Living room', metadata: { propertyTypeName: 'HDB', floorAreaSqm: '95.00', styles: [
    { name: 'Japandi', role: 'PRIMARY' }, { name: 'Minimalist', role: 'SECONDARY' },
  ] } }
  assert.deepEqual(projectAttributes(project).map(f => f.label), ['Category', 'Design style', 'Property type', 'Floor area'])
  assert.equal(projectAttributes(project, { secondary: true })[2].value, 'Minimalist')
  assert.deepEqual(projectAttributes({ categoryName: 'Kitchen' }).map(f => f.value), ['Kitchen'])
})

test('active chips include every dimension including individual design styles', () => {
  const chips = activeProjectFilters({ query: 'home', categoryId: 1, categoryName: 'Bedroom', styleIds: [2, 3], styles: [
    { id: 2, name: 'Japandi' }, { id: 3, name: 'Modern' },
  ], propertyTypeId: 4, types: [{ id: 4, name: 'HDB' }], sizeRangeId: 5, sizes: [
    { id: 5, name: 'Medium', min_sqm: 80, max_sqm: 120 },
  ] })
  assert.deepEqual(chips.map(x => x.key), ['title', 'category', 'style-2', 'style-3', 'property', 'size'])
  assert.match(chips.at(-1).label, /80 m² to under 120 m²/)
})
