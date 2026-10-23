import { Alert, Button, Card, Drawer, Input, Modal, Select, Spin, Table, Tag, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentAdmin } from '../api/designerAccounts.js'
import { searchAdminAccounts } from '../api/adminAccounts.js'
import { fetchAccountLoginHistory, reactivateAdminAccount, suspendAdminAccount } from '../api/adminAccountManagement.js'
import { deleteDesignerAccount } from '../api/accountDeletion.js'
import { accountDeletionError, canPermanentlyDelete } from '../account/accountDeletionRules.js'
import {
  INITIAL_ACCOUNT_SEARCH, accountRoleLabel, accountSearchFailureMessage, accountStatusLabel,
} from '../account/accountSearchRules.js'
import {
  historyErrorMessage, loginOutcomeLabel, moderationActionLabel, moderationErrorMessage,
  singaporeLoginTime,
} from '../account/accountManagementRules.js'
import '../account/accountSearch.css'
import '../account/accountDeletion.css'

const historyColumns = [
  { title: 'Date and time (SGT)', dataIndex: 'occurredAt', key: 'occurredAt',
    render: singaporeLoginTime },
  { title: 'Result', dataIndex: 'successful', key: 'successful',
    render: (successful) => <Tag color={successful ? 'success' : 'error'}>{loginOutcomeLabel(successful)}</Tag> },
]

