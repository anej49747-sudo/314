import DesignerReturnButton from '../components/DesignerReturnButton.jsx'
import { Alert, Button, Input, Select, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { currentDesigner, currentDesignerArea } from '../api/designerAuthentication.js'
import { getMyPortfolio } from '../api/portfolio.js'
import { answerProjectQuestion, getDesignerProjectQuestions } from '../api/projectQuestions.js'
import { answerValidationError, pageLabel, questionDate } from '../qa/questionRules.js'
import '../qa/projectQuestions.css'

const statusOptions = [
  { value: 'pending', label: 'Awaiting response' },
  { value: 'answered', label: 'Answered' },
  { value: 'all', label: 'All questions' },
]

export default function DesignerProjectQuestionsPage() {
  const navigate = useNavigate()
  const [authorized, setAuthorized] = useState(false)
  const [portfolio, setPortfolio] = useState([])
  const [projectError, setProjectError] = useState('')
  const [status, setStatus] = useState('pending')
  const [projectId, setProjectId] = useState('')
  const [query, setQuery] = useState('')
  const [delayedQuery, setDelayedQuery] = useState('')
  const [page, setPage] = useState(0)
  const [results, setResults] = useState({ items: [], total: 0, totalPages: 0, pendingCount: 0, answeredCount: 0, allCount: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [replyingTo, setReplyingTo] = useState(null)
  const [reply, setReply] = useState('')
  const [busy, setBusy] = useState(false)
  const [replyError, setReplyError] = useState('')
  const [success, setSuccess] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    async function verify() {
      try {
        const me = await currentDesigner()
        if (!active) return
        if (me.passwordChangeRequired) {
          navigate('/designer/change-password', { replace: true }); return
        }
        await currentDesignerArea()
        if (!active) return
        setAuthorized(true)
        try { const list = await getMyPortfolio(); if (active) setPortfolio(list) }
        catch { if (active) setProjectError('Project filtering is unavailable. You can still answer questions.') }
      } catch (failure) {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to access project questions. Please try again.')
      } finally { if (active) setLoading(false) }
    }
    verify()
    return () => { active = false }
  }, [navigate])

  useEffect(() => {
    const handle = setTimeout(() => setDelayedQuery(query), 250)
    return () => clearTimeout(handle)
  }, [query])

  useEffect(() => {
    if (!authorized) return undefined
    let active = true
    setLoading(true)
    getDesignerProjectQuestions({ status, projectId, query: delayedQuery, page }).then((data) => {
      if (active) { setResults(data); setError('') }
    }).catch(() => { if (active) setError('Unable to load questions. Please try again.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [authorized, status, projectId, delayedQuery, page, reloadKey])

  async function publish(event, id) {
    event.preventDefault()
    const problem = answerValidationError(reply)
    if (busy || problem) { setReplyError(problem); return }
    setBusy(true)
    setReplyError('')
    try {
      await answerProjectQuestion(id, reply.trim())
      setReplyingTo(null)
      setReply('')
      setSuccess('Your answer is published on the project page.')
      setPage(0)
      setReloadKey((k) => k + 1)
    } catch (failure) {
      setReplyError(failure.response?.status === 409 ? 'This question has already been answered.' :
        'Unable to publish the answer. Please try again.')
    } finally { setBusy(false) }
  }

  const setFilter = (value) => { setStatus(value); setPage(0); setReplyingTo(null); setSuccess('') }
  const counts = { pending: results.pendingCount, answered: results.answeredCount, all: results.allCount }
  return <section className="ld-qa-designer-page" aria-labelledby="designer-qa-title">
    <DesignerReturnButton />
    <header className="ld-qa-designer-header"><p className="ld-qa-eyebrow">Designer workspace</p>
      <h1 id="designer-qa-title">Project questions</h1>
      <p>Review customer enquiries and publish answers about your own portfolio projects.</p></header>
    <div className="ld-qa-stats" aria-label="Question summary">
      <div><span>Awaiting response</span><strong>{results.pendingCount}</strong><small>Private until answered</small></div>
      <div><span>Answered</span><strong>{results.answeredCount}</strong><small>Public on project pages</small></div>
      <div><span>All questions</span><strong>{results.allCount}</strong><small>Across your projects</small></div>
    </div>
    {success && <Alert type="success" role="status" showIcon message={success} className="ld-qa-feedback" />}
    {error && <Alert type="error" role="alert" showIcon message={error} className="ld-qa-feedback" />}
    {projectError && <Alert type="warning" showIcon message={projectError} className="ld-qa-feedback" />}
    <div className="ld-qa-designer-layout">
      <aside className="ld-qa-filters"><h2>Question status</h2>
        <nav aria-label="Question status">
          {statusOptions.map((option) => <button key={option.value} type="button"
            aria-current={status === option.value ? 'true' : undefined}
            className={`ld-qa-filter${status === option.value ? ' selected' : ''}`}
            onClick={() => setFilter(option.value)}>{option.label}<span>{counts[option.value]}</span></button>)}
        </nav><p>You can answer questions only about your projects. Publishing a reply makes the question and answer visible to visitors.</p>
      </aside>
      <section className="ld-qa-queue" aria-labelledby="ld-qa-queue-heading">
        <h2 id="ld-qa-queue-heading">{statusOptions.find((option) => option.value === status).label}</h2>
        <p>{status === 'pending' ? 'Questions needing your reply, oldest first.' :
          status === 'answered' ? 'Your published project answers, newest first.' : 'Questions across your projects.'}</p>
        <div className="ld-qa-queue-tools">
          <Input.Search aria-label="Search questions" placeholder="Find a question…" value={query} maxLength={100}
            onChange={(event) => { setQuery(event.target.value); setPage(0) }} allowClear />
          <Select aria-label="Filter by project" value={projectId} onChange={(value) => { setProjectId(value); setPage(0) }}
            options={[{ value: '', label: 'All my projects' }, ...portfolio.map((item) => ({ value: String(item.id), label: item.title }))]} />
        </div>
        {loading && <div className="ld-qa-loading"><Spin /> Loading questions…</div>}
        {!loading && !error && results.items.length === 0 && <div className="ld-qa-empty">No questions match your filters.</div>}
        {!loading && results.items.map((item) => <article className="ld-qa-queue-item" key={item.id}>
          <div className="ld-qa-queue-top"><span className="ld-qa-queue-status">{item.answer ? 'Answered' : 'Awaiting response'}</span>
            <Link to={`/projects/${item.projectId}`}>View project</Link></div>
          <h3>{item.question}</h3>
          <div className="ld-qa-queue-meta">{item.projectTitle} · Asked {questionDate(item.createdAt)}</div>
          {item.answer ? <div className="ld-qa-answer"><span>Your answer · {questionDate(item.answeredAt)}</span><p>{item.answer}</p></div> : <>
            {replyingTo === item.id ? <form className="ld-qa-reply" onSubmit={(event) => publish(event, item.id)}>
              <label htmlFor={`designer-reply-${item.id}`}>Your answer</label>
              <Input.TextArea id={`designer-reply-${item.id}`} rows={4} maxLength={2000}
                placeholder="Share helpful details about this renovation…" value={reply}
                onChange={(event) => setReply(event.target.value)} />
              <div className="ld-qa-form-actions"><span>{reply.length} / 2000 characters</span>
                <div><Button onClick={() => { setReplyingTo(null); setReplyError('') }}>Cancel</Button>
                  <Button type="primary" htmlType="submit" loading={busy} disabled={Boolean(answerValidationError(reply)) || busy}>Publish answer</Button></div>
              </div>
              {replyError && <Alert type="error" role="alert" message={replyError} />}
            </form> : <Button onClick={() => { setReplyingTo(item.id); setReply(''); setReplyError(''); setSuccess('') }}>Write answer</Button>}
          </>}
        </article>)}
        {!loading && !error && results.totalPages > 1 && <div className="ld-qa-pagination">
          <span>{pageLabel(page, results.totalPages)} · {results.total} results</span>
          <div><Button disabled={page === 0} onClick={() => { setPage((p) => p - 1); setReplyingTo(null) }}>Previous</Button>
            <Button disabled={page + 1 >= results.totalPages} onClick={() => { setPage((p) => p + 1); setReplyingTo(null) }}>Next</Button></div>
        </div>}
      </section>
    </div>
  </section>
}
