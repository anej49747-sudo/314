import { Alert, Button, Input, Select, Spin } from 'antd'
import { Info, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listCategories } from '../api/categories.js'
import { getPublicProjects } from '../api/publicProjects.js'
import { projectStatusLabel } from '../project/projectDetailsRules.js'
import {
  normalizeProjectQuery,
  projectSearchCount,
  projectSearchDescription,
  projectSearchHeading,
  projectSearchPath,
} from '../project/projectSearchRules.js'
import '../project/projectSearch.css'

export default function ProjectSearchPage() {
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')
  const [categoryId, setCategoryId] = useState(null)
  const [categories, setCategories] = useState([])
  const [categoriesLoading, setCategoriesLoading] = useState(true)
  const [categoriesError, setCategoriesError] = useState('')
  const [categoryRevision, setCategoryRevision] = useState(0)
  const [revision, setRevision] = useState(0)
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    async function loadCategories() {
      setCategoriesLoading(true)
      setCategoriesError('')
      try {
        const entries = await listCategories()
        if (active) setCategories(entries)
      } catch {
        if (active) setCategoriesError('Unable to load project categories. Please try again.')
      } finally {
        if (active) setCategoriesLoading(false)
      }
    }
    loadCategories()
    return () => { active = false }
  }, [categoryRevision])

  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true)
      setError('')
      try {
        const results = await getPublicProjects(query, categoryId)
        if (active) setProjects(results)
      } catch {
        if (active) setError('Unable to load projects. Please try again later.')
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [query, categoryId, revision])

  const categoryName = categories.find((category) => category.id === categoryId)?.name || ''
  const activeFilters = Boolean(query || categoryId != null)

  function submitSearch(event) {
    event.preventDefault()
    setQuery(normalizeProjectQuery(input))
    setRevision((value) => value + 1)
  }

  function clearTitle() {
    setInput('')
    setQuery('')
    setRevision((value) => value + 1)
  }

  function clearCategory() {
    setCategoryId(null)
  }

  function clearAll() {
    setInput('')
    setQuery('')
    setCategoryId(null)
    setRevision((value) => value + 1)
  }

  return (
    <section className="project-search-page" aria-labelledby="project-search-title">
      <div className="project-search-heading">
        <p className="project-search-eyebrow">Explore &amp; discover</p>
        <h1 id="project-search-title">Explore interior design projects</h1>
        <p>Find inspiration from completed and ongoing renovation projects. Search by project title and filter by category to discover designs that interest you.</p>
      </div>

      <div className="project-search-panel">
        <div className="project-search-filters-layout">
          <form className="project-search-keyword-control" role="search" onSubmit={submitSearch}>
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
          </form>
          <div className="project-search-category-control">
            <label className="project-search-label" htmlFor="project-category-select">Project category</label>
            <Select
              id="project-category-select"
              aria-label="Project category"
              aria-describedby="project-category-note"
              className="project-search-category-select"
              value={categoryId ?? 'all'}
              onChange={(value) => setCategoryId(value === 'all' ? null : value)}
              loading={categoriesLoading}
              disabled={categoriesLoading || Boolean(categoriesError)}
              options={[
                { value: 'all', label: 'All categories' },
                ...categories.map((category) => ({ value: category.id, label: category.name })),
              ]}
            />
            <p className="project-search-category-note" id="project-category-note">Updates results immediately</p>
          </div>
        </div>
        <p className="project-search-help"><Info size={16} aria-hidden="true" /> Title search applies on Search or Enter. Category changes filter the last submitted search immediately.</p>
        {categoriesError && (
          <div className="project-search-category-error">
            <Alert type="error" role="alert" showIcon message={categoriesError} />
            <Button size="small" onClick={() => setCategoryRevision((value) => value + 1)}>Retry categories</Button>
          </div>
        )}
      </div>

      <section className="project-search-results" aria-labelledby="project-search-results-title">
        <div className="project-search-results-heading">
          <div>
            <h2 id="project-search-results-title">{projectSearchHeading(query, categoryName)}</h2>
            <p>{query && categoryId != null
              ? 'Projects matching your title search and selected category.'
              : query ? 'Projects matching your title search.'
                : categoryId != null ? 'Projects in the selected category.'
                  : 'Explore publicly available projects from interior designers.'}</p>
          </div>
          {!loading && !error && <span className="project-search-count" role="status">{projectSearchCount(projects.length)}</span>}
        </div>

        {activeFilters && (
          <div className="project-search-active" aria-label="Active search filters">
            {query && <button type="button" className="project-search-chip" onClick={clearTitle}
              aria-label="Clear title search">Title: “{query}” <span aria-hidden="true">×</span></button>}
            {categoryId != null && <button type="button" className="project-search-chip" onClick={clearCategory}
              aria-label="Clear category filter">Category: {categoryName || 'Selected category'} <span aria-hidden="true">×</span></button>}
            <button type="button" className="project-search-clear" onClick={clearAll}>Clear all filters</button>
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
            <h3>{activeFilters ? 'No projects found' : 'No projects available yet'}</h3>
            <p>{activeFilters
              ? 'Try another project title or category, or clear your filters to see all projects.'
              : 'Check back later for newly published projects.'}</p>
            {activeFilters && <Button onClick={clearAll}>Clear all filters</Button>}
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
