import { Alert, Button, Spin } from 'antd'
import { ArrowLeft, Heart, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getSavedProjects, removeFavourite } from '../api/favourites.js'
import { projectStatusLabel } from '../project/projectDetailsRules.js'
import { projectSearchDescription, projectSearchPath } from '../project/projectSearchRules.js'
import { savedProjectCount, withoutSavedProject } from '../project/savedProjectsRules.js'
import '../project/projectSearch.css'
import '../project/savedProjects.css'

export default function CustomerFavouritesPage() {
  const navigate = useNavigate()
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [feedback, setFeedback] = useState('')
  const [removingId, setRemovingId] = useState(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true)
      setError('')
      try {
        const saved = await getSavedProjects()
        if (active) setProjects(saved)
      } catch (failure) {
        if (!active) return
        if (failure.response?.status === 401) {
          navigate('/login', { replace: true, state: { returnTo: '/customer/favourites' } })
        } else if (failure.response?.status === 403) {
          setError('My Favourites is available only to signed-in customer accounts.')
        } else {
          setError('Unable to load your saved projects. Please try again.')
        }
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [navigate, revision])

  async function onRemove(projectId) {
    if (removingId !== null) return
    setRemovingId(projectId)
    setFeedback('')
    try {
      await removeFavourite(projectId)
      setProjects((current) => withoutSavedProject(current, projectId))
      setFeedback('Project removed from favourites.')
    } catch (failure) {
      if (failure.response?.status === 401) {
        navigate('/login', { replace: true, state: { returnTo: '/customer/favourites' } })
      } else {
        setFeedback('Unable to remove the project. Please try again.')
      }
    } finally {
      setRemovingId(null)
    }
  }

  return (
    <section className="saved-projects-page" aria-labelledby="saved-projects-title">
      <div className="saved-projects-heading">
        <Link className="saved-projects-back" to="/customer">
          <ArrowLeft size={16} aria-hidden="true" /> Customer area
        </Link>
        <p className="project-search-eyebrow">Your collection</p>
        <h1 id="saved-projects-title">My Favourites</h1>
        <p>Keep your favourite renovation ideas in one place. Revisit saved projects whenever you need inspiration.</p>
      </div>

      {feedback && <Alert className="saved-projects-feedback" type={feedback.startsWith('Unable') ? 'error' : 'success'}
        message={feedback} showIcon role="status" />}
      <section className="saved-projects-list" aria-labelledby="saved-projects-list-title">
        <div className="project-search-results-heading">
          <div>
            <h2 id="saved-projects-list-title">Saved projects</h2>
            <p>Projects you've added to your favourites.</p>
          </div>
          {!loading && !error && <span className="project-search-count" role="status">
            {savedProjectCount(projects.length)}
          </span>}
        </div>

        {loading && <div className="loading-state" role="status"><Spin /> <span>Loading your favourites…</span></div>}
        {!loading && error && (
          <div className="saved-projects-empty" role="alert">
            <h3>Unable to display favourites</h3>
            <p>{error}</p>
            <Button onClick={() => setRevision((value) => value + 1)}>Try again</Button>
            <p><Link to="/login">Log in with a customer account</Link></p>
          </div>
        )}
        {!loading && !error && projects.length === 0 && (
          <div className="saved-projects-empty">
            <Heart size={32} aria-hidden="true" />
            <h3>No saved projects yet</h3>
            <p>Explore interior design projects and save the ones that inspire you.</p>
            <Button type="primary" icon={<Search size={17} />} onClick={() => navigate('/projects')}>
              Explore projects
            </Button>
          </div>
        )}
        {!loading && !error && projects.length > 0 && (
          <div className="project-search-grid" aria-label="Saved interior design projects">
            {projects.map((project) => (
              <article className="project-search-card saved-project-card" key={project.id}>
                {project.coverImageUrl && <Link to={projectSearchPath(project.id)}
                  className="project-search-cover-link" aria-label={`View ${project.title}`}>
                  <img className="project-search-cover" src={project.coverImageUrl}
                    alt={`Cover photo for ${project.title}`} loading="lazy" />
                </Link>}
                <div className="project-search-tags">
                  <span className="project-search-category"><span className="ld-category-kicker">Style</span>{project.categoryName}</span>
                  <span className={project.status === 'COMPLETED'
                    ? 'project-search-status project-search-status-completed'
                    : 'project-search-status project-search-status-current'}>
                    {projectStatusLabel(project.status)}
                  </span>
                </div>
                <h3>{project.title}</h3>
                <p className="project-search-description">{projectSearchDescription(project.description)}</p>
                <div className="saved-project-card-designer">
                  <span className="project-search-designer-label">Designed by</span>
                  <span className="project-search-designer-name">{project.designer.name}</span>
                </div>
                <div className="saved-project-card-actions">
                  <Link to={projectSearchPath(project.id)} aria-label={`View ${project.title} project details`}>
                    View project <span aria-hidden="true">→</span>
                  </Link>
                  <Button type="text" danger size="small"
                    loading={removingId === project.id} disabled={removingId !== null}
                    onClick={() => onRemove(project.id)}
                    aria-label={`Remove ${project.title} from favourites`}>
                    <Heart size={15} fill="currentColor" aria-hidden="true" /> Remove
                  </Button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </section>
  )
}
