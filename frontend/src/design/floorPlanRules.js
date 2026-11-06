/** Story #26: pure, centimetre-based floor-plan geometry, state history and templates. */
export const MIN_FLOOR = 400
export const MAX_FLOOR = 2500
export const MAX_ELEMENTS = 120
export const MAX_DRAFTS = 30
export const HISTORY_LIMIT = 80
export const SNAP_CM = 25

export function uid() {
  return typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID() : `design-${Date.now()}-${Math.random().toString(36).slice(2)}`
}
export const clamp = (n, min, max) => Math.min(max, Math.max(min, n))
export const metres = (cm) => (cm / 100).toFixed(2).replace(/0+$/, '').replace(/\.$/, '')
export const snapTo = (n, enabled = true) => enabled ? Math.round(n / SNAP_CM) * SNAP_CM : Math.round(n)
export const validTitle = (s) => typeof s === 'string' && s.trim().length >= 1 && s.trim().length <= 120
export const validFloor = (n) => Number.isInteger(n) && n >= MIN_FLOOR && n <= MAX_FLOOR

export function hydratePlan(server) {
  return {
    title: server.title, floorWidthCm: server.floorWidthCm,
    floorHeightCm: server.floorHeightCm,
    elements: (server.elements || []).map((e) => ({ ...e, clientId: uid() })),
  }
}
export function savePayload(plan, version) {
  return {
    version, title: plan.title.trim(), floorWidthCm: plan.floorWidthCm,
    floorHeightCm: plan.floorHeightCm,
    elements: plan.elements.map(({ kind, name, xCm, yCm, widthCm, heightCm, rotation }) =>
      ({ kind, name: name.trim(), xCm, yCm, widthCm, heightCm, rotation })),
  }
}
export function rectOverlap(a, b) {
  return Math.min(a.xCm + a.widthCm, b.xCm + b.widthCm) > Math.max(a.xCm, b.xCm) &&
    Math.min(a.yCm + a.heightCm, b.yCm + b.heightCm) > Math.max(a.yCm, b.yCm)
}
export function roomOverlapCount(plan) {
  let count = 0
  const rooms = plan.elements.filter((e) => e.kind === 'ROOM')
  for (let i = 0; i < rooms.length; i += 1) {
    for (let j = i + 1; j < rooms.length; j += 1) {
      if (rectOverlap(rooms[i], rooms[j])) count += 1
    }
  }
  return count
}
export function fitsFloor(e, plan) {
  if (![e.xCm, e.yCm, e.widthCm, e.heightCm, e.rotation].every(Number.isInteger) ||
    e.xCm < 0 || e.yCm < 0 || e.widthCm < 20 || e.heightCm < 20 ||
    e.xCm + e.widthCm > plan.floorWidthCm || e.yCm + e.heightCm > plan.floorHeightCm ||
    e.rotation < 0 || e.rotation >= 360 || (e.kind === 'ROOM' && e.rotation !== 0)) return false
  const radians = e.rotation * Math.PI / 180
  const rotatedWidth = Math.abs(e.widthCm * Math.cos(radians)) + Math.abs(e.heightCm * Math.sin(radians))
  const rotatedHeight = Math.abs(e.widthCm * Math.sin(radians)) + Math.abs(e.heightCm * Math.cos(radians))
  const centerX = e.xCm + e.widthCm / 2
  const centerY = e.yCm + e.heightCm / 2
  return centerX - rotatedWidth / 2 >= -0.001 && centerY - rotatedHeight / 2 >= -0.001 &&
    centerX + rotatedWidth / 2 <= plan.floorWidthCm + 0.001 &&
    centerY + rotatedHeight / 2 <= plan.floorHeightCm + 0.001
}
export function validPlan(plan) {
  return validTitle(plan.title) && validFloor(plan.floorWidthCm) && validFloor(plan.floorHeightCm) &&
    plan.elements.length <= MAX_ELEMENTS && plan.elements.every((e) =>
      ['ROOM', 'FURNITURE', 'OPENING'].includes(e.kind) &&
      typeof e.name === 'string' && e.name.trim().length > 0 && e.name.trim().length <= 80 && fitsFloor(e, plan))
}
export function shiftElement(e, dx, dy, mode, plan, snapping = true) {
  if (mode === 'resize') {
    return { ...e, widthCm: clamp(snapTo(e.widthCm + dx, snapping), 20, plan.floorWidthCm - e.xCm),
      heightCm: clamp(snapTo(e.heightCm + dy, snapping), 20, plan.floorHeightCm - e.yCm) }
  }
  return { ...e, xCm: clamp(snapTo(e.xCm + dx, snapping), 0, plan.floorWidthCm - e.widthCm),
    yCm: clamp(snapTo(e.yCm + dy, snapping), 0, plan.floorHeightCm - e.heightCm) }
}
const LIBRARY = {
  'Living room': ['ROOM', 600, 400], Kitchen: ['ROOM', 300, 250],
  Bedroom: ['ROOM', 350, 300], Bathroom: ['ROOM', 240, 220],
  Sofa: ['FURNITURE', 210, 90], Bed: ['FURNITURE', 210, 190],
  Table: ['FURNITURE', 150, 90], Cabinet: ['FURNITURE', 180, 60],
  Door: ['OPENING', 90, 24], Window: ['OPENING', 140, 24],
}
export const LIBRARY_GROUPS = {
  Rooms: ['Living room', 'Kitchen', 'Bedroom', 'Bathroom'],
  Furniture: ['Sofa', 'Bed', 'Table', 'Cabinet'],
  Openings: ['Door', 'Window'],
}
export function makeElement(name, plan) {
  const [kind, baseW, baseH] = LIBRARY[name] || LIBRARY.Sofa
  const widthCm = Math.min(baseW, plan.floorWidthCm - 20)
  const heightCm = Math.min(baseH, plan.floorHeightCm - 20)
  // Seek an uncrowded location for the new element; keeping furnishings within the floor.
  let chosen = { xCm: 25, yCm: 25 }
  outer: for (let y = 25; y + heightCm <= plan.floorHeightCm; y += 50) {
    for (let x = 25; x + widthCm <= plan.floorWidthCm; x += 50) {
      const candidate = { xCm: x, yCm: y, widthCm, heightCm }
      if (kind !== 'ROOM' || !plan.elements.some((e) => e.kind === 'ROOM' && rectOverlap(candidate, e))) {
        chosen = { xCm: x, yCm: y }
        break outer
      }
    }
  }
  return { clientId: uid(), kind, name, ...chosen, widthCm, heightCm, rotation: 0 }
}
export function patchElement(plan, id, transform) {
  return { ...plan, elements: plan.elements.map((item) => item.clientId === id ? transform(item) : item) }
}
export function rotateElement(item, degrees = 90) {
  return item.kind === 'ROOM' ? item : { ...item, rotation: (item.rotation + degrees) % 360 }
}
export function changeFloor(plan, field, n) {
  const updated = { ...plan, [field]: n }
  // Avoid silently clipping or deleting existing rooms or furniture on a floor-size change.
  return validFloor(n) && plan.elements.every((e) => fitsFloor(e, updated)) ? updated : plan
}
export function planReducer(state, action) {
  if (action.type === 'reset') return { current: action.plan, past: [], future: [], saved: JSON.stringify(action.plan) }
  if (!state.current) return state
  if (action.type === 'undo') {
    if (!state.past.length) return state
    return { ...state, current: state.past.at(-1), past: state.past.slice(0, -1), future: [state.current, ...state.future] }
  }
  if (action.type === 'redo') {
    if (!state.future.length) return state
    return { ...state, current: state.future[0], past: [...state.past, state.current], future: state.future.slice(1) }
  }
  if (action.type === 'saved') return { ...state, saved: JSON.stringify(state.current) }
  if (action.type === 'commit') {
    const next = action.transform(state.current)
    if (JSON.stringify(next) === JSON.stringify(state.current)) return state
    return { ...state, current: next, past: [...state.past, state.current].slice(-HISTORY_LIMIT), future: [] }
  }
  return state
}
export const unsavedChanges = (state) => !!state.current && JSON.stringify(state.current) !== state.saved
