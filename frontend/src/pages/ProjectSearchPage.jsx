import { Alert, Button, Input, Select, Spin } from 'antd'
import { Heart, Info, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listCategories } from '../api/categories.js'
import { getCatalogue } from '../api/discovery.js'
import '../ui/discovery.css'
import { currentSession } from '../api/authentication.js'
import { getSavedProjects } from '../api/favourites.js'
import { getPublicProjects } from '../api/publicProjects.js'
import { usePublicRecognitions } from '../recognition/usePublicRecognitions.js'
import { FeaturedProjectBadge } from '../components/RecognitionBadges.jsx'
import ProjectAttributes from '../components/ProjectAttributes.jsx'
import { activeProjectFilters, sizeRangeLabel } from '../discovery/presentationRules.js'
import { projectStatusLabel } from '../project/projectDetailsRules.js'
import {
  normalizeProjectQuery,
  projectSearchCount,
  projectSearchDescription,
  projectSearchHeading,
  projectSearchPath,
  savedProjectIdSet,
} from '../project/projectSearchRules.js'
import '../project/projectSearch.css'

export default function ProjectSearchPage() {
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')
  const [categoryId, setCategoryId] = useState(null)
  const [filterOpen, setFilterOpen] = useState(false)
  const [styleIds, setStyleIds] = useState([])
  const [propertyTypeId, setPropertyTypeId] = useState(null)
  const [sizeRangeId, setSizeRangeId] = useState(null)
  const [extra, setExtra] = useState({ styles: [], types: [], sizes: [] })
  const [categories, setCategories] = useState([])
  const [categoriesLoading, setCategoriesLoading] = useState(true)
  const [categoriesError, setCategoriesError] = useState('')
  const [categoryRevision, setCategoryRevision] = useState(0)
  const [revision, setRevision] = useState(0)
  const [projects, setProjects] = useState([])
  const { featuredProject } = usePublicRecognitions()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [savedIds, setSavedIds] = useState(new Set())
  const [savedIndicatorError, setSavedIndicatorError] = useState(false)

  useEffect(() => {
    let alive = true
    Promise.all([getCatalogue('styles'), getCatalogue('property-types'), getCatalogue('size-ranges')])
      .then(([styles, types, sizes]) => { if (alive) setExtra({ styles, types, sizes }) }).catch(() => {})
    return () => { alive = false }
  }, [])
  // The public search remains available to guests and non-customer roles.
  useEffect(() => {
    let active = true
    async function loadSavedIndicators() {
      try {
        const account = await currentSession()
        if (!active || account?.role !== 'CUSTOMER') return
        try {
          const savedProjects = await getSavedProjects()
          if (active) setSavedIds(savedProjectIdSet(savedProjects))
        } catch {
          if (active) setSavedIndicatorError(true)
        }
      } catch {
        // Anonymous visitors have no favourites to display.
      }
    }
    loadSavedIndicators()
    return () => { active = false }
  }, [])

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
        const results = await getPublicProjects(query, categoryId, { styleIds, propertyTypeId, sizeRangeId })
        if (active) setProjects(results)
      } catch {
        if (active) setError('Unable to load projects. Please try again later.')
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [query, categoryId, styleIds, propertyTypeId, sizeRangeId, revision])

  const categoryName = categories.find((category) => category.id === categoryId)?.name || ''
  const chips = activeProjectFilters({ query, categoryId, categoryName, styleIds, styles: extra.styles,
    propertyTypeId, types: extra.types, sizeRangeId, sizes: extra.sizes })
  const activeFilters = chips.length > 0
  function removeFilter(filter) {
    if (filter.key === 'title') clearTitle()
    else if (filter.key === 'category') clearCategory()
    else if (filter.key.startsWith('style-')) setStyleIds(current => current.filter(id => String(id) !== String(filter.id)))
    else if (filter.key === 'property') setPropertyTypeId(null)
    else if (filter.key === 'size') setSizeRangeId(null)
  }

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
    setStyleIds([])
    setPropertyTypeId(null)
    setSizeRangeId(null)
    setRevision((value) => value + 1)
  }

  return (
    <section className="project-search-page" aria-labelledby="project-search-title">
      <div className="project-search-heading">
        <p className="project-search-eyebrow">Project catalogue</p>
        <h1 id="project-search-title">Find interior design projects</h1>
        <p>Search renovation projects by title, category, design style, property type and floor area. For a photo-first browsing experience, visit <Link to="/explore">Explore</Link>.</p>
      </div>

      <div className="project-search-panel">
        <form role="search" onSubmit={submitSearch} className="project-search-keyword-control">
          <label className="project-search-label" htmlFor="project-search-input">Search projects by title</label>
          <div className="project-search-row"><Input id="project-search-input" type="search" size="large" maxLength={160} prefix={<Search size={18}/>} placeholder="Search projects..." value={input} onChange={event => setInput(event.target.value)}/>
            <Button type="primary" htmlType="submit" size="large" icon={<Search size={18}/>}>Search</Button></div>
        </form>
        <Button className="discovery-filter-toggle" aria-expanded={filterOpen} onClick={() => setFilterOpen(!filterOpen)}>{filterOpen ? 'Hide filters' : 'Filters'} {Number(categoryId!=null)+Number(propertyTypeId!=null)+Number(sizeRangeId!=null)+styleIds.length || ''}</Button>
        {filterOpen && <div className="discovery-filter-panel">
          <div><label htmlFor="project-category-filter">Renovation category</label><Select id="project-category-filter" value={categoryId} allowClear placeholder="All categories" loading={categoriesLoading} options={categories.map(c=>({value:c.id,label:c.name}))} onChange={setCategoryId} /></div>
          <div><label htmlFor="project-property-filter">Property type</label><Select id="project-property-filter" value={propertyTypeId} allowClear placeholder="All property types" disabled={!extra.types.length} options={extra.types.map(c=>({value:c.id,label:c.name}))} onChange={setPropertyTypeId}/></div>
          <div><label htmlFor="project-area-filter">Floor area</label><Select id="project-area-filter" value={sizeRangeId} allowClear placeholder="Any size" disabled={!extra.sizes.length} options={extra.sizes.map(c=>({value:c.id,label:sizeRangeLabel(c)}))} onChange={setSizeRangeId}/></div>
          <div><label htmlFor="project-style-filter">Design styles</label><Select id="project-style-filter" mode="multiple" value={styleIds} placeholder="All styles" disabled={!extra.styles.length} options={extra.styles.map(c=>({value:c.id,label:c.name}))} onChange={setStyleIds}/></div>
          <div style={{alignSelf:'end'}}><Button onClick={clearAll}>Clear all filters</Button></div>
        </div>}
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
            <p>{activeFilters
              ? 'Projects matching your selected search and classification filters.'
              : 'Browse the full public project catalogue from interior designers.'}</p>
          </div>
          {!loading && !error && <span className="project-search-count" role="status">{projectSearchCount(projects.length)}</span>}
        </div>

        {savedIndicatorError && (
          <Alert type="warning" showIcon role="status" className="project-search-saved-warning"
            message="Saved-project indicators are unavailable. You can still browse projects." />
        )}

        {activeFilters && (
          <div className="project-search-active" aria-label="Active search filters">
            {chips.map(chip => <button key={chip.key} type="button" className="project-search-chip"
              onClick={() => removeFilter(chip)} aria-label={`Remove ${chip.label} filter`}>
              {chip.label} <span aria-hidden="true">×</span>
            </button>)}
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
              ? 'Adjust your search or classification filters, or clear them to see all projects.'
              : 'Check back later for newly published projects.'}</p>
            {activeFilters && <Button onClick={clearAll}>Clear all filters</Button>}
          </div>
        )}
        {!loading && !error && projects.length > 0 && (
          <div className="project-search-grid" aria-label="Project search results">
            {projects.map((project) => (
              <article className="project-search-card" key={project.id}>
                {project.coverImageUrl && <Link to={projectSearchPath(project.id)}
                  className="project-search-cover-link" aria-label={`View ${project.title}`}>
                  <img className="project-search-cover" src={project.coverImageUrl}
                    alt={`Cover photo for ${project.title}`} loading="lazy" />
                </Link>}
                <div className="project-search-tags">
                  <span className={project.status === 'COMPLETED'
                    ? 'project-search-status project-search-status-completed'
                    : 'project-search-status project-search-status-current'}>
                    {projectStatusLabel(project.status)}
                  </span>
                  {savedIds.has(String(project.id)) && (
                    <span className="project-search-saved-indicator" role="img"
                      aria-label="Saved to favourites" title="Saved to favourites">
                      <Heart size={18} fill="currentColor" aria-hidden="true" />
                    </span>
                  )}
                </div>
                <FeaturedProjectBadge featuredProject={featuredProject} projectId={project.id} />
                <h3>{project.title}</h3>
                <ProjectAttributes project={project} />
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
