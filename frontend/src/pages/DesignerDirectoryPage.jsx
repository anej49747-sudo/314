import { Alert, Button, Empty, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getDesignerDirectory } from '../api/designerDirectory.js'
import { currentSession } from '../api/authentication.js'
import { MAX_COMPARE_DESIGNERS, normalizeComparisonIds, toggleComparisonId } from '../designer/compareDesignersRules.js'
import '../designer/compareDesigners.css'
import { directoryAddress, directoryCountLabel, directoryDescription, designerProfilePath } from '../designer/designerDirectoryRules.js'

export default function DesignerDirectoryPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [designers, setDesigners] = useState([])
  const [customer, setCustomer] = useState(false)
  const [selectedIds, setSelectedIds] = useState(() => normalizeComparisonIds(location.state?.designerIds))
  const [selectionMessage, setSelectionMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    async function load() {
      try {
        const entries = await getDesignerDirectory()
        if (active) {
          setDesigners(entries)
          setSelectedIds((current) => normalizeComparisonIds(current, entries.map((item) => item.id)))
        }
      } catch {
        if (active) setError('Unable to load interior designers. Please try again later.')
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    currentSession().then((account) => {
      if (active) setCustomer(account.role === 'CUSTOMER')
    }).catch(() => {
      if (active) setCustomer(false)
    })
    return () => { active = false }
  }, [])

  function toggleDesigner(id) {
    const next = toggleComparisonId(selectedIds, id, designers.map((item) => item.id))
    setSelectedIds(next.ids)
    setSelectionMessage(next.limitReached
      ? 'You can compare up to three designers. Remove one before adding another.' : '')
  }

  return (
    <section className={`designer-directory-page${customer && selectedIds.length ? ' compare-directory-with-selection' : ''}`} aria-labelledby="designer-directory-title">
      <div className="designer-directory-heading">
        <p className="designer-directory-eyebrow">Explore professionals</p>
        <h1 id="designer-directory-title">Interior designers</h1>
        <p className="designer-directory-intro">
          Browse interior designers and design studios. Select a listing to see its profile and portfolio. Signed-in customers can compare two or three designers side by side.
        </p>
      </div>
      {customer && selectedIds.length > 0 && <div className="compare-selection-tray" role="region" aria-label="Selected designers for comparison" aria-live="polite">
        <div className="compare-selection-details">
          <strong>{selectedIds.length} designer{selectedIds.length === 1 ? '' : 's'} selected</strong>
          <div className="compare-selection-pills">
            {selectedIds.map((id) => {
              const designer = designers.find((item) => String(item.id) === id)
              return designer && <span className="compare-selection-pill" key={id}>{designer.name}</span>
            })}
          </div>
        </div>
        <div className="compare-selection-actions">
          <Button onClick={() => { setSelectedIds([]); setSelectionMessage('') }}>Clear all</Button>
          <Button type="primary" disabled={selectedIds.length < 2}
            onClick={() => navigate('/customer/compare', { state: { designerIds: selectedIds } })}>
            Compare designers →
          </Button>
        </div>
      </div>}
      {customer && selectionMessage && <Alert className="compare-selection-alert" type="warning" role="alert" showIcon message={selectionMessage} />}
      <div className="designer-directory-section-heading">
        <h2>All designers</h2>
        {!loading && !error && <span>{directoryCountLabel(designers.length)}</span>}
      </div>
      {loading && <div className="loading-state" role="status"><Spin /> <span>Loading interior designers…</span></div>}
      {error && <Alert type="error" role="alert" showIcon message={error} />}
      {!loading && !error && designers.length === 0 && (
        <div className="designer-directory-empty">
          <Empty description="No interior designers are available yet." />
        </div>
      )}
      {!loading && !error && designers.length > 0 && customer && (
        <p className="compare-directory-hint">{selectedIds.length} of {MAX_COMPARE_DESIGNERS} selected · Select at least two designers to compare.</p>
      )}
      {!loading && !error && designers.length > 0 && (
        <div className="designer-directory-grid">
          {designers.map((designer) => (
            <article className="designer-directory-card" key={designer.id}>
              <h3>{designer.name}</h3>
              {directoryAddress(designer.address) && (
                <p className="designer-directory-address">{directoryAddress(designer.address)}</p>
              )}
              <p className="designer-directory-description">{directoryDescription(designer.description)}</p>
              {customer && <label className="compare-designer-checkbox">
                <input type="checkbox" checked={selectedIds.includes(String(designer.id))}
                  onChange={() => toggleDesigner(designer.id)}
                  aria-label={`Compare ${designer.name}`} />
                Compare
              </label>}
              <div className="designer-directory-card-footer">
                <Link to={designerProfilePath(designer.id)} state={{ directoryName: designer.name }}>
                  View profile <span aria-hidden="true">→</span>
                </Link>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
