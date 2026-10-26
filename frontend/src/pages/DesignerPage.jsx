import { Alert, Button, Spin } from 'antd'
import { ArrowRight, Building2, Eye, FolderOpen, Heart, Plus, ShieldCheck } from 'lucide-react'
import { ActionCard, ImagePreviewCard, SectionHeading } from '../components/VisualCards.jsx'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { currentDesigner, currentDesignerArea } from '../api/designerAuthentication.js'
import { getMyPortfolio } from '../api/portfolio.js'

export default function DesignerPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [account, setAccount] = useState(null)
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [projectsError, setProjectsError] = useState('')

  useEffect(() => {
    let active = true
    async function initialize() {
      try {
        const profile = await currentDesigner()
        if (!active) return
        if (profile.passwordChangeRequired) {
          navigate('/designer/change-password', { replace: true }); return
        }
        const allowed = await currentDesignerArea()
        if (!active) return
        setAccount(allowed)
        try {
          const portfolio = await getMyPortfolio()
          if (active) setProjects(portfolio)
        } catch {
          if (active) setProjectsError('Unable to load your recent projects. You can still manage your portfolio.')
        }
      } catch (failure) {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to load your designer workspace. Please try again.')
      } finally { if (active) setLoading(false) }
    }
    initialize()
    return () => { active = false }
  }, [navigate])

  const totalViews = projects.reduce((sum, project) => sum + (Number(project.viewCount) || 0), 0)
  const totalFavourites = projects.reduce((sum, project) => sum + (Number(project.favouriteCount) || 0), 0)
  return <section className="ui-dashboard" aria-labelledby="designer-dashboard-title">
    <header className="ui-dashboard-header">
      <div><p className="ui-eyebrow">Designer workspace</p>
        <h1 id="designer-dashboard-title">{account ? `Welcome back, ${account.displayName}` : 'Designer workspace'}</h1>
        <p>Manage your studio and showcase your renovation work.</p></div>
      <Button type="primary" icon={<Plus size={17} />} onClick={() => navigate('/designer/portfolio/new')} disabled={!account}>New project</Button>
    </header>
    {location.state?.passwordChanged && <Alert type="success" role="status" showIcon message="Password changed successfully." />}
    {loading && <div className="loading-state" role="status"><Spin /> Loading your workspace…</div>}
    {error && <Alert type="error" role="alert" showIcon message={error} />}
    {account && <>
      <div className="ui-metrics" aria-label="Portfolio summary">
        <div className="ui-metric"><FolderOpen size={20} aria-hidden="true"/><strong>{projectsError ? '—' : projects.length}</strong><span>Portfolio projects</span></div>
        <div className="ui-metric"><Eye size={20} aria-hidden="true"/><strong>{projectsError ? '—' : totalViews.toLocaleString()}</strong><span>Project views</span></div>
        <div className="ui-metric"><Heart size={20} aria-hidden="true"/><strong>{projectsError ? '—' : totalFavourites.toLocaleString()}</strong><span>Current favourites</span></div>
      </div>
      <section className="ui-dashboard-section" aria-labelledby="designer-shortcuts-heading">
        <h2 id="designer-shortcuts-heading">Manage your workspace</h2>
        <div className="ui-shortcuts">
          {[{ to: '/designer/portfolio', label: 'My portfolio', detail: 'View and edit your projects', Icon: FolderOpen },
            { to: '/designer/company-information', label: 'Company information', detail: 'Update your studio profile', Icon: Building2 },
            { to: '/designer/security', label: 'Account security', detail: 'Change email or password', Icon: ShieldCheck }]
            .map(({ to, label, detail, Icon }) => <ActionCard key={to} to={to} title={label} description={detail} Icon={Icon} />)}
        </div>
      </section>
      <section className="ui-dashboard-section" aria-labelledby="designer-recent-heading">
        <SectionHeading id="designer-recent-heading" title="Recent projects"
          description="Your latest portfolio work at a glance."
          action={<Link className="ui-text-action" to="/designer/portfolio">Manage portfolio <ArrowRight size={16}/></Link>} />
        {projectsError && <Alert type="warning" showIcon message={projectsError} />}
        {!projectsError && projects.length === 0 && <div className="ui-empty-state"><FolderOpen size={28}/><h3>No projects yet</h3>
          <p>Add your first renovation project to start your portfolio.</p></div>}
        {!projectsError && projects.length > 0 && <div className="ui-image-grid">
          {projects.slice(0, 3).map(project => <ImagePreviewCard key={project.id}
            to={`/designer/portfolio/${project.id}/edit`} imageUrl={project.images?.[0]?.url}
            title={project.title} subtitle={`${project.categoryName} · ${project.status === 'COMPLETED' ? 'Completed' : 'Current'}`}
            fallbackIcon={FolderOpen} />)}
        </div>}
      </section>
    </>}
  </section>
}
