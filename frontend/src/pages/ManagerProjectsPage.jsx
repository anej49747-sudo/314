import { Alert, Button, Card, Drawer, Form, Input, Select, Spin, Table, Tag, message } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentManager, listCategories } from '../api/categories.js'
import { correctManagerProject, listManagerProjects } from '../api/managerProjects.js'
import { categoryDisplayName, filterManagerProjects, managerProjectFailureMessage,
  managerProjectPayload } from '../manager/managerProjectRules.js'
import '../manager/managerProjects.css'

export default function ManagerProjectsPage() {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [authorized, setAuthorized] = useState(false)
  const [checking, setChecking] = useState(true)
  const [projects, setProjects] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filters, setFilters] = useState({ query: '', category: 'all', status: 'all' })
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let active = true
    currentManager().then(() => { if (active) setAuthorized(true) })
      .catch((failure) => {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to confirm Platform Manager access.')
      }).finally(() => { if (active) setChecking(false) })
    return () => { active = false }
  }, [navigate])

  useEffect(() => {
    if (!authorized) return undefined
    const controller = new AbortController()
    setLoading(true)
    setError('')
    Promise.all([listManagerProjects(controller.signal), listCategories()])
      .then(([entries, availableCategories]) => {
        if (!controller.signal.aborted) { setProjects(entries); setCategories(availableCategories) }
      }).catch((failure) => {
        if (controller.signal.aborted) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to load published projects. Please try again.')
      }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => { controller.abort() }
  }, [authorized, navigate, refresh])

  const filtered = useMemo(() => filterManagerProjects(projects, filters), [projects, filters])
  const filterCategoryOptions = [
    { value: 'all', label: 'All categories' },
    { value: 'uncategorised', label: 'Uncategorised' },
    ...categories.map((item) => ({ value: String(item.id), label: item.name })),
  ]

  function openEdit(project) {
    setSaveError('')
    setEditing(project)
    form.setFieldsValue({ title: project.title, description: project.description,
      categoryId: project.categoryId ?? 'uncategorised', status: project.status })
  }

  async function save(values) {
    if (!editing || saving) return
    setSaving(true)
    setSaveError('')
    try {
      const saved = await correctManagerProject(editing.id, managerProjectPayload(values))
      setEditing(null)
      setProjects((items) => items.map((item) => item.id === saved.id ? saved : item))
      message.success(`Project #${saved.id} updated. Published information now reflects the edit.`)
    } catch (failure) {
      const fields = failure.response?.data?.fieldErrors
      if (fields) form.setFields(Object.entries(fields).map(([name, value]) => ({ name, errors: [value] })))
      setSaveError(managerProjectFailureMessage(failure))
    } finally { setSaving(false) }
  }

  const columns = [
    { title: 'Project', dataIndex: 'title', key: 'title', render: (name, project) => <>
      <strong>{name}</strong><div className="manager-project-sub">Project #{project.id}</div></> },
    { title: 'Interior Designer', dataIndex: 'designerName', key: 'designerName' },
    { title: 'Category', key: 'category', render: (_, project) => categoryDisplayName(project) },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (status) =>
      <Tag color={status === 'COMPLETED' ? '#16a34a' : '#365b82'}>{status === 'COMPLETED' ? 'Completed' : 'Current'}</Tag> },
    { title: 'Action', key: 'action', render: (_, project) =>
      <Button onClick={() => openEdit(project)}>Edit</Button> },
  ]

  return <section className="manager-projects-page">
    <p className="categories-eyebrow">Platform management</p>
    <h1>Edit project information</h1>
    <p className="categories-intro">Correct inaccurate project information without changing its designer, photos, favourites or view counts.</p>
    {checking && <div className="loading-state"><Spin /> Checking manager access…</div>}
    {error && <Alert showIcon type="error" role="alert" message={error} action={authorized ?
      <Button size="small" onClick={() => setRefresh((n) => n + 1)}>Retry</Button> : undefined} />}
    {authorized && !checking && <>
      <Card className="manager-project-card" title="Find a published project">
        <div className="manager-project-filters">
          <label>Search title or designer
            <Input aria-label="Search title or designer" placeholder="Search projects" value={filters.query}
              onChange={(event) => setFilters((value) => ({ ...value, query: event.target.value }))} allowClear />
          </label>
          <label>Category
            <Select aria-label="Filter by category" value={filters.category} options={filterCategoryOptions}
              onChange={(category) => setFilters((value) => ({ ...value, category }))} />
          </label>
          <label>Status
            <Select aria-label="Filter by status" value={filters.status}
              options={[{ value: 'all', label: 'All statuses' }, { value: 'CURRENT', label: 'Current' },
                { value: 'COMPLETED', label: 'Completed' }]}
              onChange={(status) => setFilters((value) => ({ ...value, status }))} />
          </label>
          <Button onClick={() => setFilters({ query: '', category: 'all', status: 'all' })}>Reset filters</Button>
        </div>
      </Card>
      <Card className="manager-project-card" title={`Published projects (${filtered.length} of ${projects.length})`}>
        <Table rowKey="id" columns={columns} dataSource={filtered} loading={loading}
          pagination={{ pageSize: 10 }} scroll={{ x: 700 }} />
      </Card>
    </>}
    <Drawer title="Edit project information" width={550} open={Boolean(editing)}
      onClose={() => { if (!saving) setEditing(null) }} destroyOnHidden>
      {editing && <>
        <div className="manager-project-readonly">
          <p><strong>Project:</strong> #{editing.id}</p>
          <p><strong>Designer:</strong> {editing.designerName}</p>
          <p><strong>Photos:</strong> {editing.imageCount} retained</p>
          <p><strong>Favourites and views:</strong> Unchanged</p>
        </div>
        {saveError && <Alert type="error" role="alert" showIcon message={saveError} className="form-feedback" />}
        <Form form={form} layout="vertical" onFinish={save} disabled={saving}>
          <Form.Item name="title" label="Project title" rules={[{ required: true, whitespace: true,
            message: 'Enter a project title.' }, { max: 160, message: 'Maximum 160 characters.' }]}>
            <Input maxLength={160} />
          </Form.Item>
          <Form.Item name="description" label="Description" rules={[{ required: true, whitespace: true,
            message: 'Enter a project description.' }, { max: 3000, message: 'Maximum 3000 characters.' }]}>
            <Input.TextArea rows={5} maxLength={3000} showCount />
          </Form.Item>
          <Form.Item name="categoryId" label="Category">
            <Select options={[{ value: 'uncategorised', label: 'Uncategorised' },
              ...categories.map((item) => ({ value: item.id, label: item.name }))]} />
          </Form.Item>
          <Form.Item name="status" label="Project status" rules={[{ required: true }]}>
            <Select options={[{ value: 'CURRENT', label: 'Current' },
              { value: 'COMPLETED', label: 'Completed' }]} />
          </Form.Item>
          <Alert className="manager-project-note" type="info" showIcon
            message="Updates are published immediately. Project ownership and images cannot be changed here." />
          <div className="manager-project-actions">
            <Button onClick={() => setEditing(null)} disabled={saving}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={saving}>Save changes</Button>
          </div>
        </Form>
      </>}
    </Drawer>
  </section>
}
