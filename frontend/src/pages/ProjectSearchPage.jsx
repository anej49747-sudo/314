import { Alert, Button, Input, Spin } from 'antd'
import { Info, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getPublicProjects } from '../api/publicProjects.js'
import { projectStatusLabel } from '../project/projectDetailsRules.js'
import {
  normalizeProjectQuery,
  projectSearchCount,
  projectSearchDescription,
  projectSearchPath,
} from '../project/projectSearchRules.js'
import '../project/projectSearch.css'

export default function ProjectSearchPage() {
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')
  const [revision, setRevision] = useState(0)
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true)
      setError('')
      try {
        const results = await getPublicProjects(query)
        if (active) setProjects(results)
      } catch {
        if (active) setError('Unable to load projects. Please try again later.')
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [query, revision])

  function submitSearch(event) {
    event.preventDefault()
    setQuery(normalizeProjectQuery(input))
    setRevision((value) => value + 1)
  }

  function clearSearch() {
    setInput('')
    setQuery('')
    setRevision((value) => value + 1)
  }

  return (
    <section className="project-search-page" aria-labelledby="project-search-title">
      <div className="project-search-heading">
        <p className="project-search-eyebrow">Explore &amp; discover</p>
        <h1 id="project-search-title">Explore interior design projects</h1>
        <p>Find inspiration from completed and ongoing renovation projects. Search by project title to discover designs that interest you.</p>
      </div>

      <form className="project-search-panel" role="search" onSubmit={submitSearch}>
        <label className="project-search-label" htmlFor="project-search-input">Search projects by title</label>
        <div className="project-search-row">
          <Input
            id="project-search-input"
            type="search"
            size="large"
            maxLength={160}
            prefix={<Search size={18} aria-hidden="true" />}
            placeholder="e.g. Modern kitchen, urban loft"
            value={input}
            onChange={(event) => setInput(event.target.value)}
          />
          <Button htmlType="submit" type="primary" size="large" icon={<Search size={18} />}>
            Search
          </Button>
        </div>
        <p className="project-search-help"><Info size={16} aria-hidden="true" /> Search is based on the project title only. Press Enter or select Search to see results.</p>
      </form>

      <section className="project-search-results" aria-labelledby="project-search-results-title">
        <div className="project-search-results-heading">
          <div>
            <h2 id="project-search-results-title">{query ? 'Search results' : 'All projects'}</h2>
            <p>{query ? 'Projects matching your title search.' : 'Explore publicly available projects from interior designers.'}</p>
          </div>
          {!loading && !error && <span className="project-search-count" role="status">{projectSearchCount(projects.length)}</span>}
        </div>

        {query && (
          <div className="project-search-active">
            <span className="project-search-chip">Search: “{query}”</span>
            <button type="button" className="project-search-clear" onClick={clearSearch}>Clear search</button>
          </div>
        )}

        {loading && <div className="loading-state" role="status"><Spin /> <span>Loading projects…</span></div>}
        {!loading && error && (
          <div className="project-search-error">
            <Alert type="error" role="alert" showIcon message={error} />
            <Button onClick={() => setRevision((value) => value + 1)}>Try again</Button>
          </div>
        )}
        {!loading && !error && projects.length === 0 && (
          <div className="project-search-empty">
            <Search size={28} aria-hidden="true" />
            <h3>{query ? 'No projects found' : 'No projects available yet'}</h3>
            <p>{query ? 'Try another project title or clear your search to see all projects.' : 'Check back later for newly published projects.'}</p>
            {query && <Button onClick={clearSearch}>View all projects</Button>}
          </div>
        )}
        {!loading && !error && projects.length > 0 && (
          <div className="project-search-grid" aria-label="Project search results">
            {projects.map((project) => (
              <article className="project-search-card" key={project.id}>
                <div className="project-search-tags">
                  <span className="project-search-category">{project.categoryName}</span>
                  <span className={project.status === 'COMPLETED'
                    ? 'project-search-status project-search-status-completed'
                    : 'project-search-status project-search-status-current'}>
                    {projectStatusLabel(project.status)}
                  </span>
                </div>
                <h3>{project.title}</h3>
                <p className="project-search-description">{projectSearchDescription(project.description)}</p>
                <div className="project-search-card-footer">
                  <div>
                    <span className="project-search-designer-label">Designed by</span>
                    <span className="project-search-designer-name">{project.designer.name}</span>
                  </div>
                  <Link to={projectSearchPath(project.id)} aria-label={`View ${project.title} project details`}>
                    View project <span aria-hidden="true">→</span>
                  </Link>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </section>
  )
}
