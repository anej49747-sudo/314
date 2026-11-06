import { Alert, Button, Input, Select, Spin } from 'antd'
import { ArrowDown, ArrowUp, Copy, Download, ImagePlus, Layers, Palette, Redo2, Save,
  Search, Trash2, Type, Undo2 } from 'lucide-react'
import { useCallback, useEffect, useReducer, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getMoodboardPhotos, loadMoodboard, saveMoodboard } from '../api/moodboards.js'
import CustomerReturnButton from '../components/CustomerReturnButton.jsx'
import { canvasPayload, CANVAS_HEIGHT, CANVAS_WIDTH, clamp, editorReducer, initialCanvas,
  MAX_ELEMENTS, moveOrResize, newNote, newPhoto, newSwatch, placeLayer, templateLayout,
  uniqueId, updateItem } from '../moodboard/canvasRules.js'
import { downloadPng, renderMoodboardPng } from '../moodboard/exportPng.js'
import '../ui/moodboards.css'

const EMPTY = { canvas: null, past: [], future: [] }
const COLOR_PRESETS = ['#C8B99F', '#E9E1D5', '#73908D', '#365B82', '#C28C75', '#7A6F66']
function errorText(err, fallback) {
  if (err.response?.status === 409) return 'This board was edited in another session. Reload the page before saving.'
  return err.response?.data?.message || fallback
}
function ensureAccessiblePhoto(photo) {
  // Backend owns photo authorisation. Only server-returned URLs enter image/export paths.
  return photo.photoUrl && photo.photoUrl.startsWith('/api/projects/') ? photo.photoUrl : null
}

function CanvasElement({ item, selected, onSelect, onGesture }) {
  return <div role="button" tabIndex={0} aria-label={`${item.kind.toLowerCase()} element${item.sourceLabel ? `: ${item.sourceLabel}` : ''}`}
    aria-pressed={selected} className={`ld-moodboard-canvas-element ${selected ? 'is-selected' : ''} ${item.kind.toLowerCase()}`}
    onPointerDown={(event) => onGesture(event, item, 'move')}
    onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); onSelect(item.clientId) } }}
    style={{ left: `${item.x / CANVAS_WIDTH * 100}%`, top: `${item.y / CANVAS_HEIGHT * 100}%`,
      width: `${item.width / CANVAS_WIDTH * 100}%`, height: `${item.height / CANVAS_HEIGHT * 100}%` }}>
    {item.kind === 'PHOTO' ? ensureAccessiblePhoto(item) ?
      <img src={item.photoUrl} alt={item.sourceLabel || 'Saved project inspiration'} draggable={false} /> :
      <span className="ld-moodboard-unavailable">Photo unavailable</span> : item.kind === 'TEXT' ?
        <span className="ld-moodboard-text-content" style={{ color: item.color, fontSize: `${item.fontSize / CANVAS_WIDTH * 100}cqw` }}>
          {item.text}</span> : <span className="ld-moodboard-swatch" style={{ backgroundColor: item.color }} />}
    {selected && <button type="button" className="ld-moodboard-resize" aria-label="Resize element"
      onPointerDown={(event) => { event.stopPropagation(); onGesture(event, item, 'resize') }} />}
  </div>
}

