import { Alert, Button, Input, InputNumber, Modal, Spin } from 'antd'
import { Download, Grid3X3, Redo2, RotateCw, Save, Trash2, Undo2 } from 'lucide-react'
import { useCallback, useEffect, useReducer, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import CustomerReturnButton from '../components/CustomerReturnButton.jsx'
import { getDesignDraft, saveDesignDraft } from '../api/designDrafts.js'
import { exportFloorPlan } from '../design/exportFloorPlan.js'
import { changeFloor, clamp, fitsFloor, hydratePlan, LIBRARY_GROUPS, makeElement,
  MAX_ELEMENTS, metres, patchElement, planReducer, roomOverlapCount, rotateElement,
  savePayload, shiftElement, unsavedChanges, validPlan } from '../design/floorPlanRules.js'
import '../ui/designDrafts.css'

const INITIAL_STATE = { current: null, past: [], future: [], saved: null }
const OVERLAP_MESSAGE = 'Some room rectangles overlap. Adjust the layout before relying on the plan.'
function errorMessage(err, fallback) {
  if (err.response?.status === 409) return 'This design was changed in another session. Reload before saving.'
  return err.response?.data?.message || fallback
}

function PlanItem({ item, selected, onStart, onSelect }) {
  const { xCm: x, yCm: y, widthCm: w, heightCm: h, rotation, kind, name } = item
  const fill = kind === 'ROOM' ? '#f7f8f7' : kind === 'FURNITURE' ? '#d7e5ee' : '#c9d5dc'
  const stroke = selected ? '#2f5d86' : kind === 'ROOM' ? '#657787' : '#678097'
  return <g role="button" tabIndex={0} aria-pressed={selected} aria-label={`${kind.toLowerCase()} ${name}`}
    style={{ cursor: 'grab', touchAction: 'none' }}
    onPointerDown={(event) => onStart(event, item, 'move')}
    onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); onSelect(item.clientId) } }}>
    <g transform={rotation && kind !== 'ROOM' ? `rotate(${rotation} ${x + w / 2} ${y + h / 2})` : undefined}>
      <rect x={x} y={y} width={w} height={h} rx={kind === 'ROOM' ? 1 : 7}
        fill={fill} stroke={stroke} strokeWidth={kind === 'ROOM' ? 5 : selected ? 4 : 2.5} />
      {kind === 'FURNITURE' && <rect x={x + 8} y={y + 8} width={Math.max(1, w - 16)}
        height={Math.max(1, h - 16)} rx={6} fill="none" stroke="#97adbd" strokeWidth="1.5" />}
      {kind === 'OPENING' && <line x1={x + 5} y1={y + h / 2} x2={x + w - 5} y2={y + h / 2}
        stroke="#365b82" strokeWidth="3" strokeDasharray={name === 'Window' ? '12 5' : 'none'} />}
      <text x={x + w / 2} y={y + h / 2 - (kind === 'ROOM' ? 9 : 0)} fill="#233c54"
        fontFamily="Arial, sans-serif" fontSize={Math.max(11, Math.min(18, w / 8))}
        fontWeight="600" textAnchor="middle" dominantBaseline="central" pointerEvents="none">
        {name.length > 19 ? `${name.slice(0, 17)}…` : name}
      </text>
      {kind === 'ROOM' && <text x={x + w / 2} y={y + h / 2 + 16} textAnchor="middle"
        fontFamily="Arial, sans-serif" fontSize="14" fill="#526581" pointerEvents="none">
        {(w * h / 10000).toFixed(1)} m²
      </text>}
      {selected && <g onPointerDown={(event) => { event.stopPropagation(); onStart(event, item, 'resize') }}
        aria-label={`Resize ${name}`} style={{ cursor: 'nwse-resize' }}>
        <rect x={x + w - 10} y={y + h - 10} width="20" height="20" rx="3"
          fill="#fff" stroke="#365b82" strokeWidth="3" />
      </g>}
    </g>
  </g>
}

