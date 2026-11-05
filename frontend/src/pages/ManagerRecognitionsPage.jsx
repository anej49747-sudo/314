import ManagerReturnButton from '../components/ManagerReturnButton.jsx'
import { Alert, Button, Card, Input, Modal, Select, Spin, Table, Tabs, message } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentManager } from '../api/categories.js'
import { getDesignerDirectory } from '../api/designerDirectory.js'
import { listManagerProjects } from '../api/managerProjects.js'
import { getManagerAwards, getManagerFeature, grantManagerAward, removeManagerAward,
  removeManagerFeature, setManagerFeature } from '../api/recognitions.js'
import { designerAwards, normalizeAwardTitle, recognitionError, singaporeMonth,
  validFeatureMonth } from '../recognition/recognitionRules.js'
import '../recognition/recognition.css'

function confirmRecognition(title, content, destructive = false) {
  return new Promise((resolve) => {
    Modal.confirm({ title, content, okText: destructive ? 'Remove' : 'Confirm',
      okButtonProps: destructive ? { danger: true } : {},
      onOk: () => resolve(true), onCancel: () => resolve(false) })
  })
}

export default function ManagerRecognitionsPage() {
  const navigate = useNavigate()
  const [checking, setChecking] = useState(true)
  const [authorized, setAuthorized] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [projects, setProjects] = useState([])
  const [designers, setDesigners] = useState([])
  const [awards, setAwards] = useState([])
  const [month, setMonth] = useState(() => singaporeMonth())
  const [feature, setFeature] = useState(null)
  const [monthLoading, setMonthLoading] = useState(true)
  const [projectSearch, setProjectSearch] = useState('')
  const [selectedProject, setSelectedProject] = useState(null)
  const [selectedDesigner, setSelectedDesigner] = useState(null)
  const [awardTitle, setAwardTitle] = useState('')
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let active = true
    currentManager().then(() => { if (active) setAuthorized(true) }).catch((failure) => {
      if (!active) return
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setLoadError('Unable to verify Platform Manager access.')
    }).finally(() => { if (active) setChecking(false) })
    return () => { active = false }
  }, [navigate])

  useEffect(() => {
    if (!authorized) return undefined
    let active = true
    Promise.all([listManagerProjects(), getDesignerDirectory(), getManagerAwards()])
      .then(([p, d, a]) => { if (active) { setProjects(p); setDesigners(d); setAwards(a) } })
      .catch((failure) => {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setLoadError('Unable to load projects and designer awards. Please retry.')
      })
    return () => { active = false }
  }, [authorized, navigate, refresh])

  useEffect(() => {
    if (!authorized || !validFeatureMonth(month)) return undefined
    let active = true
    setMonthLoading(true)
    getManagerFeature(month).then((data) => { if (active) setFeature(data.selection) })
      .catch(() => { if (active) setLoadError('Unable to load the selected month.') })
      .finally(() => { if (active) setMonthLoading(false) })
    return () => { active = false }
  }, [authorized, month, refresh])

  const filteredProjects = useMemo(() => projects.filter((project) =>
    `${project.title} ${project.designerName}`.toLowerCase().includes(projectSearch.trim().toLowerCase())),
  [projects, projectSearch])
  const eligibleDesigners = designers.map((item) => ({ value: item.id, label: item.name }))
  const selectedDesignerAwards = designerAwards(awards, selectedDesigner)

  async function featureProject(project) {
    if (busy || !validFeatureMonth(month)) return
    const replacing = feature && feature.projectId !== project.id
    if (replacing && !(await confirmRecognition('Replace Project of the Month?',
      `This will replace the existing selection for ${month}. The project and its photos remain unchanged.`))) return
    setBusy(true)
    setActionError('')
    try {
      const result = await setManagerFeature(month, project.id)
      setFeature(result.selection)
      setSelectedProject(null)
      message.success('Project of the Month saved.')
    } catch (failure) { setActionError(recognitionError(failure)) }
    finally { setBusy(false) }
  }

  async function clearFeature() {
    if (busy || !feature) return
    if (!(await confirmRecognition('Remove monthly selection?',
      `Remove the featured project for ${month}? The project will not be deleted.`, true))) return
    setBusy(true)
    setActionError('')
    try {
      await removeManagerFeature(month)
      setFeature(null)
      message.success('Monthly feature removed.')
    } catch (failure) { setActionError(recognitionError(failure)) }
    finally { setBusy(false) }
  }

  async function grantAward() {
    if (busy) return
    setActionError('')
    let title
    try { title = normalizeAwardTitle(awardTitle) } catch (failure) {
      setActionError(failure.message); return
    }
    if (!selectedDesigner) { setActionError('Choose an Interior Designer first.'); return }
    if (selectedDesignerAwards.some((award) => award.title.toLowerCase() === title.toLowerCase())) {
      setActionError('This designer already has an award with the same title.'); return
    }
    setBusy(true)
    try {
      const saved = await grantManagerAward(selectedDesigner, title)
      setAwards((items) => [saved, ...items])
      setAwardTitle('')
      message.success('Award granted.')
    } catch (failure) { setActionError(recognitionError(failure)) }
    finally { setBusy(false) }
  }

  async function revokeAward(award) {
    if (busy) return
    if (!(await confirmRecognition('Remove designer award?',
      `Remove “${award.title}” from this designer? Other awards remain unchanged.`, true))) return
    setBusy(true)
    setActionError('')
    try {
      await removeManagerAward(award.id)
      setAwards((items) => items.filter((item) => item.id !== award.id))
      message.success('Award removed.')
    } catch (failure) { setActionError(recognitionError(failure)) }
    finally { setBusy(false) }
  }

  const projectColumns = [
    { title: 'Project', dataIndex: 'title', render: (value) => <strong>{value}</strong> },
    { title: 'Designer', dataIndex: 'designerName' },
    { title: 'Status', dataIndex: 'status', render: (status) => <span className={status === 'COMPLETED' ? 'recognition-status-completed' : 'recognition-status-current'}>{status === 'COMPLETED' ? 'Completed' : 'Current'}</span> },
    { title: 'Action', render: (_, project) => <Button disabled={busy} onClick={() => setSelectedProject(project)}>Select</Button> },
  ]

  const projectPanel = <>
    <Card className="recognition-manager-card" title="Project of the Month">
      <p>Choose one eligible published project for each calendar month. Earlier monthly selections remain available.</p>
      <div className="recognition-manager-row">
        <label className="recognition-manager-field"><span>Feature month</span>
          <div className="recognition-month-picker">
            <Select aria-label="Feature month" value={Number(month.slice(5, 7))}
              options={Array.from({ length: 12 }, (_, index) => ({
                value: index + 1,
                label: new Intl.DateTimeFormat('en-SG', { month: 'long', timeZone: 'UTC' })
                  .format(new Date(Date.UTC(2026, index, 1))),
              }))}
              onChange={(value) => { setMonth(`${month.slice(0, 4)}-${String(value).padStart(2, '0')}`); setSelectedProject(null); setActionError('') }} />
            <Input aria-label="Feature year" type="number" min={1} max={9999} step={1}
              value={Number(month.slice(0, 4))}
              onChange={(event) => { const year = Number(event.target.value)
                if (Number.isInteger(year) && year >= 1 && year <= 9999) {
                  setMonth(`${String(year).padStart(4, '0')}-${month.slice(5, 7)}`)
                  setSelectedProject(null); setActionError('')
                }
              }} />
          </div>
        </label>
      </div>
      {monthLoading ? <Spin /> : <div className="recognition-manager-selected">
        {feature ? <div className="recognition-feature-preview">
          {feature.coverUrl && <img src={feature.coverUrl} alt={`Featured project: ${feature.projectTitle}`} loading="lazy" />}
          <div className="recognition-feature-preview-content">
            <strong>Selected for {month}</strong>
            <p>{feature.projectTitle} (Project #{feature.projectId})</p>
            <Button danger disabled={busy} onClick={clearFeature}>Remove</Button>
          </div>
        </div>
          : <p>No project selected for {month}.</p>}
      </div>}
      {selectedProject && <div className="recognition-manager-selected">
        <strong>Ready to feature:</strong> {selectedProject.title}
        <div className="recognition-manager-actions">
          <Button type="primary" disabled={busy || !validFeatureMonth(month)} loading={busy}
            onClick={() => featureProject(selectedProject)}>
            {feature ? 'Save / replace selection' : 'Feature project'}
          </Button>
          <Button onClick={() => setSelectedProject(null)}>Cancel</Button>
        </div>
      </div>}
    </Card>
    <Card title="Find a project" className="recognition-manager-card">
      <Input.Search aria-label="Search projects" placeholder="Search by project or designer name"
        value={projectSearch} onChange={(event) => setProjectSearch(event.target.value)} allowClear />
      <Table style={{ marginTop: 18 }} rowKey="id" dataSource={filteredProjects} columns={projectColumns}
        pagination={{ pageSize: 8 }} scroll={{ x: 650 }} />
    </Card>
  </>

  const awardsPanel = <>
    <Card className="recognition-manager-card" title="Award Interior Designers">
      <p>Give an active designer one or more named awards. Only the award title is public; no description is needed.</p>
      <div className="recognition-manager-row">
        <label className="recognition-manager-field"><span>Interior Designer</span>
          <Select style={{ width: 315 }} showSearch optionFilterProp="label" options={eligibleDesigners}
            placeholder="Choose designer" value={selectedDesigner}
            onChange={(id) => { setSelectedDesigner(id); setActionError('') }} />
        </label>
        <label className="recognition-manager-field"><span>Award title</span>
          <Input style={{ width: 340 }} maxLength={70} showCount value={awardTitle}
            placeholder="e.g. Best Space Planning" onChange={(event) => setAwardTitle(event.target.value)} />
        </label>
        <Button type="primary" disabled={!selectedDesigner || busy} loading={busy} onClick={grantAward}>
          Grant award
        </Button>
      </div>
      <div className="recognition-manager-awards-list">
        <p className="recognition-manager-panel-label">{selectedDesigner
          ? `Awards (${selectedDesignerAwards.length})` : 'Select a designer to view their awards'}</p>
        {selectedDesigner && selectedDesignerAwards.length === 0 && <p className="recognition-manager-muted">No awards yet.</p>}
        {selectedDesignerAwards.map((award) => <div className="recognition-manager-award-item" key={award.id}>
          <span className="recognition-manager-award-text">★ <strong>{award.title}</strong>
            <span className="recognition-manager-muted"> · {new Date(award.awardedAt).toLocaleDateString('en-SG')}</span></span>
          <Button danger size="small" disabled={busy} onClick={() => revokeAward(award)}>Remove</Button>
        </div>)}
      </div>
    </Card>
  </>

  return <section className="recognition-manager-page">
    <ManagerReturnButton />
    <p className="categories-eyebrow">Platform management</p>
    <h1>Project features &amp; designer awards</h1>
    <p className="recognition-subtitle">Recognise renovation projects and Interior Designers. Changes appear on public pages.</p>
    {checking && <Spin />}
    {loadError && <Alert showIcon role="alert" type="error" message={loadError}
      action={<Button onClick={() => { setLoadError(''); setRefresh((value) => value + 1) }}>Retry</Button>} />}
    {actionError && <Alert showIcon role="alert" type="error" message={actionError} closable onClose={() => setActionError('')} />}
    {authorized && !checking && <Tabs items={[
      { key: 'projects', label: 'Project of the Month', children: projectPanel },
      { key: 'awards', label: 'Designer awards', children: awardsPanel },
    ]} />}
  </section>
}
