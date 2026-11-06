import { Alert, Button, Input, Modal, Spin } from 'antd'
import { ArrowRight, FolderPlus, Images, Pencil, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createMoodboard, deleteMoodboard, listMoodboards, loadMoodboard, saveMoodboard } from '../api/moodboards.js'
import CustomerReturnButton from '../components/CustomerReturnButton.jsx'
import { MAX_BOARDS, validTitle } from '../moodboard/canvasRules.js'
import '../ui/moodboards.css'

export default function MoodboardsPage() {
  const navigate = useNavigate()
  const [boards, setBoards] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [modal, setModal] = useState(null)
  const [title, setTitle] = useState('')

  useEffect(() => {
    let current = true
    listMoodboards().then((items) => { if (current) setBoards(items) }).catch((err) => {
      if (!current) return
      if ([401, 403].includes(err.response?.status)) navigate('/login', { replace: true })
      else setError('Could not load your moodboards. Try refreshing this page.')
    }).finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [navigate])

  function editBoard(board = null) {
    setTitle(board?.title || '')
    setModal(board ? { mode: 'rename', board } : { mode: 'create' })
  }
  async function submit() {
    if (!validTitle(title) || busy) return
    setBusy(true); setError(''); setNotice('')
    try {
      if (modal.mode === 'create') {
        const board = await createMoodboard(title.trim())
        navigate(`/customer/moodboards/${board.id}`)
      } else {
        // Rename uses the same optimistic version contract as canvas saves.
        const board = await loadMoodboard(modal.board.id)
        const changed = await saveMoodboard(board.id, {
          version: board.version, title: title.trim(), backgroundColor: board.backgroundColor,
          elements: board.elements.map(({ kind, photoId, sourceLabel, text, color, fontSize,
            x, y, width, height }) => ({ kind,
            photoId: photoId || null, sourceLabel, text, color, fontSize, x, y, width, height })),
        })
        setBoards((current) => current.map((item) => item.id === changed.id ?
          { ...item, title: changed.title, updatedAt: changed.updatedAt } : item))
        setNotice('Moodboard renamed.')
        setModal(null)
      }
    } catch (err) {
      setError(err.response?.status === 409
        ? 'The moodboard was edited elsewhere. Refresh and try again.'
        : err.response?.data?.message || 'Unable to save your moodboard right now.')
    } finally { setBusy(false) }
  }
  function requestDelete(board) {
    Modal.confirm({ title: 'Delete this moodboard?',
      content: `“${board.title}” and its arranged elements will be permanently deleted. Original project photos will not be affected.`,
      okText: 'Delete board', okButtonProps: { danger: true }, cancelText: 'Keep board',
      onOk: async () => {
        try {
          await deleteMoodboard(board.id)
          setBoards((current) => current.filter((item) => item.id !== board.id))
          setNotice('Moodboard deleted.')
          setError('')
        } catch { setError('Could not delete this moodboard. Please try again.') }
      },
    })
  }

  return <main className="ld-moodboards-page">
    <CustomerReturnButton />
    <header className="ld-moodboards-page-heading">
      <div><p className="ui-eyebrow">RENOVATION INSPIRATION</p><h1>My moodboards</h1>
        <p>Build your own visual direction with saved project photos, colour swatches and notes.</p></div>
      <Button type="primary" icon={<FolderPlus size={17} />} onClick={() => editBoard()}
        disabled={boards.length >= MAX_BOARDS || loading}>Create moodboard</Button>
    </header>
    {notice && <Alert type="success" showIcon message={notice} role="status" className="ld-moodboards-alert" />}
    {error && <Alert type="error" showIcon message={error} role="alert" className="ld-moodboards-alert" />}
    {loading ? <div className="ld-moodboards-loading"><Spin /> Loading your moodboards…</div> :
      boards.length === 0 ? <section className="ld-moodboards-empty">
        <Images size={34} aria-hidden="true" /><h2>Your first inspiration board starts here</h2>
        <p>Create a moodboard, then select photos from projects you've saved to favourites.</p>
        <Button type="primary" onClick={() => editBoard()}>Create my first moodboard</Button>
      </section> : <div className="ld-moodboards-overview-grid" aria-label="Saved moodboards">
        {boards.map((board) => <article className="ld-moodboard-overview-card" key={board.id}>
          <button type="button" className="ld-moodboard-cover" onClick={() => navigate(`/customer/moodboards/${board.id}`)}
            aria-label={`Open ${board.title}`}>
            {board.coverUrl ? <img src={board.coverUrl} alt="Moodboard inspiration" /> :
              <span className="ld-moodboard-cover-empty"><Images size={36} /> Add your inspiration</span>}
          </button>
          <div className="ld-moodboard-card-info"><h2>{board.title}</h2>
            <p>{board.elementCount} {board.elementCount === 1 ? 'element' : 'elements'} · Last edited {new Date(board.updatedAt).toLocaleDateString()}</p>
            <div className="ld-moodboard-card-actions">
              <Button onClick={() => navigate(`/customer/moodboards/${board.id}`)} icon={<ArrowRight size={15} />}>Open board</Button>
              <Button aria-label={`Rename ${board.title}`} onClick={() => editBoard(board)} icon={<Pencil size={15} />} />
              <Button danger aria-label={`Delete ${board.title}`} onClick={() => requestDelete(board)} icon={<Trash2 size={15} />} />
            </div>
          </div>
        </article>)}
      </div>}
    <Modal open={!!modal} title={modal?.mode === 'rename' ? 'Rename moodboard' : 'Create moodboard'}
      onCancel={() => !busy && setModal(null)} onOk={submit} okText={modal?.mode === 'rename' ? 'Save name' : 'Create board'}
      okButtonProps={{ disabled: !validTitle(title), loading: busy }} destroyOnHidden>
      <label className="ld-moodboards-title-label" htmlFor="moodboard-title">Moodboard name</label>
      <Input id="moodboard-title" value={title} maxLength={120} onChange={(event) => setTitle(event.target.value)}
        onPressEnter={submit} placeholder="e.g. Warm Japandi home" />
      <p className="ld-moodboards-field-hint">1–120 characters</p>
    </Modal>
  </main>
}