export default function DesignDraftEditorPage() {
  const { draftId } = useParams()
  const navigate = useNavigate()
  const [history, dispatch] = useReducer(planReducer, INITIAL_STATE)
  const [version, setVersion] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [snap, setSnap] = useState(true)
  const [zoom, setZoom] = useState(100)
  const [preview, setPreview] = useState(null)
  const planSvg = useRef(null)
  const gesture = useRef(null)
  const plan = history.current
  const dirty = unsavedChanges(history)
  const selected = plan?.elements.find((el) => el.clientId === selectedId)
  const overlaps = plan ? roomOverlapCount(plan) : 0

  useEffect(() => {
    let active = true
    getDesignDraft(draftId).then((data) => {
      if (!active) return
      dispatch({ type: 'reset', plan: hydratePlan(data) })
      setVersion(data.version)
      setSelectedId(null)
    }).catch((err) => {
      if (!active) return
      if ([401, 403].includes(err.response?.status)) navigate('/login', { replace: true })
      else setError('Unable to load this design draft.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [draftId, navigate])

  useEffect(() => {
    if (!dirty) return undefined
    function beforeUnload(event) { event.preventDefault(); event.returnValue = '' }
    window.addEventListener('beforeunload', beforeUnload)
    return () => window.removeEventListener('beforeunload', beforeUnload)
  }, [dirty])

  const commit = useCallback((transform) => {
    dispatch({ type: 'commit', transform })
    setNotice('')
  }, [])

  function onElementChange(id, transform) {
    if (!plan) return
    commit((current) => {
      const next = patchElement(current, id, transform)
      return next.elements.every((item) => fitsFloor(item, next)) ? next : current
    })
  }
  function addItem(name) {
    if (!plan || plan.elements.length >= MAX_ELEMENTS) { setError('Maximum 120 elements per draft.'); return }
    const item = makeElement(name, plan)
    commit((current) => ({ ...current, elements: [...current.elements, item] }))
    setSelectedId(item.clientId)
    setError('')
  }
  function clientPoint(event) {
    const svg = planSvg.current
    if (!svg) return null
    const matrix = svg.getScreenCTM()
    if (!matrix) return null
    const point = svg.createSVGPoint()
    point.x = event.clientX
    point.y = event.clientY
    return point.matrixTransform(matrix.inverse())
  }
  function gestureStart(event, item, mode) {
    if (event.button !== 0 || !plan) return
    const start = clientPoint(event)
    if (!start) return
    event.preventDefault(); event.stopPropagation()
    setSelectedId(item.clientId)
    gesture.current = { start, item, mode, plan, final: item }
    function move(pointer) {
      if (!gesture.current) return
      const now = clientPoint(pointer)
      if (!now) return
      const active = gesture.current
      const shifted = shiftElement(active.item, now.x - start.x, now.y - start.y, mode, active.plan, snap)
      active.final = shifted
      setPreview({ id: active.item.clientId, item: shifted })
    }
    function stop() {
      const active = gesture.current
      gesture.current = null
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', stop)
      window.removeEventListener('pointercancel', stop)
      setPreview(null)
      if (active && (active.final.xCm !== active.item.xCm || active.final.yCm !== active.item.yCm ||
        active.final.widthCm !== active.item.widthCm || active.final.heightCm !== active.item.heightCm)) {
        onElementChange(active.item.clientId, () => active.final)
      }
    }
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', stop)
    window.addEventListener('pointercancel', stop)
  }

  useEffect(() => {
    function onKeyDown(e) {
      if (!e.ctrlKey && !e.metaKey) return
      if (['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName)) return
      if (e.key.toLowerCase() === 'z') {
        e.preventDefault()
        dispatch({ type: e.shiftKey ? 'redo' : 'undo' })
      } else if (e.key.toLowerCase() === 'y') {
        e.preventDefault()
        dispatch({ type: 'redo' })
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  async function save() {
    if (!plan || saving || !dirty) return
    if (!validPlan(plan)) { setError('Check the design name, dimensions and elements before saving.'); return }
    setSaving(true); setError(''); setNotice('')
    try {
      const saved = await saveDesignDraft(draftId, savePayload(plan, version))
      dispatch({ type: 'reset', plan: hydratePlan(saved) })
      setVersion(saved.version)
      setSelectedId(null)
      setNotice('Design draft saved.')
    } catch (err) { setError(errorMessage(err, 'Unable to save this design.')) }
    finally { setSaving(false) }
  }
  async function exportPng() {
    if (!planSvg.current || exporting) return
    setExporting(true); setError('')
    try { await exportFloorPlan(planSvg.current, plan.title); setNotice('PNG floor plan exported.') }
    catch { setError('PNG export failed. Try the latest Chrome or Edge browser.') }
    finally { setExporting(false) }
  }
  function removeSelected() {
    if (!selectedId) return
    commit((current) => ({ ...current, elements: current.elements.filter((el) => el.clientId !== selectedId) }))
    setSelectedId(null)
  }
  function modifyNumber(field, value) {
    if (!selected || value === null || !Number.isFinite(value)) return
    const n = field === 'rotation' ? Math.round(value) : Math.round(value * 100)
    onElementChange(selectedId, (item) => ({ ...item, [field]: n }))
  }
  function amendFloor(field, value) {
    if (value === null || !Number.isFinite(value)) return
    commit((current) => changeFloor(current, field, Math.round(value * 100)))
  }
  function requestReturn() {
    if (!dirty) { navigate('/customer/designs'); return }
    Modal.confirm({ title: 'Discard unsaved changes?',
      content: 'Changes since your last save will be lost.', okText: 'Discard changes',
      okButtonProps: { danger: true }, cancelText: 'Stay',
      onOk: () => navigate('/customer/designs') })
  }

  if (loading) return <main className="ld-design-editor-page"><div className="ld-designs-loading"><Spin /> Loading floor plan…</div></main>
  if (!plan) return <main className="ld-design-editor-page">
    <CustomerReturnButton to="/customer/designs">Back to my design drafts</CustomerReturnButton>
    <Alert type="error" showIcon message={error || 'Design unavailable'} />
  </main>
  const width = plan.floorWidthCm, height = plan.floorHeightCm
  const displayElements = plan.elements.map((item) => preview?.id === item.clientId ? preview.item : item)
  const sortedElements = [...displayElements].sort((a, b) =>
    (a.kind === 'ROOM' ? 0 : a.kind === 'FURNITURE' ? 1 : 2) -
    (b.kind === 'ROOM' ? 0 : b.kind === 'FURNITURE' ? 1 : 2))
  return <main className="ld-design-editor-page">
    <div className="ld-design-editor-nav">
      <Button onClick={requestReturn}>← Back to my design drafts</Button>
      <span className="ld-design-status">{dirty ? 'Unsaved changes' : 'Saved'}</span>
    </div>
    <header className="ld-designs-heading ld-design-editor-heading">
      <div><p className="ui-eyebrow">RENOVATION PLANNING</p><h1>Renovation design draft</h1>
        <p>Create and communicate a conceptual measured 2D floor plan.</p></div>
      <div className="ld-design-editor-actions">
        <Button icon={<Download size={16} />} loading={exporting} onClick={exportPng}>Export PNG</Button>
        <Button type="primary" icon={<Save size={16} />} disabled={!dirty || !validPlan(plan)} loading={saving}
          onClick={save}>Save draft</Button>
      </div>
    </header>
    {error && <Alert type="error" showIcon role="alert" message={error} className="ld-designs-alert" />}
    {notice && <Alert type="success" showIcon role="status" message={notice} className="ld-designs-alert" />}
    <div className="ld-design-editor-meta">
      <label htmlFor="ld-design-title">Design name</label>
      <Input id="ld-design-title" maxLength={120} value={plan.title} style={{ maxWidth: 300 }}
        onChange={(event) => commit((current) => ({ ...current, title: event.target.value }))} />
      <label htmlFor="ld-design-width">Floor width (m)</label>
      <InputNumber id="ld-design-width" min={4} max={25} precision={2} step={0.25}
        value={width / 100} onChange={(n) => amendFloor('floorWidthCm', n)} />
      <label htmlFor="ld-design-height">Floor height (m)</label>
      <InputNumber id="ld-design-height" min={4} max={25} precision={2} step={0.25}
        value={height / 100} onChange={(n) => amendFloor('floorHeightCm', n)} />
    </div>
    <div className="ld-design-editor-layout">
      <aside className="ld-design-toolbox" aria-label="Floor plan tools">
        <h2>Design tools</h2><p>Add rooms, furniture and openings to the floor.</p>
        {Object.entries(LIBRARY_GROUPS).map(([group, names]) => <section key={group}>
          <h3>{group}</h3><div className="ld-design-tool-buttons">
            {names.map((name) => <Button key={name} disabled={plan.elements.length >= MAX_ELEMENTS}
              onClick={() => addItem(name)}>{name}</Button>)}
          </div></section>)}
        <div className="ld-design-tool-footer">
          <Button icon={<Undo2 size={16} />} disabled={!history.past.length}
            onClick={() => dispatch({ type: 'undo' })}>Undo</Button>
          <Button icon={<Redo2 size={16} />} disabled={!history.future.length}
            onClick={() => dispatch({ type: 'redo' })}>Redo</Button>
          <Button icon={<Grid3X3 size={16} />} type={snap ? 'primary' : 'default'}
            onClick={() => setSnap((current) => !current)}>Snap {snap ? 'on' : 'off'}</Button>
        </div>
        <p>Grid snapping: 0.25 m. Undo/Redo includes the most recent 80 edits.</p>
      </aside>
      <section className="ld-design-plan-panel" aria-labelledby="floor-plan-heading">
        <div className="ld-design-plan-toolbar"><div><h2 id="floor-plan-heading">Floor plan canvas</h2>
          <p>{metres(width)} × {metres(height)} m · {plan.elements.filter((item) => item.kind === 'ROOM').length} rooms</p></div>
          <div className="ld-design-zoom"><Button disabled={zoom <= 75}
            onClick={() => setZoom((n) => clamp(n - 25, 75, 150))}>−</Button>
            <span>{zoom}%</span><Button disabled={zoom >= 150}
              onClick={() => setZoom((n) => clamp(n + 25, 75, 150))}>+</Button></div>
        </div>
        {overlaps > 0 && <Alert type="warning" showIcon message={`${overlaps} room overlap${overlaps > 1 ? 's' : ''} detected`} description={OVERLAP_MESSAGE} />}
        <div className="ld-design-svg-scroll">
          <svg ref={planSvg} className="ld-design-svg" role="img" aria-label="Editable proposed renovation floor plan"
            width={`${zoom}%`} viewBox={`${-75} ${-80} ${width + 150} ${height + 170}`}>
            <title>Conceptual two-dimensional renovation floor plan</title>
            <defs><pattern id="ld-design-grid" width="25" height="25" patternUnits="userSpaceOnUse">
              <path d="M 25 0 L 0 0 0 25" fill="none" stroke="#e3e9ef" strokeWidth="1" /></pattern></defs>
            <rect x={-75} y={-80} width={width + 150} height={height + 170} fill="#eef3f8" />
            <rect x={0} y={0} width={width} height={height} fill="#fff" stroke="#3c5c78" strokeWidth={8} />
            <rect x={0} y={0} width={width} height={height} fill="url(#ld-design-grid)" />
            <text x={width / 2} y={-30} textAnchor="middle" fontSize="18" fill="#365b82" fontFamily="Arial, sans-serif">
              {metres(width)} m
            </text>
            <text x={-28} y={height / 2} fontSize="18" fill="#365b82" fontFamily="Arial, sans-serif"
              textAnchor="middle" transform={`rotate(-90 -28 ${height / 2})`}>{metres(height)} m</text>
            {sortedElements.map((item) => <PlanItem key={item.clientId} item={item}
              selected={selectedId === item.clientId} onStart={gestureStart} onSelect={setSelectedId} />)}
          </svg>
        </div>
        <p className="ld-design-canvas-hint">Select, drag and resize elements using the corner handle. Measurements are conceptual and are not structural plans.</p>
      </section>
      <aside className="ld-design-inspector" aria-label="Selected element properties">
        <h2>Properties</h2>
        {!selected ? <><p>Choose a room, furniture item or opening to edit its measured position and size.</p>
          <p>You can place and resize objects freely within the floor boundary.</p></> : <>
          <p className="ld-design-kind">{selected.kind.toLowerCase()}</p>
          <label htmlFor="selected-name">Name</label>
          <Input id="selected-name" maxLength={80} value={selected.name}
            onChange={(event) => onElementChange(selectedId, (item) => ({ ...item, name: event.target.value }))} />
          <div className="ld-design-properties-grid">
            {[["xCm", "X position"], ["yCm", "Y position"], ["widthCm", "Width"], ["heightCm", "Height"]]
              .map(([field, label]) => <div key={field}><label htmlFor={`design-${field}`}>{label} (m)</label>
                <InputNumber id={`design-${field}`} value={selected[field] / 100} precision={2}
                  min={field.startsWith('width') || field.startsWith('height') ? 0.2 : 0}
                  step={0.25} style={{ width: '100%' }} onChange={(value) => modifyNumber(field, value)} /></div>)}
          </div>
          <label htmlFor="design-rotation">Rotation (degrees)</label>
          <InputNumber id="design-rotation" min={0} max={359} step={15} style={{ width: '100%' }}
            disabled={selected.kind === 'ROOM'} value={selected.rotation}
            onChange={(value) => modifyNumber('rotation', value)} />
          <div className="ld-design-inspector-actions">
            <Button disabled={selected.kind === 'ROOM'} icon={<RotateCw size={15} />}
              onClick={() => onElementChange(selectedId, (item) => rotateElement(item))}>Rotate 90°</Button>
            <Button danger icon={<Trash2 size={15} />} onClick={removeSelected}>Remove</Button>
          </div>
        </>}
        <p className="ld-design-inspector-note">An estimate of furniture and room placement only. Site measurements and safety checks are required.</p>
      </aside>
    </div>
  </main>
}
