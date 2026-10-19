import { Alert, Button, Card, Empty, Segmented, Spin, Tag } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyPortfolio } from '../api/portfolio.js'
import { currentDesigner, currentDesignerArea } from '../api/designerAuthentication.js'
import { filteredPortfolio, portfolioStatusLabel, portfolioSummary } from '../designer/portfolioRules.js'

const filters = [
  { label: 'All', value: 'ALL' },
  { label: 'Current', value: 'CURRENT' },
  { label: 'Completed', value: 'COMPLETED' },
]

export default function DesignerPortfolioPage() {
  const navigate = useNavigate()
  const [authorized, setAuthorized] = useState(false)
  const [loading, setLoading] = useState(true)
  const [projects, setProjects] = useState([])
  const [filter, setFilter] = useState('ALL')
  const [error, setError] = useState('')

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

  const counts = portfolioSummary(projects)
  const visible = filteredPortfolio(projects, filter)

  return (
    <div className="portfolio-page">
      <p className="portfolio-eyebrow">Designer workspace</p>
      <h1>My portfolio</h1>
      <p className="portfolio-intro">View your current and completed interior design projects.</p>
      {loading && <div className="loading-state"><Spin /> <span>Loading portfolio…</span></div>}
      {error && <Alert type="error" showIcon role="alert" message={error} className="form-feedback" />}
      {authorized && !loading && <>
        <div className="portfolio-summary" aria-label="Portfolio totals">
          <Card size="small"><strong>{counts.total}</strong><span>All projects</span></Card>
          <Card size="small"><strong>{counts.current}</strong><span>Current</span></Card>
          <Card size="small"><strong>{counts.completed}</strong><span>Completed</span></Card>
        </div>
        <Card className="portfolio-panel" title="Portfolio projects">
          <div className="portfolio-filter-row">
            <span>Filter projects</span>
            <Segmented options={filters} value={filter} onChange={setFilter} aria-label="Filter projects by status" />
          </div>
          {visible.length === 0 ? (
            <Empty description={projects.length === 0
              ? 'No projects in your portfolio yet. You can add projects when project creation is available.'
              : `No ${portfolioStatusLabel(filter).toLowerCase()} projects found.`} />
          ) : (
            <div className="portfolio-grid">
              {visible.map((project) => (
                <Card key={project.id} className="portfolio-project-card" size="small">
                  <div className="portfolio-project-heading">
                    <h2>{project.title}</h2>
                    <Tag color={project.status === 'CURRENT' ? 'blue' : 'green'}>{portfolioStatusLabel(project.status)}</Tag>
                  </div>
                  <p className="portfolio-project-category">{project.categoryName}</p>
                  <p className="portfolio-project-description">{project.description}</p>
                </Card>
              ))}
            </div>
          )}
        </Card>
        <Button onClick={() => navigate('/designer')}>Back to designer workspace</Button>
      </>}
    </div>
  )
}
