import { Alert, Button, Card, Input, Select, Spin, Table, Tag } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentAdmin } from '../api/designerAccounts.js'
import { searchAdminAccounts } from '../api/adminAccounts.js'
import {
  INITIAL_ACCOUNT_SEARCH, accountRoleLabel, accountSearchFailureMessage, accountStatusLabel,
} from '../account/accountSearchRules.js'
import '../account/accountSearch.css'

const columns = [
  { title: 'Account ID', dataIndex: 'id', key: 'id', width: 130 },
  { title: 'Name', dataIndex: 'displayName', key: 'displayName' },
  { title: 'Email', dataIndex: 'email', key: 'email' },
  { title: 'Role', dataIndex: 'role', key: 'role', render: accountRoleLabel },
  { title: 'Status', dataIndex: 'active', key: 'active', render: (active) => (
    <Tag color={active ? 'success' : 'default'}>{accountStatusLabel(active)}</Tag>
  ) },
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

  const total = results?.totalElements ?? 0
  const entries = results?.accounts ?? []
  const start = page * 20 + 1
  const end = page * 20 + entries.length

  return (
    <div className="admin-account-search-page">
      <p className="admin-account-eyebrow">User administration</p>
      <h1>Search user accounts</h1>
      <p className="admin-account-intro">
        Find Renovation Customer and Interior Designer accounts to support account administration.
        This page is read-only; account changes are handled by separate features.
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
    </div>
  )
}
