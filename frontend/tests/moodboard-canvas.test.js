import test from 'node:test'
import assert from 'node:assert/strict'
import {
  CANVAS_HEIGHT, CANVAS_WIDTH, HISTORY_LIMIT, canvasPayload, editorReducer,
  initialCanvas, moveOrResize, newNote, newPhoto, newSwatch, placeLayer,
  templateLayout, validGeometry, validTitle,
} from '../src/moodboard/canvasRules.js'

test('freeform coordinate transformations clamp to the 900 by 580 canvas', () => {
  const item = newPhoto({ id: 23, label: 'Kitchen', url: '/api/projects/4/images/23' })
  assert.deepEqual([CANVAS_WIDTH, CANVAS_HEIGHT], [900, 580])
  assert.equal(moveOrResize(item, -500, -500).x, 0)
  assert.equal(moveOrResize(item, 10000, 10000).y, 580 - item.height)
  assert.equal(moveOrResize(item, 10000, 10000, 'resize').width, 900 - item.x)
  assert.equal(validGeometry(moveOrResize(item, 10000, 10000, 'resize')), true)
})

test('layering reorders without dropping items', () => {
  const photo = newPhoto({ id: 4, label: 'Room', url: '/api/projects/5/images/4' })
  const note = newNote()
  const board = { title: 'Board', backgroundColor: '#FFFFFF', elements: [photo, note] }
  const raised = placeLayer(board, photo.clientId, 1)
  assert.deepEqual(raised.elements.map(e => e.kind), ['TEXT', 'PHOTO'])
  assert.equal(placeLayer(raised, photo.clientId, 1), raised)
})

test('undo/redo restores exact layout and invalidates redo on next edit', () => {
  const board = { title: 'Bedroom', backgroundColor: '#FFFFFF', elements: [newSwatch()] }
  const fresh = editorReducer({}, { type: 'reset', canvas: board })
  const edited = editorReducer(fresh, { type: 'commit', change: (value) => ({ ...value, title: 'Kitchen' }) })
  assert.equal(edited.canvas.title, 'Kitchen')
  const undone = editorReducer(edited, { type: 'undo' })
  assert.equal(undone.canvas.title, 'Bedroom')
  assert.equal(editorReducer(undone, { type: 'redo' }).canvas.title, 'Kitchen')
  assert.equal(editorReducer(undone, { type: 'commit', change: (value) => ({ ...value, title: 'Lounge' }) }).future.length, 0)
  let history = fresh
  for (let i = 0; i < HISTORY_LIMIT + 9; i++) {
    history = editorReducer(history, { type: 'commit', change: value => ({ ...value, title: `Board ${i}` }) })
  }
  assert.equal(history.past.length, HISTORY_LIMIT)
})

test('canvas payload excludes browser-only properties and preserves spatial order', () => {
  const photo = newPhoto({ id: 8, label: 'Warm room', url: '/api/projects/3/images/8' })
  const note = newNote()
  const board = { title: '  Moodboard  ', backgroundColor: '#FFFFFF', elements: [photo, note] }
  const payload = canvasPayload(board, 2)
  assert.equal(payload.title, 'Moodboard')
  assert.equal(payload.version, 2)
  assert.equal(payload.elements.length, 2)
  assert.equal(payload.elements[0].photoId, 8)
  assert.equal('clientId' in payload.elements[0], false)
  assert.equal('photoUrl' in payload.elements[0], false)
  assert.equal(payload.elements[1].text, note.text)
  assert.throws(() => canvasPayload({ ...board, title: '' }, 2))
  assert.equal(validTitle('   '), false)
})

test('saved board loads photos, text, swatches and templates without erasing annotations', () => {
  const fromServer = { title: 'Bedroom', backgroundColor: '#EAEAEA',
    elements: [{ kind: 'TEXT', text: 'Japandi', color: '#365B82', fontSize: 28,
      x: 40, y: 50, width: 200, height: 100 }] }
  const canvas = initialCanvas(fromServer)
  assert.equal(canvas.elements.length, 1)
  assert.equal(canvas.elements[0].text, 'Japandi')
  for (const name of ['minimal', 'balanced', 'editorial']) {
    const changed = templateLayout(canvas, name)
    assert.equal(changed.elements[0].text, 'Japandi')
    assert.equal(changed.elements.every(validGeometry), true)
  }
})
