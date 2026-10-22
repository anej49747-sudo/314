import { Alert, Button, Card, Drawer, Input, Select, Spin, Table, Tag, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentAdmin } from '../api/designerAccounts.js'
import { decideDesignerReport, getDesignerReport, searchDesignerReports } from '../api/designerReports.js'
import {
  reportErrorMessage, reportReasonLabel, reportTimestamp, validateReportDecision,
} from '../dispute/reportRules.js'
import '../dispute/designerReports.css'

const statusColors = { OPEN: 'warning', RESOLVED: 'success', DISMISSED: 'default' }

function ReportStatus({ status }) {
  return <Tag color={statusColors[status] ?? 'default'}>{status}</Tag>
}

export default function AdminDesignerReportsPage() {
  const navigate = useNavigate()
  const [access, setAccess] = useState('loading')
  const [draft, setDraft] = useState('')
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('OPEN')
  const [page, setPage] = useState(0)
  const [refresh, setRefresh] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')
  const [outcome, setOutcome] = useState('RESOLVED')
  const [note, setNote] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let active = true
    currentAdmin().then(() => { if (active) setAccess('admin') })
      .catch((failure) => {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setAccess('error')
      })
    return () => { active = false }
  }, [navigate])

  useEffect(() => {
    if (access !== 'admin') return undefined
    const controller = new AbortController()
    setLoading(true)
    setError('')
    searchDesignerReports({ status, q: query, page }, controller.signal).then((result) => {
      if (!controller.signal.aborted) setData(result)
    }).catch((failure) => {
      if (controller.signal.aborted) return
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setError(reportErrorMessage(failure, true))
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => { controller.abort() }
  }, [access, status, query, page, refresh, navigate])

  async function openDetail(id) {
    setSelected(null)
    setDetailLoading(true)
    setDetailError('')
    setNote('')
    setOutcome('RESOLVED')
    try {
      setSelected(await getDesignerReport(id))
    } catch (failure) {
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setDetailError(reportErrorMessage(failure, true))
    } finally {
      setDetailLoading(false)
    }
  }

  async function confirmDecision() {
    if (!selected || saving) return
    const invalid = validateReportDecision(outcome, note)
    if (invalid) { setDetailError(invalid); return }
    setSaving(true)
    setDetailError('')
    try {
      const updated = await decideDesignerReport(selected.id, { outcome, note: note.trim() })
      setSelected(updated)
      setRefresh((value) => value + 1)
      message.success(`Report REF-${updated.id} marked ${updated.status.toLowerCase()}.`)
    } catch (failure) {
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setDetailError(reportErrorMessage(failure, true))
    } finally {
      setSaving(false)
    }
  }

  const columns = [
    { title: 'Report', dataIndex: 'id', render: (id) => `REF-${id}` },
    { title: 'Designer', dataIndex: 'designerName' },
    { title: 'Customer', dataIndex: 'customerName' },
    { title: 'Reason', dataIndex: 'reason', render: reportReasonLabel },
    { title: 'Submitted (SGT)', dataIndex: 'submittedAt', render: reportTimestamp },
    { title: 'Status', dataIndex: 'status', render: (value) => <ReportStatus status={value} /> },
    { title: 'Action', dataIndex: 'id', render: (id) => (
      <Button onClick={() => openDetail(id)}>Review details</Button>
    ) },
  ]

  if (access === 'loading') return <div role="status"><Spin /> Checking administrator access…</div>
  if (access === 'error') return <Alert type="error" showIcon message="Unable to verify User Admin access." />
  if (access !== 'admin') return null

  return <section className="admin-designer-reports" aria-labelledby="designer-reports-title">
    <p className="report-eyebrow">User administration</p>
    <h1 id="designer-reports-title" className="report-heading">Designer reports</h1>
    <p className="report-intro">Review private customer concerns and record an outcome. No action automatically suspends a designer.</p>
    <div className="report-stat-row">
      {[
        ['Open reports', data?.openCount], ['Resolved', data?.resolvedCount], ['Dismissed', data?.dismissedCount],
      ].map(([label, value]) => <Card key={label} size="small">
        <div>{label}</div><div className="report-stat-value">{value ?? '—'}</div>
      </Card>)}
    </div>
    <Card>
      <form className="report-search-fields" onSubmit={(event) => {
        event.preventDefault(); setPage(0); setQuery(draft.trim())
      }}>
        <div className="report-search-text">
          <label htmlFor="report-search">Search by report reference, customer or designer</label>
          <Input id="report-search" value={draft} maxLength={100}
            onChange={(event) => setDraft(event.target.value)} placeholder="Search reports" />
        </div>
        <div>
          <label htmlFor="report-status">Status</label>
          <Select id="report-status" value={status} style={{ width: 180 }}
            options={[{ value: 'OPEN', label: 'Open' }, { value: 'ALL', label: 'All statuses' },
              { value: 'RESOLVED', label: 'Resolved' }, { value: 'DISMISSED', label: 'Dismissed' }]}
            onChange={(value) => { setStatus(value); setPage(0) }} />
        </div>
        <Button htmlType="submit" type="primary">Search</Button>
        <Button onClick={() => { setDraft(''); setQuery(''); setStatus('OPEN'); setPage(0) }}>Reset</Button>
      </form>
      {error && <Alert type="error" role="alert" showIcon message={error}
        action={<Button size="small" onClick={() => setRefresh((v) => v + 1)}>Retry</Button>} />}
      <div className="report-table-container">
        <Table rowKey="id" loading={loading} columns={columns} dataSource={data?.reports ?? []}
          locale={{ emptyText: 'No matching reports' }}
          pagination={{ current: page + 1, pageSize: 20, total: data?.totalElements ?? 0,
            showSizeChanger: false, onChange: (next) => setPage(next - 1) }} />
      </div>
    </Card>
    <Drawer open={Boolean(selected) || detailLoading || Boolean(detailError)} width={690}
      title={selected ? `Report REF-${selected.id}` : 'Report details'}
      onClose={() => { if (!saving) { setSelected(null); setDetailError(''); setDetailLoading(false) } }}
      destroyOnHidden>
      {detailLoading && <Spin />}
      {detailError && <Alert type="error" showIcon message={detailError} role="alert" />}
      {selected && <>
        <div className="report-detail-field"><strong>Status</strong><ReportStatus status={selected.status} /></div>
        <div className="report-detail-field"><strong>Designer</strong>{selected.designerName}</div>
        <div className="report-detail-field"><strong>Reported by</strong>{selected.customerName}</div>
        <div className="report-detail-field"><strong>Reason</strong>{reportReasonLabel(selected.reason)}</div>
        <div className="report-detail-field"><strong>Submitted (SGT)</strong>{reportTimestamp(selected.submittedAt)}</div>
        <div className="report-detail-field"><strong>Customer description</strong>
          <p className="report-detail-text">{selected.description}</p>
        </div>
        {selected.status === 'OPEN' ? <div className="report-outcome-form">
          <strong>Record an outcome</strong>
          <p>Only the User Admin team can read these internal decision notes.</p>
          <Select value={outcome} onChange={setOutcome} options={[
            { value: 'RESOLVED', label: 'Resolve report' },
            { value: 'DISMISSED', label: 'Dismiss report' },
          ]} />
          <label htmlFor="designer-report-decision">Internal decision note *</label>
          <Input.TextArea id="designer-report-decision" rows={4} maxLength={1000}
            value={note} onChange={(event) => setNote(event.target.value)} />
          <span className="designer-report-help">10–1,000 characters · {note.length} / 1000</span>
          <Button loading={saving} disabled={saving} type="primary" onClick={confirmDecision}>Confirm outcome</Button>
        </div> : <div className="report-outcome-form">
          <strong>Recorded outcome</strong>
          <p className="report-detail-text">{selected.decisionNote}</p>
          <div>Decided by account #{selected.decidedById ?? 'Unavailable'}</div>
          <div>Decided (SGT): {reportTimestamp(selected.decidedAt)}</div>
        </div>}
        <p className="designer-report-help">Reports and internal outcomes are not published on public profiles.</p>
      </>}
    </Drawer>
  </section>
}
