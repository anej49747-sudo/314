import { Alert, Button, Input, Modal, Select, Spin } from 'antd'
import { ChevronDown, ChevronUp, MessageCircleQuestion } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentSession } from '../api/authentication.js'
import { askProjectQuestion, getAnsweredProjectQuestions, getMyPendingProjectQuestions } from '../api/projectQuestions.js'
import { pageLabel, QA_BROWSE_SIZE, QA_PREVIEW_SIZE, questionDate, questionValidationError } from './questionRules.js'
import { questionAudience, showsQuestionPrompt } from './questionAudience.js'
import './projectQuestions.css'

export default function ProjectQuestionsPanel({ project }) {
  const navigate = useNavigate()
  const [role, setRole] = useState('loading')
  const [showAsk, setShowAsk] = useState(false)
  const [loginOpen, setLoginOpen] = useState(false)
  const [body, setBody] = useState('')
  const [sending, setSending] = useState(false)
  const [sendError, setSendError] = useState('')
  const [sendSuccess, setSendSuccess] = useState('')
  const [pending, setPending] = useState({ items: [], total: 0 })
  const [pendingError, setPendingError] = useState('')
  const [browseAll, setBrowseAll] = useState(false)
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [sort, setSort] = useState('newest')
  const [page, setPage] = useState(0)
  const [results, setResults] = useState({ items: [], total: 0, page: 0, totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [expanded, setExpanded] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    currentSession().then((session) => {
      if (active) setRole(questionAudience(session))
    }).catch(() => { if (active) setRole('visitor') })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (role !== 'customer') return undefined
    let active = true
    getMyPendingProjectQuestions(project.id).then((entries) => {
      if (active) { setPending(entries); setPendingError('') }
    }).catch(() => { if (active) setPendingError('Unable to load your pending questions.') })
    return () => { active = false }
  }, [project.id, role, reloadKey])

  useEffect(() => {
    const handle = setTimeout(() => setDebouncedSearch(search), 250)
    return () => clearTimeout(handle)
  }, [search])

  useEffect(() => {
    let active = true
    setLoading(true)
    setLoadError('')
    getAnsweredProjectQuestions(project.id, {
      query: browseAll ? debouncedSearch : '', order: browseAll ? sort : 'newest',
      page: browseAll ? page : 0, size: browseAll ? QA_BROWSE_SIZE : QA_PREVIEW_SIZE,
    }).then((data) => {
      if (active) { setResults(data); setExpanded(null) }
    }).catch(() => { if (active) setLoadError('Unable to load project questions. Please try again.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [project.id, browseAll, debouncedSearch, sort, page, reloadKey])

  async function submitQuestion(event) {
    event.preventDefault()
    const validation = questionValidationError(body)
    if (validation || sending) { setSendError(validation); return }
    setSending(true)
    setSendError('')
    try {
      await askProjectQuestion(project.id, body.trim())
      setBody('')
      setShowAsk(false)
      setSendSuccess('Question submitted. Only you and the designer can see it until it is answered.')
      setReloadKey((key) => key + 1)
    } catch {
      setSendError('Could not submit your question. Please try again.')
    } finally { setSending(false) }
  }

  function toggleBrowse() {
    setBrowseAll((all) => !all)
    setSearch('')
    setDebouncedSearch('')
    setPage(0)
    setExpanded(null)
  }

  return <section className="ld-qa-panel" aria-labelledby="project-qa-heading">
    <header className="ld-qa-header">
      <div><p className="ld-qa-eyebrow">Helpful project insights</p>
        <h2 id="project-qa-heading">Questions &amp; answers</h2>
        <p>{showsQuestionPrompt(role) ? 'Find answers about this renovation, or ask the designer something new.' : 'Find answers about this renovation from its designer.'}</p></div>
      <span className="ld-qa-count">{results.total} answered</span>
    </header>
    {showsQuestionPrompt(role) && <div className="ld-qa-ask-prompt">
      <div><h3>Have a question about this project?</h3><p>Ask the designer about materials, layout or renovation decisions.</p></div>
      <Button type="primary" onClick={() => {
        if (role !== 'customer') { setLoginOpen(true); return }
        setShowAsk((open) => !open)
        setSendSuccess('')
      }}>Ask a question</Button>
    </div>}
    {sendSuccess && <Alert showIcon type="success" role="status" message={sendSuccess} className="ld-qa-feedback" />}
    {showAsk && role === 'customer' && <form className="ld-qa-ask-form" onSubmit={submitQuestion}>
      <h3>Ask {project.designer.name}</h3>
      <p>Only you and the designer can see unanswered questions. Once answered, the question and answer become public without your name.</p>
      <label htmlFor="project-qa-question">Your project question</label>
      <Input.TextArea id="project-qa-question" value={body} maxLength={600} rows={4}
        onChange={(event) => setBody(event.target.value)} placeholder="What materials were used for the flooring?" />
      <div className="ld-qa-form-actions"><span>{body.length} / 600 characters</span>
        <div><Button onClick={() => { setShowAsk(false); setSendError('') }}>Cancel</Button>
          <Button type="primary" htmlType="submit" loading={sending} disabled={Boolean(questionValidationError(body)) || sending}>Send question</Button></div>
      </div>
      {sendError && <Alert type="error" role="alert" message={sendError} />}
    </form>}
    {role === 'customer' && pending.total > 0 && <section className="ld-qa-pending" aria-labelledby="qa-pending-title">
      <div className="ld-qa-pending-heading"><h3 id="qa-pending-title">Your questions awaiting an answer</h3><span>{pending.total} pending</span></div>
      <p>Visible only to you and this project's designer until answered.</p>
      {pending.items.map((item) => <div className="ld-qa-pending-item" key={item.id}>
        <div><span className="ld-qa-question-text">{item.question}</span><small>Asked {questionDate(item.createdAt)} · Waiting for a reply</small></div>
        <span className="ld-qa-status">Pending</span>
      </div>)}
      {pending.total > 3 && <p>{pending.total - 3} more pending questions.</p>}
    </section>}
    {pendingError && <Alert type="warning" showIcon message={pendingError} />}
    <div className="ld-qa-answered-title"><div><h3>Answered questions</h3><p>Browse responses directly from this project's interior designer.</p></div>
      <span>{results.total === 0 ? 'No answers' : `Showing ${results.items.length} of ${results.total} answers`}</span></div>
    {browseAll && <div className="ld-qa-tools">
      <label htmlFor="ld-qa-search">Search answers</label><Input id="ld-qa-search" placeholder="Search materials, layout, budget…"
        value={search} maxLength={100} onChange={(event) => { setSearch(event.target.value); setPage(0) }} allowClear />
      <label htmlFor="ld-qa-sort">Order</label><Select id="ld-qa-sort" value={sort} onChange={(value) => { setSort(value); setPage(0) }}
        options={[{ value: 'newest', label: 'Newest answered' }, { value: 'oldest', label: 'Oldest answered' }]} />
    </div>}
    {loading && <div className="ld-qa-loading"><Spin /> Loading answers…</div>}
    {loadError && <Alert type="error" role="alert" message={loadError} />}
    {!loading && !loadError && results.items.length === 0 && <div className="ld-qa-empty">No {browseAll && search ? 'matching ' : ''}answered questions yet.</div>}
    {!loading && !loadError && results.items.map((item) => <article className="ld-qa-entry" key={item.id}>
      <button type="button" className="ld-qa-expand" aria-expanded={expanded === item.id}
        aria-controls={`ld-qa-answer-${item.id}`} onClick={() => setExpanded((open) => open === item.id ? null : item.id)}>
        <span><small>Answered {questionDate(item.answeredAt)}</small><span className="ld-qa-question-text">{item.question}</span></span>
        {expanded === item.id ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
      </button>
      {expanded === item.id && <div className="ld-qa-answer" id={`ld-qa-answer-${item.id}`}>
        <span>Designer answer · {project.designer.name} · {questionDate(item.answeredAt)}</span>
        <p>{item.answer}</p>
      </div>}
    </article>)}
    <div className="ld-qa-browse-footer"><span>{browseAll ? 'Search or move between pages to find more answers.' :
      results.total > QA_PREVIEW_SIZE ? `Showing the ${QA_PREVIEW_SIZE} most recent answers.` : ''}</span>
      {results.total > QA_PREVIEW_SIZE || browseAll ? <Button onClick={toggleBrowse}>
        {browseAll ? 'Show fewer questions' : 'Browse all answered questions'} {browseAll ? '↑' : '↓'}</Button> : null}</div>
    {browseAll && !loading && !loadError && results.totalPages > 1 && <div className="ld-qa-pagination">
      <span>{pageLabel(page, results.totalPages)} · {results.total} results</span>
      <div><Button disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</Button>
        <Button disabled={page + 1 >= results.totalPages} onClick={() => setPage((p) => p + 1)}>Next</Button></div>
    </div>}
    <p className="ld-qa-privacy"><MessageCircleQuestion size={16} aria-hidden="true" /> Unanswered questions stay private. Answered questions appear without the customer's name.</p>
    <Modal title="Log in to ask a question" open={loginOpen} onCancel={() => setLoginOpen(false)}
      footer={<><Button onClick={() => setLoginOpen(false)}>Cancel</Button>
        <Button type="primary" onClick={() => navigate('/login', { state: { returnTo: `/projects/${project.id}` } })}>Log in</Button></>}>
      <p>Everyone can read answered questions. Only customers can submit new project questions.</p>
      {role === 'other' && <p>To ask a question, sign in with a customer account.</p>}
    </Modal>
  </section>
}