export default function MoodboardEditorPage() {
  const { boardId } = useParams()
  const navigate = useNavigate()
  const [editor, dispatch] = useReducer(editorReducer, EMPTY)
  const [version, setVersion] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [dirty, setDirty] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [photos, setPhotos] = useState([])
  const [query, setQuery] = useState('')
  const [activeTool, setActiveTool] = useState('photos')
  const boardRef = useRef(null)
  const drag = useRef(null)
  const canvas = editor.canvas
  const selected = canvas?.elements.find((item) => item.clientId === selectedId)

  useEffect(() => {
    let active = true
    Promise.all([loadMoodboard(boardId), getMoodboardPhotos()]).then(([board, library]) => {
      if (!active) return
      dispatch({ type: 'reset', canvas: initialCanvas(board) })
      setVersion(board.version)
      setPhotos(library)
      setDirty(false)
    }).catch((err) => {
      if (!active) return
      if ([401, 403].includes(err.response?.status)) navigate('/login', { replace: true })
      else setError('Unable to open this moodboard. Please try again.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [boardId, navigate])

  useEffect(() => {
    if (!dirty) return undefined
    function beforeUnload(event) { event.preventDefault(); event.returnValue = '' }
    window.addEventListener('beforeunload', beforeUnload)
    return () => window.removeEventListener('beforeunload', beforeUnload)
  }, [dirty])

  const commit = useCallback((change) => {
    dispatch({ type: 'commit', change })
    setDirty(true); setNotice('')
  }, [])

  function changeSelected(transform) {
    if (selectedId) commit((current) => updateItem(current, selectedId, transform))
  }
  function add(item) {
    if (!canvas || canvas.elements.length >= MAX_ELEMENTS) { setError('Maximum 60 elements per moodboard.'); return }
    commit((current) => ({ ...current, elements: [...current.elements, item] }))
    setSelectedId(item.clientId)
    setError('')
  }

  function gestureStart(event, item, mode) {
    if (event.button !== 0 || !boardRef.current) return
    event.preventDefault(); event.stopPropagation(); setSelectedId(item.clientId)
    const before = editor.canvas
    const startX = event.clientX, startY = event.clientY
    const ratio = CANVAS_WIDTH / boardRef.current.getBoundingClientRect().width
    drag.current = { before, id: item.clientId, moved: false }
    function move(ev) {
      if (!drag.current) return
      const dx = (ev.clientX - startX) * ratio, dy = (ev.clientY - startY) * ratio
      const moved = Math.abs(dx) + Math.abs(dy) >= 1
      if (moved) drag.current.moved = true
      dispatch({ type: 'preview', canvas: updateItem(before, item.clientId,
        (current) => moveOrResize(current, dx, dy, mode)) })
    }
    function end() {
      if (drag.current?.moved) {
        dispatch({ type: 'recordGesture', before: drag.current.before })
        setDirty(true); setNotice('')
      }
      drag.current = null
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', end)
      window.removeEventListener('pointercancel', end)
    }
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', end)
    window.addEventListener('pointercancel', end)
  }

  const moveByKeyboard = useCallback((dx, dy) => {
    if (!selectedId) return
    commit((current) => updateItem(current, selectedId, (item) => moveOrResize(item, dx, dy)))
  }, [selectedId, commit])

  useEffect(() => {
    function keydown(event) {
      const editing = ['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName)
        || document.activeElement?.isContentEditable
      if (editing) return
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'z') {
        event.preventDefault(); dispatch({ type: event.shiftKey ? 'redo' : 'undo' }); setDirty(true)
      } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'y') {
        event.preventDefault(); dispatch({ type: 'redo' }); setDirty(true)
      } else if (selectedId && ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(event.key)) {
        event.preventDefault()
        const distance = event.shiftKey ? 10 : 1
        moveByKeyboard(event.key === 'ArrowLeft' ? -distance : event.key === 'ArrowRight' ? distance : 0,
          event.key === 'ArrowUp' ? -distance : event.key === 'ArrowDown' ? distance : 0)
      } else if (selectedId && ['Delete', 'Backspace'].includes(event.key)) {
        event.preventDefault(); commit((current) => ({ ...current,
          elements: current.elements.filter((item) => item.clientId !== selectedId) })); setSelectedId(null)
      }
    }
    document.addEventListener('keydown', keydown)
    return () => document.removeEventListener('keydown', keydown)
  }, [selectedId, commit, moveByKeyboard])

  async function save() {
    if (!canvas || saving) return
    setSaving(true); setError(''); setNotice('')
    try {
      const result = await saveMoodboard(boardId, canvasPayload(canvas, version))
      dispatch({ type: 'reset', canvas: initialCanvas(result) })
      setSelectedId(null); setVersion(result.version); setDirty(false)
      setNotice('Moodboard saved. Your layout is available next time you sign in.')
    } catch (err) { setError(errorText(err, 'Could not save your moodboard.')) }
    finally { setSaving(false) }
  }

  async function exportImage() {
    if (!canvas || exporting) return
    setExporting(true); setError('')
    try { downloadPng(await renderMoodboardPng(canvas), canvas.title) }
    catch { setError('PNG export failed. Check that your project photos are available, then try again.') }
    finally { setExporting(false) }
  }

  if (loading) return <div className="ld-moodboards-loading"><Spin /> Loading moodboard editor…</div>
  if (!canvas) return <main className="ld-moodboards-page"><CustomerReturnButton to="/customer/moodboards" children="Back to moodboards" />
    <Alert type="error" showIcon message={error || 'Moodboard not found'} /></main>

  const visibleProjects = photos.map((project) => ({ ...project,
    photos: project.photos.filter((photo) => `${project.projectTitle} ${photo.label}`.toLowerCase().includes(query.toLowerCase())) }))
    .filter((project) => project.photos.length)

  return <main className="ld-moodboard-editor-page">
    <div className="ld-moodboard-editor-nav"><CustomerReturnButton to="/customer/moodboards" children="Back to moodboards" confirmLeave={dirty} />
      <span className="ld-moodboard-editor-status">{dirty ? 'Unsaved changes' : 'All changes saved'}</span></div>
    <header className="ld-moodboard-editor-header"><div><p className="ui-eyebrow">RENOVATION INSPIRATION</p>
      <h1>Canvas editor</h1><p>Arrange your saved photos, design notes and colours freely.</p></div>
      <div className="ld-moodboard-editor-head-actions">
        <Button icon={<Undo2 size={16} />} disabled={!editor.past.length} onClick={() => { dispatch({ type: 'undo' }); setDirty(true) }}>Undo</Button>
        <Button icon={<Redo2 size={16} />} disabled={!editor.future.length} onClick={() => { dispatch({ type: 'redo' }); setDirty(true) }}>Redo</Button>
        <Button icon={<Download size={16} />} loading={exporting} onClick={exportImage}>Export PNG</Button>
        <Button type="primary" icon={<Save size={16} />} loading={saving} disabled={!dirty} onClick={save}>Save board</Button>
      </div></header>
    {notice && <Alert className="ld-moodboards-alert" type="success" showIcon role="status" message={notice} />}
    {error && <Alert className="ld-moodboards-alert" type="error" showIcon role="alert" message={error}
      action={error.includes('another session') ? <Button size="small" onClick={() => window.location.reload()}>Reload latest</Button> : null} />}
    <div className="ld-moodboard-editor-meta">
      <label htmlFor="ld-moodboard-name">Board name</label>
      <Input id="ld-moodboard-name" value={canvas.title} maxLength={120}
        onChange={(event) => commit((current) => ({ ...current, title: event.target.value }))} />
      <label htmlFor="ld-moodboard-template">Layout template</label>
      <Select id="ld-moodboard-template" defaultValue="custom" style={{ minWidth: 165 }}
        options={[{ value: 'custom', label: 'Freeform' }, { value: 'editorial', label: 'Editorial' },
          { value: 'balanced', label: 'Balanced' }, { value: 'minimal', label: 'Minimal' }]}
        onChange={(choice) => { if (choice !== 'custom') commit((current) => templateLayout(current, choice)) }} />
      <span className="ld-moodboard-editor-help">900 × 580 design canvas · Arrow keys move selected items</span>
    </div>
    <div className="ld-moodboard-editor-layout">
      <aside className="ld-moodboard-toolbox" aria-label="Add moodboard elements">
        <div className="ld-moodboard-tab-controls">
          <Button type={activeTool === 'photos' ? 'primary' : 'default'} icon={<ImagePlus size={16} />}
            onClick={() => setActiveTool('photos')}>Photos</Button>
          <Button type={activeTool === 'elements' ? 'primary' : 'default'} icon={<Palette size={16} />}
            onClick={() => setActiveTool('elements')}>Elements</Button>
        </div>
        {activeTool === 'photos' ? <>
          <h2>Saved project photos</h2><p>Pick photos from your favourite projects. Selecting them won't add project views.</p>
          <Input prefix={<Search size={15} />} placeholder="Search saved projects" value={query}
            onChange={(event) => setQuery(event.target.value)} aria-label="Search saved projects" />
          {!photos.length && <p className="ld-moodboard-library-empty">Save projects to favourites to add their photos here.</p>}
          {visibleProjects.map((group) => <section className="ld-moodboard-photo-group" key={group.projectId}>
            <h3>{group.projectTitle}</h3><div className="ld-moodboard-photo-grid">
              {group.photos.map((photo) => <button key={photo.id} type="button" aria-label={`Add ${photo.label} to moodboard`}
                disabled={canvas.elements.length >= MAX_ELEMENTS} onClick={() => add(newPhoto(photo, {
                  x: 70 + (canvas.elements.length % 5) * 28,
                  y: 65 + (canvas.elements.length % 5) * 24,
                }))}>
                <img src={photo.url} alt={photo.label} loading="lazy" />
                <span>+ Add photo</span></button>)}
            </div></section>)}
        </> : <><h2>Design elements</h2><p>Mix annotations and colour swatches with your project images.</p>
          <Button block icon={<Type size={16} />} onClick={() => add(newNote())}>Add text block</Button>
          <div className="ld-moodboard-swatch-picker"><h3>Colour swatches</h3>
            <div className="ld-moodboard-swatch-options">{COLOR_PRESETS.map((color) => <button key={color}
              aria-label={`Add ${color} swatch`} style={{ background: color }} onClick={() => add(newSwatch(color))} />)}</div>
          </div></>}
        <p className="ld-moodboard-element-count">{canvas.elements.length}/{MAX_ELEMENTS} elements</p>
      </aside>
      <section className="ld-moodboard-center" aria-label="Moodboard editing canvas">
        <div className="ld-moodboard-canvas-scroll"><div className="ld-moodboard-canvas" ref={boardRef}
          role="group" aria-label="Freeform moodboard canvas" style={{ backgroundColor: canvas.backgroundColor }}>
          {canvas.elements.map((item) => <CanvasElement key={item.clientId} item={item}
            selected={selectedId === item.clientId} onSelect={setSelectedId} onGesture={gestureStart} />)}
          {!canvas.elements.length && <div className="ld-moodboard-canvas-empty">Choose a saved photo, add a note, or add a swatch to begin your canvas.</div>}
        </div></div>
        <p className="ld-moodboard-canvas-hint">Drag to move · Drag a selected corner to resize · Select an element to edit its properties</p>
      </section>
      <aside className="ld-moodboard-properties" aria-label="Element properties">
        <h2><Layers size={17} /> Properties</h2>
        {!selected ? <><p>Select an element to edit its position, colour, text or layering.</p>
          <label htmlFor="ld-moodboard-background">Canvas background</label>
          <input id="ld-moodboard-background" type="color" value={canvas.backgroundColor}
            onChange={(event) => commit((current) => ({ ...current, backgroundColor: event.target.value }))} />
        </> : <><p className="ld-moodboard-item-kind">{selected.kind === 'PHOTO' ? 'Project photo' : selected.kind === 'TEXT' ? 'Text block' : 'Colour swatch'}</p>
          {selected.kind === 'PHOTO' && <p>{selected.sourceLabel || 'Selected photo'}{!selected.photoUrl ? ' · Unavailable' : ''}</p>}
          {selected.kind === 'TEXT' && <>
            <label htmlFor="ld-moodboard-text">Text</label>
            <Input.TextArea id="ld-moodboard-text" value={selected.text} maxLength={180} rows={3}
              onChange={(event) => changeSelected((el) => ({ ...el, text: event.target.value }))} />
            <label htmlFor="ld-moodboard-font">Font size</label><Input id="ld-moodboard-font" type="number" min={12} max={56}
              value={selected.fontSize} onChange={(event) => changeSelected((el) => ({ ...el,
                fontSize: clamp(Math.round(Number(event.target.value) || 12), 12, 56) }))} />
          </>}
          {selected.kind !== 'PHOTO' && <><label htmlFor="ld-moodboard-colour">{selected.kind === 'TEXT' ? 'Text colour' : 'Swatch colour'}</label>
            <input id="ld-moodboard-colour" type="color" value={selected.color}
              onChange={(event) => changeSelected((el) => ({ ...el, color: event.target.value }))} /></>}
          <h3>Position and size</h3><div className="ld-moodboard-geometry">
            {[['x','Left'],['y','Top'],['width','Width'],['height','Height']].map(([key, label]) =>
              <label key={key}>{label}<input type="number" value={selected[key]} min={0} max={key === 'y' || key === 'height' ? 580 : 900}
                onChange={(event) => changeSelected((el) => {
                  const value = Math.round(Number(event.target.value) || 0)
                  if (key === 'x') return { ...el, x: clamp(value, 0, CANVAS_WIDTH - el.width) }
                  if (key === 'y') return { ...el, y: clamp(value, 0, CANVAS_HEIGHT - el.height) }
                  if (key === 'width') return { ...el, width: clamp(value, 55, CANVAS_WIDTH - el.x) }
                  return { ...el, height: clamp(value, 45, CANVAS_HEIGHT - el.y) }
                })} /></label>)}
          </div>
          <h3>Arrange layers</h3><div className="ld-moodboard-layer-actions">
            <Button icon={<ArrowUp size={15} />} onClick={() => commit((current) => placeLayer(current, selectedId, 1))}>Bring forward</Button>
            <Button icon={<ArrowDown size={15} />} onClick={() => commit((current) => placeLayer(current, selectedId, -1))}>Send backward</Button>
            <Button icon={<Copy size={15} />} disabled={canvas.elements.length >= MAX_ELEMENTS} onClick={() => {
              const copy = { ...selected, clientId: uniqueId(),
                x: clamp(selected.x + 22, 0, CANVAS_WIDTH - selected.width),
                y: clamp(selected.y + 22, 0, CANVAS_HEIGHT - selected.height) }
              add(copy)
            }}>Duplicate</Button>
            <Button danger icon={<Trash2 size={15} />} onClick={() => {
              commit((current) => ({ ...current, elements: current.elements.filter((el) => el.clientId !== selectedId) }))
              setSelectedId(null)
            }}>Remove element</Button>
          </div>
          <p className="ld-moodboard-canvas-hint">Use arrow keys to move by 1 px, or Shift + arrows for 10 px.</p>
        </>}
      </aside>
    </div>
  </main>
}