export default function AdminAccountSearchPage() {
  const navigate = useNavigate()
  const [checking, setChecking] = useState(true)
  const [authorized, setAuthorized] = useState(false)
  const [authorizationError, setAuthorizationError] = useState('')
  const [draft, setDraft] = useState({ ...INITIAL_ACCOUNT_SEARCH })
  const [applied, setApplied] = useState({ ...INITIAL_ACCOUNT_SEARCH })
  const [page, setPage] = useState(0)
  const [refresh, setRefresh] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [results, setResults] = useState(null)
  const [moderation, setModeration] = useState(null)
  const [reason, setReason] = useState('')
  const [moderationError, setModerationError] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleteConfirmation, setDeleteConfirmation] = useState('')
  const [deleteError, setDeleteError] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [historyAccount, setHistoryAccount] = useState(null)
  const [historyOutcome, setHistoryOutcome] = useState('all')
  const [historyPage, setHistoryPage] = useState(0)
  const [history, setHistory] = useState(null)
  const [historyError, setHistoryError] = useState('')
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyRefresh, setHistoryRefresh] = useState(0)

  useEffect(() => {
    let active = true
    currentAdmin().then(() => {
      if (active) setAuthorized(true)
    }).catch((failure) => {
      if (!active) return
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setAuthorizationError('Unable to verify User Admin access. Please try again.')
    }).finally(() => { if (active) setChecking(false) })
    return () => { active = false }
  }, [navigate])

  useEffect(() => {
    if (!authorized) return undefined
    const controller = new AbortController()
    setLoading(true)
    setError('')
    searchAdminAccounts(applied, page, controller.signal).then((data) => {
      if (!controller.signal.aborted) setResults(data)
    }).catch((failure) => {
      if (controller.signal.aborted) return
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setError(accountSearchFailureMessage(failure))
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false)
    })
    return () => { controller.abort() }
  }, [authorized, applied, page, refresh, navigate])

  useEffect(() => {
    if (!historyAccount) return undefined
    const controller = new AbortController()
    setHistoryLoading(true)
    setHistoryError('')
    fetchAccountLoginHistory(historyAccount.id, historyOutcome, historyPage, controller.signal)
      .then((data) => { if (!controller.signal.aborted) setHistory(data) })
      .catch((failure) => {
        if (controller.signal.aborted) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setHistoryError(historyErrorMessage(failure))
      }).finally(() => { if (!controller.signal.aborted) setHistoryLoading(false) })
    return () => { controller.abort() }
  }, [historyAccount, historyOutcome, historyPage, historyRefresh, navigate])

  function submit(event) {
    event.preventDefault()
    setPage(0)
    setApplied({ ...draft })
  }

  function reset() {
    setDraft({ ...INITIAL_ACCOUNT_SEARCH })
    setApplied({ ...INITIAL_ACCOUNT_SEARCH })
    setPage(0)
  }

  function openModeration(account) {
    setModeration(account)
    setReason('')
    setModerationError('')
  }

  async function confirmModeration() {
    if (!moderation || saving) return
    if (moderation.active && !reason.trim()) {
      setModerationError('Enter a suspension reason.')
      return
    }
    setSaving(true)
    setModerationError('')
    try {
      if (moderation.active) await suspendAdminAccount(moderation.id, reason.trim())
      else await reactivateAdminAccount(moderation.id)
      message.success(moderation.active ? 'Account suspended.' : 'Account reactivated.')
      setModeration(null)
      setRefresh((value) => value + 1)
    } catch (failure) {
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setModerationError(moderationErrorMessage(failure))
    } finally {
      setSaving(false)
    }
  }

  function openDelete(account) {
    if (account.role !== 'INTERIOR_DESIGNER') return
    setDeleteTarget(account)
    setDeleteConfirmation('')
    setDeleteError('')
  }

  async function confirmDelete() {
    if (!deleteTarget || deleting || !canPermanentlyDelete(deleteConfirmation)) return
    setDeleting(true)
    setDeleteError('')
    try {
      await deleteDesignerAccount(deleteTarget.id)
      message.success('Designer account permanently deleted.')
      setDeleteTarget(null)
      setPage(0)
      setRefresh((value) => value + 1)
    } catch (failure) {
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setDeleteError(accountDeletionError(failure))
    } finally {
      setDeleting(false)
    }
  }

  function openHistory(account) {
    setHistoryAccount(account)
    setHistoryOutcome('all')
    setHistoryPage(0)
    setHistory(null)
    setHistoryError('')
  }

  const columns = [
    { title: 'Account ID', dataIndex: 'id', key: 'id', width: 110 },
    { title: 'Name', dataIndex: 'displayName', key: 'displayName' },
    { title: 'Email', dataIndex: 'email', key: 'email' },
    { title: 'Role', dataIndex: 'role', key: 'role', render: accountRoleLabel },
    { title: 'Status', dataIndex: 'active', key: 'active', render: (active) => (
      <Tag color={active ? 'success' : 'default'}>{accountStatusLabel(active)}</Tag>
    ) },
    { title: 'Actions', key: 'actions', width: 365, render: (_, account) => (
      <div className="admin-account-row-actions">
        <Button size="small" onClick={() => openHistory(account)}>Login history</Button>
        <Button size="small" danger={account.active} onClick={() => openModeration(account)}>
          {moderationActionLabel(account.active)}
        </Button>
        {account.role === 'INTERIOR_DESIGNER' && <Button size="small" danger
          onClick={() => openDelete(account)}>Delete account</Button>}
      </div>
    ) },
  ]

  const total = results?.totalElements ?? 0
  const entries = results?.accounts ?? []
  const start = page * 20 + 1
  const end = page * 20 + entries.length
  const historyTotal = history?.totalElements ?? 0

  return (
    <div className="admin-account-search-page">
      <p className="admin-account-eyebrow">User administration</p>
      <h1>Manage user accounts</h1>
      <p className="admin-account-intro">
        Find Renovation Customer and Interior Designer accounts, manage their access,
        and review recorded login attempts. Privileged accounts are not shown.
      </p>
      {checking && <div className="loading-state"><Spin /> Checking User Admin access…</div>}
      {authorizationError && <Alert type="error" showIcon message={authorizationError} role="alert" />}
      {authorized && <>
        <Card className="admin-account-panel" title="Find an account">
          <form onSubmit={submit}>
            <div className="admin-account-filters">
              <label>Name or email
                <Input type="search" value={draft.query} maxLength={100} autoComplete="off"
                  placeholder="Search by name or email" onChange={(event) =>
                    setDraft((current) => ({ ...current, query: event.target.value }))} />
              </label>
              <label>Account role
                <Select value={draft.role} options={[
                  { label: 'All roles', value: '' },
                  { label: 'Renovation Customer', value: 'CUSTOMER' },
                  { label: 'Interior Designer', value: 'INTERIOR_DESIGNER' },
                ]} onChange={(role) => setDraft((current) => ({ ...current, role }))} />
              </label>
              <label>Account status
                <Select value={draft.status} options={[
                  { label: 'All statuses', value: '' },
                  { label: 'Active', value: 'active' },
                  { label: 'Inactive', value: 'inactive' },
                ]} onChange={(status) => setDraft((current) => ({ ...current, status }))} />
              </label>
            </div>
            <div className="admin-account-actions">
              <Button type="primary" htmlType="submit" disabled={loading}>Search accounts</Button>
              <Button htmlType="button" onClick={reset}>Reset filters</Button>
            </div>
            <p className="admin-account-hint">Press Enter or select “Search accounts” to apply the search and filters.</p>
          </form>
        </Card>
        <Card className="admin-account-panel" title="Account results"
          extra={!loading && !error ? `${total} ${total === 1 ? 'account' : 'accounts'}` : undefined}>
          {error && <Alert type="error" showIcon role="alert" message={error}
            action={<Button size="small" onClick={() => setRefresh((value) => value + 1)}>Try again</Button>} />}
          {!error && <>
            <div className="admin-account-table-wrap">
              <Table columns={columns} dataSource={entries} rowKey="id" pagination={false}
                loading={loading} locale={{ emptyText: 'No accounts found. Try different criteria or reset the filters.' }} />
            </div>
            {!loading && total > 0 && <div className="admin-account-pagination">
              <span>Showing {start}–{end} of {total} accounts</span>
              <div className="admin-account-page-actions">
                <Button disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</Button>
                <span>Page {page + 1} of {results?.totalPages ?? 1}</span>
                <Button disabled={page + 1 >= (results?.totalPages ?? 1)}
                  onClick={() => setPage((value) => value + 1)}>Next</Button>
              </div>
            </div>}
          </>}
        </Card>
      </>}
      <Modal title={moderation?.active ? 'Suspend account' : 'Reactivate account'}
        open={!!moderation} onCancel={() => { if (!saving) setModeration(null) }}
        onOk={confirmModeration} okText={moderation?.active ? 'Suspend account' : 'Reactivate account'}
        okButtonProps={{ danger: !!moderation?.active, disabled: moderation?.active && !reason.trim() }}
        confirmLoading={saving} cancelButtonProps={{ disabled: saving }} destroyOnHidden>
        <p>{moderation?.displayName} ({moderation?.email})</p>
        {moderation?.active ? <>
          <p>Suspension blocks new sign-ins and revokes the account’s existing authenticated access.</p>
          <label className="admin-account-reason-label">Internal suspension reason
            <Input.TextArea value={reason} rows={3} maxLength={500} showCount
              placeholder="Explain why access is being suspended" onChange={(event) => setReason(event.target.value)} />
          </label>
        </> : <p>Reactivation permits this account to sign in again. Old sessions are not restored.</p>}
        {moderationError && <Alert className="admin-account-dialog-error" type="error" role="alert"
          showIcon message={moderationError} />}
      </Modal>
      <Modal title="Permanently delete designer account?" open={!!deleteTarget}
        onCancel={() => { if (!deleting) setDeleteTarget(null) }}
        onOk={confirmDelete} okText="Permanently delete account"
        okButtonProps={{ danger: true, disabled: !canPermanentlyDelete(deleteConfirmation) }}
        confirmLoading={deleting} closable={!deleting}>
        <p>This will permanently delete the designer and their company, projects,
          photos, reviews and related records. This cannot be undone.</p>
        <p>Anonymous historical platform activity totals will remain.</p>
        {deleteTarget && <p><strong>{deleteTarget.displayName}</strong> — {deleteTarget.email}</p>}
        <label className="account-deletion-confirm-label" htmlFor="admin-delete-confirm">
          Type <strong>DELETE</strong> to confirm
        </label>
        <Input id="admin-delete-confirm" value={deleteConfirmation} autoComplete="off"
          onChange={(event) => setDeleteConfirmation(event.target.value)} disabled={deleting} />
        {deleteError && <Alert type="error" showIcon role="alert" message={deleteError}
          className="account-deletion-error" />}
      </Modal>
      <Drawer title="Login history" open={!!historyAccount} width={560}
        onClose={() => setHistoryAccount(null)} destroyOnHidden>
        {historyAccount && <>
          <p className="admin-account-history-person">
            <strong>{historyAccount.displayName}</strong> — {historyAccount.email}
          </p>
          <p className="admin-account-hint">Only attempts recorded since login-history tracking was enabled are available.
            Times are displayed in Singapore time (SGT).</p>
          <label className="admin-account-history-filter">Outcome
            <Select value={historyOutcome} options={[
              { label: 'All attempts', value: 'all' },
              { label: 'Successful', value: 'success' },
              { label: 'Failed', value: 'failed' },
            ]} onChange={(outcome) => { setHistoryOutcome(outcome); setHistoryPage(0) }} />
          </label>
          {historyError && <Alert role="alert" type="error" showIcon message={historyError}
            action={<Button size="small" onClick={() => setHistoryRefresh((value) => value + 1)}>Retry</Button>} />}
          {!historyError && <>
            <Table size="small" columns={historyColumns} dataSource={history?.events ?? []}
              rowKey="id" pagination={false} loading={historyLoading}
              locale={{ emptyText: 'No recorded login attempts for this selection.' }} />
            {!historyLoading && historyTotal > 0 && <div className="admin-account-pagination">
              <span>{historyTotal} attempt{historyTotal === 1 ? '' : 's'}</span>
              <div className="admin-account-page-actions">
                <Button size="small" disabled={historyPage === 0}
                  onClick={() => setHistoryPage((current) => current - 1)}>Previous</Button>
                <span>Page {historyPage + 1} of {history?.totalPages ?? 1}</span>
                <Button size="small" disabled={historyPage + 1 >= (history?.totalPages ?? 1)}
                  onClick={() => setHistoryPage((current) => current + 1)}>Next</Button>
              </div>
            </div>}
          </>}
        </>}
      </Drawer>
    </div>
  )
}
