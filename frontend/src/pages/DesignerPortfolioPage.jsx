import { Alert, Button, Card, Empty, Modal, Segmented, Spin, Tag } from 'antd'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { deletePortfolioProject, getMyPortfolio } from '../api/portfolio.js'
import { currentDesigner, currentDesignerArea } from '../api/designerAuthentication.js'
import { filteredPortfolio, portfolioStatusLabel, portfolioSummary } from '../designer/portfolioRules.js'
import { projectDeletionFailureMessage, withoutDeletedProject } from '../designer/projectDeletionRules.js'

const filters = [
  { label: 'All', value: 'ALL' },
  { label: 'Current', value: 'CURRENT' },
  { label: 'Completed', value: 'COMPLETED' },
]

export default function DesignerPortfolioPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [authorized, setAuthorized] = useState(false)
  const [loading, setLoading] = useState(true)
  const [projects, setProjects] = useState([])
  const [filter, setFilter] = useState('ALL')
  const [error, setError] = useState('')
  const [deletedProjectName, setDeletedProjectName] = useState('')
  const [deletionTarget, setDeletionTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    let active = true
    async function initialize() {
      try {
        const designer = await currentDesigner()
        if (!active) return
        if (designer.passwordChangeRequired) {
          navigate('/designer/change-password', { replace: true })
          return
        }
        await currentDesignerArea()
        const entries = await getMyPortfolio()
        if (!active) return
        setProjects(entries)
        setAuthorized(true)
      } catch (failure) {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to load your portfolio. Please try again.')
      } finally {
        if (active) setLoading(false)
      }
    }
    initialize()
    return () => { active = false }
  }, [navigate])

  async function confirmDeletion() {
    if (!deletionTarget || deleting) return
    setDeleting(true)
    setError('')
    try {
      await deletePortfolioProject(deletionTarget.id)
      setProjects((current) => withoutDeletedProject(current, deletionTarget.id))
      setDeletedProjectName(deletionTarget.title)
      setDeletionTarget(null)
    } catch (failure) {
      setError(projectDeletionFailureMessage(failure))
      if ([401, 403].includes(failure.response?.status)) {
        setDeletionTarget(null)
        navigate('/login', { replace: true })
      }
    } finally {
      setDeleting(false)
    }
  }

  const counts = portfolioSummary(projects)
  const visible = filteredPortfolio(projects, filter)

  return (
    <div className="portfolio-page">
      <p className="portfolio-eyebrow">Designer workspace</p>
      <h1>My portfolio</h1>
      <p className="portfolio-intro">View your current and completed interior design projects.</p>
      {location.state?.createdProjectName && <Alert type="success" showIcon role="status"
        message={`Project “${location.state.createdProjectName}” added to your portfolio.`} className="form-feedback" />}
      {location.state?.editedProjectName && <Alert type="success" showIcon role="status"
        message={`Project “${location.state.editedProjectName}” updated.`} className="form-feedback" />}
      {deletedProjectName && <Alert type="success" showIcon role="status"
        message={`Project “${deletedProjectName}” deleted from your portfolio.`} className="form-feedback" />}
      {loading && <div className="loading-state"><Spin /> <span>Loading portfolio…</span></div>}
      {error && <Alert type="error" showIcon role="alert" message={error} className="form-feedback" />}
      {authorized && !loading && <>
        <div className="portfolio-summary" aria-label="Portfolio totals">
          <Card size="small"><strong>{counts.total}</strong><span>All projects</span></Card>
          <Card size="small"><strong>{counts.current}</strong><span>Current</span></Card>
          <Card size="small"><strong>{counts.completed}</strong><span>Completed</span></Card>
        </div>
        <Card className="portfolio-panel" title="Portfolio projects"
          extra={<Button type="primary" onClick={() => navigate('/designer/portfolio/new')}>Add project</Button>}>
          <div className="portfolio-filter-row">
            <span>Filter projects</span>
            <Segmented options={filters} value={filter} onChange={setFilter} aria-label="Filter projects by status" />
          </div>
          {visible.length === 0 ? (
            <Empty description={projects.length === 0
              ? 'No projects in your portfolio yet. Add a project to get started.'
              : `No ${portfolioStatusLabel(filter).toLowerCase()} projects found.`} />
          ) : (
            <div className="portfolio-grid">
              {visible.map((project) => (
                <Card key={project.id} className="portfolio-project-card" size="small">
                  {project.images?.length > 0 && <img className="portfolio-project-cover"
                    src={project.images[0].url} alt={`Cover photo for ${project.title}`} />}
                  <div className="portfolio-project-heading">
                    <h2>{project.title}</h2>
                    <Tag color={project.status === 'CURRENT' ? 'blue' : 'green'}>{portfolioStatusLabel(project.status)}</Tag>
                  </div>
                  <p className="portfolio-project-category">{project.categoryName}</p>
                  <p className="portfolio-project-description">{project.description}</p>
                  <div className="portfolio-project-actions">
                    <Button size="small" onClick={() => navigate(`/designer/portfolio/${project.id}/edit`)}>
                      Edit project
                    </Button>
                    <Button size="small" danger onClick={() => { setError(''); setDeletionTarget(project) }}>
                      Delete project
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </Card>
        <Modal title="Delete project?" open={deletionTarget !== null} okText="Delete project"
          okButtonProps={{ danger: true }} confirmLoading={deleting}
          okType="primary" onOk={confirmDeletion} onCancel={() => { if (!deleting) setDeletionTarget(null) }}
          cancelButtonProps={{ disabled: deleting }} closable={!deleting} maskClosable={!deleting}>
          <p>Delete “{deletionTarget?.title}” and all its photos? This cannot be undone.</p>
        </Modal>
        <Button onClick={() => navigate('/designer')}>Back to designer workspace</Button>
      </>}
    </div>
  )
}
