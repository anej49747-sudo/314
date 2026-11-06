/** Story #28: coordinate model, history and deterministic templates. No browser/React dependencies. */
export const CANVAS_WIDTH = 900
export const CANVAS_HEIGHT = 580
export const MAX_ELEMENTS = 60
export const MAX_BOARDS = 30
export const HISTORY_LIMIT = 50

export const initialCanvas = (board) => ({
  title: board.title,
  backgroundColor: board.backgroundColor || '#FFFFFF',
  elements: (board.elements || []).map((item) => ({
    ...item,
    clientId: uniqueId(),
  })),
})

export function uniqueId() {
  return typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID() : `board-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export function clamp(value, min, max) { return Math.max(min, Math.min(max, value)) }
export function validHex(value) { return /^#[0-9a-f]{6}$/i.test(value || '') }
export function validTitle(value) { return typeof value === 'string' && value.trim().length >= 1 && value.trim().length <= 120 }

export function validGeometry(item) {
  return ['x', 'y', 'width', 'height'].every((key) => Number.isInteger(item[key])) &&
    item.x >= 0 && item.y >= 0 && item.width >= 55 && item.height >= 45 &&
    item.x + item.width <= CANVAS_WIDTH && item.y + item.height <= CANVAS_HEIGHT
}

export function moveOrResize(item, deltaX, deltaY, mode = 'move') {
  if (mode === 'resize') {
    return { ...item,
      width: clamp(Math.round(item.width + deltaX), 55, CANVAS_WIDTH - item.x),
      height: clamp(Math.round(item.height + deltaY), 45, CANVAS_HEIGHT - item.y),
    }
  }
  return { ...item,
    x: clamp(Math.round(item.x + deltaX), 0, CANVAS_WIDTH - item.width),
    y: clamp(Math.round(item.y + deltaY), 0, CANVAS_HEIGHT - item.height),
  }
}

export function newPhoto(photo, position = { x: 70, y: 65 }) {
  return {
    kind: 'PHOTO', clientId: uniqueId(), photoId: photo.id,
    photoUrl: photo.url, sourceLabel: photo.label,
    x: position.x, y: position.y, width: 290, height: 215,
  }
}
export function newNote() {
  return { kind: 'TEXT', clientId: uniqueId(), text: 'My design inspiration',
    color: '#365B82', fontSize: 25, x: 345, y: 170, width: 235, height: 100 }
}
export function newSwatch(color = '#C8B99F') {
  return { kind: 'SWATCH', clientId: uniqueId(), color,
    x: 680, y: 330, width: 105, height: 105 }
}

export function updateItem(canvas, id, transform) {
  return { ...canvas, elements: canvas.elements.map((item) => item.clientId === id ? transform(item) : item) }
}
export function placeLayer(canvas, id, movement) {
  const items = [...canvas.elements]
  const from = items.findIndex((e) => e.clientId === id)
  if (from === -1 || from + movement < 0 || from + movement >= items.length) return canvas
  const [item] = items.splice(from, 1)
  items.splice(from + movement, 0, item)
  return { ...canvas, elements: items }
}
export function templateLayout(canvas, name) {
  const elements = canvas.elements.map((item) => ({ ...item }))
  if (name === 'minimal') {
    elements.forEach((el, index) => {
      const width = el.kind === 'PHOTO' ? 340 : 180
      const height = el.kind === 'PHOTO' ? 350 : 105
      Object.assign(el, { x: clamp(60 + (index % 3) * 265, 0, 900 - width),
        y: clamp(65 + Math.floor(index / 3) * 135, 0, 580 - height), width, height })
    })
  } else if (name === 'balanced') {
    elements.forEach((el, index) => {
      const width = 250, height = el.kind === 'PHOTO' ? 220 : 95
      Object.assign(el, { x: 45 + (index % 3) * 290,
        y: clamp(40 + Math.floor(index / 3) * 245, 0, 580 - height), width, height })
    })
  } else if (name === 'editorial') {
    elements.forEach((el, index) => {
      const width = index % 3 === 0 ? 370 : 240
      const height = index % 3 === 0 ? 300 : 180
      Object.assign(el, { x: clamp(45 + (index % 3) * 275, 0, 900 - width),
        y: clamp(45 + Math.floor(index / 3) * 180, 0, 580 - height), width, height })
    })
  }
  return { ...canvas, elements }
}

export function editorReducer(state, action) {
  if (action.type === 'reset') return { canvas: action.canvas, past: [], future: [] }
  if (action.type === 'commit') {
    const next = typeof action.change === 'function' ? action.change(state.canvas) : action.canvas
    if (next === state.canvas) return state
    return { canvas: next, past: [...state.past, state.canvas].slice(-HISTORY_LIMIT), future: [] }
  }
  if (action.type === 'preview') return { ...state, canvas: action.canvas }
  if (action.type === 'recordGesture') {
    if (JSON.stringify(action.before.elements) === JSON.stringify(state.canvas.elements)) return state
    return { ...state, past: [...state.past, action.before].slice(-HISTORY_LIMIT), future: [] }
  }
  if (action.type === 'undo' && state.past.length) {
    return { canvas: state.past.at(-1), past: state.past.slice(0, -1),
      future: [state.canvas, ...state.future].slice(0, HISTORY_LIMIT) }
  }
  if (action.type === 'redo' && state.future.length) {
    return { canvas: state.future[0], past: [...state.past, state.canvas].slice(-HISTORY_LIMIT),
      future: state.future.slice(1) }
  }
  return state
}

export function canvasPayload(canvas, version) {
  if (!validTitle(canvas.title) || !validHex(canvas.backgroundColor) ||
    canvas.elements.length > MAX_ELEMENTS || !canvas.elements.every(validGeometry)) {
    throw new Error('Check the board title and make sure elements remain inside the canvas.')
  }
  return {
    version, title: canvas.title.trim(), backgroundColor: canvas.backgroundColor,
    elements: canvas.elements.map(({ kind, photoId, sourceLabel, text, color, fontSize,
      x, y, width, height }) => ({
      kind, photoId: kind === 'PHOTO' ? (photoId || null) : null, sourceLabel,
      text, color, fontSize, x, y, width, height,
    })),
  }
}
