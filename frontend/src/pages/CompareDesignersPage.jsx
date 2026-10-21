import { Alert, Button, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import { getDesignerDirectory } from '../api/designerDirectory.js'
import { getDesignerProfile } from '../api/designerProfile.js'
import {
  comparisonText,
  MIN_COMPARE_DESIGNERS,
  normalizeComparisonIds,
  portfolioPreviewProjects,
  projectStatusCounts,
} from '../designer/compareDesignersRules.js'
import { designerProfilePath } from '../designer/designerDirectoryRules.js'
import { portfolioStatusLabel } from '../designer/portfolioRules.js'
import '../designer/compareDesigners.css'

function PortfolioPreview({ designer }) {
  const projects = Array.isArray(designer.projects) ? designer.projects : []
  const preview = portfolioPreviewProjects(projects)
  if (!preview.length) return <p className="compare-empty-portfolio">No portfolio projects have been added yet.</p>

  return <div className="compare-portfolio-preview">
    <span className="compare-portfolio-label">Portfolio examples</span>
    {preview.map((project) => (
      <div className="compare-project-tile" key={project.id}>
        <Link to={`/projects/${encodeURIComponent(project.id)}`} className="compare-project-title">
          {project.title}
        </Link>
        <span className={project.status === 'COMPLETED'
          ? 'compare-project-status compare-project-status-completed'
          : 'compare-project-status compare-project-status-current'}>
          {portfolioStatusLabel(project.status)}
        </span>
      </div>
    ))}
    <div className="compare-portfolio-footer">
      <span>{preview.length} of {projects.length} shown</span>
      <Link to={designerProfilePath(designer.id)}>View full portfolio →</Link>
    </div>
  </div>
}

export default function CompareDesignersPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [initialIds] = useState(() => normalizeComparisonIds(location.state?.designerIds))
  const [selectedIds, setSelectedIds] = useState(initialIds)
  const [profiles, setProfiles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true)
      setError('')
      setNotice('')
      try {
        // Check role before fetching comparison data; public profile APIs stay read-only/public.
        await currentCustomer()
      } catch (failure) {
        if (!active) return
        if (failure.response?.status === 401) {
          navigate('/login', { replace: true, state: { returnTo: '/designers' } })
        } else if (failure.response?.status === 403) {
          setError('Only signed-in Renovation Customers can compare interior designers.')
        } else {
          setError('Unable to verify your customer session. Please try again.')
        }
        setLoading(false)
        return
      }

      try {
        if (initialIds.length === 0) {
          if (active) setProfiles([])
          return
        }
        const directory = await getDesignerDirectory()
        const valid = normalizeComparisonIds(initialIds, directory.map((designer) => designer.id))
        const results = await Promise.all(valid.map(async (id) => {
          try {
            return await getDesignerProfile(id)
          } catch (failure) {
            if (failure.response?.status === 404) return null
            throw failure
          }
        }))
        if (active) {
          const available = results.filter(Boolean)
          setProfiles(available)
          if (available.length !== initialIds.length) {
            setNotice('One or more selected designers are no longer available. Choose active designers from the directory.')
          }
        }
      } catch {
        if (active) setError('Unable to load the comparison. Please try again.')
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [navigate, revision, initialIds])

  const visibleProfiles = selectedIds
    .map((id) => profiles.find((profile) => String(profile.id) === id))
    .filter(Boolean)

  function backToDirectory(ids = selectedIds) {
    navigate('/designers', { state: { designerIds: ids } })
  }

  function removeDesigner(id) {
    const next = selectedIds.filter((value) => value !== String(id))
    setSelectedIds(next)
    if (next.length < MIN_COMPARE_DESIGNERS) backToDirectory(next)
  }

  function compareRow(label, content) {
    return <tr key={label}>
      <th scope="row">{label}</th>
      {visibleProfiles.map((designer) => <td key={designer.id}>{content(designer)}</td>)}
    </tr>
  }

  return (
    <section className="compare-page" aria-labelledby="compare-page-title">
      <div className="compare-page-heading">
        <div>
          <p className="compare-eyebrow">Interior designer comparison</p>
          <h1 id="compare-page-title">Compare interior designers</h1>
          <p>Review available details side by side to help choose a designer for your renovation.</p>
        </div>
        <Button onClick={() => backToDirectory()}>← Add or change designers</Button>
      </div>
      {loading && <div className="loading-state" role="status"><Spin /> <span>Loading comparison…</span></div>}
      {!loading && error && <Alert type="error" role="alert" showIcon message={error}
        action={error.startsWith('Unable') ? <Button size="small" onClick={() => setRevision((value) => value + 1)}>Retry</Button> : null} />}
      {!loading && !error && notice && <Alert className="compare-notice" type="warning" role="status" showIcon message={notice} />}
      {!loading && !error && visibleProfiles.length < MIN_COMPARE_DESIGNERS && (
        <div className="compare-empty-state">
          <h2>Select at least two designers</h2>
          <p>Choose two or three available designers from the directory to see a side-by-side comparison.</p>
          <Button type="primary" onClick={() => backToDirectory(visibleProfiles.map((item) => String(item.id)))}>
            Browse interior designers
          </Button>
        </div>
      )}
      {!loading && !error && visibleProfiles.length >= MIN_COMPARE_DESIGNERS && <>
        <div className="compare-table-heading">
          <h2>{visibleProfiles.length} designers selected</h2>
          <Button type="text" onClick={() => backToDirectory([])}>Clear all</Button>
        </div>
        <div className="compare-table-scroll" role="region" aria-label="Interior designer comparison table" tabIndex={0}>
          <table className="compare-table" style={{ '--compare-width': `${190 + visibleProfiles.length * 330}px` }}>
            <thead><tr>
              <th scope="col">Comparison details</th>
              {visibleProfiles.map((designer) => <th scope="col" key={designer.id}>
                <span className="compare-table-name">{designer.name}</span>
                <span className="compare-table-address">{comparisonText(designer.address)}</span>
                <Button size="small" onClick={() => removeDesigner(designer.id)}
                  aria-label={`Remove ${designer.name} from comparison`}>
                  Remove from comparison ×
                </Button>
              </th>)}
            </tr></thead>
            <tbody>
              {compareRow('Display name', (designer) => comparisonText(designer.name))}
              {compareRow('Address', (designer) => comparisonText(designer.address))}
              {compareRow('About the designer', (designer) => <span className="compare-description">{comparisonText(designer.description)}</span>)}
              {compareRow('Portfolio project count', (designer) => projectStatusCounts(designer.projects).total)}
              {compareRow('Current projects', (designer) => projectStatusCounts(designer.projects).current)}
              {compareRow('Completed projects', (designer) => projectStatusCounts(designer.projects).completed)}
              {compareRow('Portfolio preview', (designer) => <PortfolioPreview designer={designer} />)}
              {compareRow('Profile', (designer) => <Link to={designerProfilePath(designer.id)}>View designer profile →</Link>)}
            </tbody>
          </table>
        </div>
        <p className="compare-mobile-hint">On smaller screens, scroll sideways to view each designer.</p>
      </>}
    </section>
  )
}
