import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { changeFloor, fitsFloor, hydratePlan, makeElement, metres, patchElement, planReducer,
  rectOverlap, roomOverlapCount, rotateElement, savePayload, shiftElement, snapTo, unsavedChanges,
  validFloor, validPlan } from '../src/design/floorPlanRules.js'

const base = { title: 'Modern layout', floorWidthCm: 1200, floorHeightCm: 800,
  elements: [{ kind: 'ROOM', name: 'Living room', xCm: 0, yCm: 0,
    widthCm: 600, heightCm: 400, rotation: 0, clientId: 'room-1' }] }

test('floor dimensions and geometry validate in centimetres', () => {
  assert.equal(validFloor(400), true)
  assert.equal(validFloor(2500), true)
  assert.equal(validFloor(399), false)
  assert.equal(validFloor(2501), false)
  assert.equal(validPlan(base), true)
  assert.equal(metres(1200), '12')
  assert.equal(metres(1250), '12.5')
  assert.equal(validPlan({ ...base, elements: [{ ...base.elements[0], xCm: 1000 }] }), false)
})

test('snapping, drag and resizing keep elements inside floor', () => {
  const item = { ...base.elements[0], xCm: 750, widthCm: 400 }
  const moved = shiftElement(item, 999, 0, 'move', base)
  assert.equal(moved.xCm, 800)
  const resized = shiftElement(item, 400, 1200, 'resize', base)
  assert.equal(resized.widthCm, 450)
  assert.equal(resized.heightCm, 800)
  assert.equal(snapTo(47), 50)
  assert.equal(snapTo(47, false), 47)
  assert.equal(fitsFloor(resized, base), true)
})

test('overlap warnings ignore furniture and touching edges', () => {
  assert.equal(rectOverlap(base.elements[0], { xCm: 600, yCm: 0, widthCm: 100, heightCm: 400 }), false)
  const touching = { ...base.elements[0], clientId: 'touching', xCm: 600, widthCm: 500 }
  const overlapping = { ...base.elements[0], clientId: 'overlap', xCm: 550, widthCm: 300 }
  const sofa = { ...base.elements[0], clientId: 'sofa', kind: 'FURNITURE' }
  assert.equal(roomOverlapCount({ ...base, elements: [base.elements[0], touching, sofa] }), 0)
  assert.equal(roomOverlapCount({ ...base, elements: [base.elements[0], touching, overlapping] }), 2)
})

test('create furniture, rotate it, and keep rooms axis aligned', () => {
  const item = makeElement('Sofa', base)
  assert.equal(item.kind, 'FURNITURE')
  assert.equal(item.rotation, 0)
  assert.equal(rotateElement(item).rotation, 90)
  assert.equal(rotateElement(base.elements[0]).rotation, 0)
  assert.equal(changeFloor(base, 'floorWidthCm', 400).floorWidthCm, 1200)
})

test('history tracks edits, undo, redo and saved checkpoint', () => {
  const first = planReducer({}, { type: 'reset', plan: base })
  const second = planReducer(first, { type: 'commit', transform: (state) => ({ ...state, title: 'New title' }) })
  assert.equal(unsavedChanges(second), true)
  const undo = planReducer(second, { type: 'undo' })
  assert.equal(undo.current.title, 'Modern layout')
  assert.equal(unsavedChanges(undo), false)
  const redo = planReducer(undo, { type: 'redo' })
  assert.equal(redo.current.title, 'New title')
  assert.equal(unsavedChanges(planReducer(redo, { type: 'saved' })), false)
})

test('save payload strips temporary IDs and cannot accept client-selected version', () => {
  const payload = savePayload(base, 3)
  assert.equal(payload.version, 3)
  assert.equal(payload.elements[0].clientId, undefined)
  assert.deepEqual(Object.keys(payload.elements[0]), ['kind', 'name', 'xCm', 'yCm', 'widthCm', 'heightCm', 'rotation'])
  const hydrated = hydratePlan({ ...base, elements: [{ ...base.elements[0], clientId: undefined }] })
  assert.equal(typeof hydrated.elements[0].clientId, 'string')
  const updated = patchElement(base, 'room-1', (item) => ({ ...item, name: 'Kitchen' }))
  assert.equal(updated.elements[0].name, 'Kitchen')
  assert.equal(base.elements[0].name, 'Living room')
})

test('frontend routes only add approved Customer pages', () => {
  const app = readFileSync(new URL('../src/App.jsx', import.meta.url), 'utf8')
  const page = readFileSync(new URL('../src/pages/CustomerPage.jsx', import.meta.url), 'utf8')
  const security = readFileSync(new URL('../../backend/src/main/java/csit314/renovation/security/SecurityConfiguration.java', import.meta.url), 'utf8')
  assert.match(app, /path="customer\/designs"/)
  assert.match(app, /path="customer\/designs\/:draftId"/)
  assert.match(page, /My design drafts/)
  assert.match(security, /\/api\/customer\/designs/)
})
