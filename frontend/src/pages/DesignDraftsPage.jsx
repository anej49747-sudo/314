import { Alert, Button, Input, InputNumber, Modal, Radio, Spin } from 'antd'
import { ArrowRight, DraftingCompass, FolderPlus, Pencil, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createDesignDraft, deleteDesignDraft, getDesignDraft, listDesignDrafts, saveDesignDraft } from '../api/designDrafts.js'
import CustomerReturnButton from '../components/CustomerReturnButton.jsx'
import { MAX_DRAFTS, metres, validFloor, validTitle } from '../design/floorPlanRules.js'
import '../ui/designDrafts.css'

const initial = { title: 'My renovation concept', floorWidthCm: 1200, floorHeightCm: 800, template: 'SAMPLE' }
export default function DesignDraftsPage() {
  const navigate = useNavigate()
  const [drafts, setDrafts] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [dialog, setDialog] = useState(null)
  const [form, setForm] = useState(initial)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    listDesignDrafts().then((items) => { if (active) setDrafts(items) }).catch((err) => {
      if (!active) return
      if ([401, 403].includes(err.response?.status)) navigate('/login', { replace: true })
      else setError('Unable to load your saved design drafts.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [navigate])

  function openCreate() {
    setForm(initial)
    setDialog({ mode: 'create' })
    setError('')
  }
  function rename(item) {
    setForm({ title: item.title })
    setDialog({ mode: 'rename', item })
    setError('')
  }
  function valid() {
    return validTitle(form.title) && (dialog?.mode === 'rename' ||
      (validFloor(form.floorWidthCm) && validFloor(form.floorHeightCm)))
  }
  async function submit() {
    if (!valid() || busy) return
    setBusy(true); setError(''); setNotice('')
    try {
      if (dialog.mode === 'create') {
        const created = await createDesignDraft(form)
        navigate(`/customer/designs/${created.id}`)
      } else {
        const original = await getDesignDraft(dialog.item.id)
        const updated = await saveDesignDraft(original.id, {
          version: original.version, title: form.title, floorWidthCm: original.floorWidthCm,
          floorHeightCm: original.floorHeightCm, elements: original.elements,
        })
        setDrafts((items) => items.map((item) => item.id === updated.id ?
          { ...item, title: updated.title, updatedAt: updated.updatedAt } : item))
        setNotice('Design draft renamed.')
        setDialog(null)
      }
    } catch (err) {
      setError(err.response?.status === 409 ? 'This design was edited elsewhere. Refresh before renaming.' :
        err.response?.data?.message || 'Unable to save the design draft.')
    } finally { setBusy(false) }
  }
  function confirmDelete(item) {
    Modal.confirm({ title: 'Delete this design draft?',
      content: `The layout “${item.title}” will be permanently deleted. Your moodboards and projects are unaffected.`,
      okText: 'Delete draft', okButtonProps: { danger: true }, cancelText: 'Keep draft',
      onOk: async () => {
        try {
          await deleteDesignDraft(item.id)
          setDrafts((items) => items.filter((other) => other.id !== item.id))
          setNotice('Design draft deleted.'); setError('')
        } catch { setError('Could not delete this draft. Try again.') }
      },
    })
  }
  return <main className="ld-designs-page">
    <CustomerReturnButton />
    <header className="ld-designs-heading">
      <div><p className="ui-eyebrow">RENOVATION PLANNING</p><h1>My design drafts</h1>
        <p>Explore measured room layouts and plan where furniture could go.</p></div>
      <Button type="primary" icon={<FolderPlus size={17} />} disabled={loading || drafts.length >= MAX_DRAFTS}
        onClick={openCreate}>Create design draft</Button>
    </header>
    {notice && <Alert type="success" showIcon message={notice} role="status" className="ld-designs-alert" />}
    {error && <Alert type="error" showIcon message={error} role="alert" className="ld-designs-alert" />}
    {loading ? <div className="ld-designs-loading"><Spin /> Loading designs…</div> :
      drafts.length === 0 ? <section className="ld-designs-empty">
        <DraftingCompass size={36} aria-hidden="true" /><h2>Start your first floor plan</h2>
        <p>Create an empty layout or use a sample arrangement to explore rooms, furniture and openings.</p>
        <Button type="primary" onClick={openCreate}>Create design draft</Button>
      </section> : <div className="ld-designs-grid" aria-label="Saved design drafts">
        {drafts.map((item) => <article className="ld-design-card" key={item.id}>
          <button type="button" className="ld-design-cover" onClick={() => navigate(`/customer/designs/${item.id}`)}
            aria-label={`Open ${item.title}`}>
            <span className="ld-design-mini-plan"><span /><span /><span /><span /></span>
          </button>
          <div className="ld-design-card-body"><h2>{item.title}</h2>
            <p>{metres(item.floorWidthCm)} × {metres(item.floorHeightCm)} m · {item.roomCount} rooms</p>
            <p>Last edited {new Date(item.updatedAt).toLocaleDateString()}</p>
            <div className="ld-design-card-actions">
              <Button icon={<ArrowRight size={15} />} onClick={() => navigate(`/customer/designs/${item.id}`)}>Open</Button>
              <Button aria-label={`Rename ${item.title}`} icon={<Pencil size={15} />} onClick={() => rename(item)} />
              <Button danger aria-label={`Delete ${item.title}`} icon={<Trash2 size={15} />} onClick={() => confirmDelete(item)} />
            </div>
          </div>
        </article>)}
      </div>}
    <Modal open={!!dialog} destroyOnHidden title={dialog?.mode === 'rename' ? 'Rename design draft' : 'Create design draft'}
      onCancel={() => !busy && setDialog(null)} onOk={submit}
      okText={dialog?.mode === 'rename' ? 'Save name' : 'Create draft'}
      okButtonProps={{ disabled: !valid(), loading: busy }}>
      <div className="ld-design-form">
        <label htmlFor="design-title">Design name</label>
        <Input id="design-title" value={form.title} maxLength={120}
          onChange={(e) => setForm((previous) => ({ ...previous, title: e.target.value }))} />
        {dialog?.mode === 'create' && <>
          <label>Starting layout</label>
          <Radio.Group value={form.template} onChange={(e) => setForm((previous) => ({ ...previous,
            template: e.target.value, floorWidthCm: e.target.value === 'SAMPLE' ? 1200 : 1000,
            floorHeightCm: 800 }))}>
            <Radio value="SAMPLE">Sample layout</Radio><Radio value="BLANK">Blank floor</Radio>
          </Radio.Group>
          <div className="ld-design-form-dimensions">
            <div><label htmlFor="design-floor-width">Floor width (m)</label>
              <InputNumber id="design-floor-width" min={4} max={25} step={0.25} precision={2} style={{ width: '100%' }}
                value={form.floorWidthCm / 100} onChange={(value) => setForm((previous) =>
                  ({ ...previous, floorWidthCm: value === null ? null : Math.round(value * 100) }))} /></div>
            <div><label htmlFor="design-floor-height">Floor height (m)</label>
              <InputNumber id="design-floor-height" min={4} max={25} step={0.25} precision={2} style={{ width: '100%' }}
                value={form.floorHeightCm / 100} onChange={(value) => setForm((previous) =>
                  ({ ...previous, floorHeightCm: value === null ? null : Math.round(value * 100) }))} /></div>
          </div>
          <p>These are conceptual measurements; verify all measurements on-site.</p>
        </>}
      </div>
    </Modal>
  </main>
}
