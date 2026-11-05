import { Alert, Button, Spin } from 'antd'
import { Heart, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getSavedProjects, removeFavourite } from '../api/favourites.js'
import SavedProjectCard from '../components/SavedProjectCard.jsx'
import CustomerReturnButton from '../components/CustomerReturnButton.jsx'
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
        <CustomerReturnButton />
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
            <Button type="primary" icon={<Search size={17} />} onClick={() => navigate('/explore')}>
              Explore projects
            </Button>
          </div>
        )}
        {!loading && !error && projects.length > 0 && (
          <div className="project-search-grid" aria-label="Saved interior design projects">
            {projects.map((project) => <SavedProjectCard key={project.id} project={project}
              onRemove={onRemove} removing={removingId === project.id}
              removeDisabled={removingId !== null} />)}
          </div>
        )}
      </section>
    </section>
  )
}
