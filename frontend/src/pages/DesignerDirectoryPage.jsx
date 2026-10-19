import { Alert, Empty, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getDesignerDirectory } from '../api/designerDirectory.js'
import { directoryAddress, directoryCountLabel, directoryDescription, designerProfilePath } from '../designer/designerDirectoryRules.js'

export default function DesignerDirectoryPage() {
  const [designers, setDesigners] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    async function load() {
      try {
        const entries = await getDesignerDirectory()
        if (active) setDesigners(entries)
      } catch {
        if (active) setError('Unable to load interior designers. Please try again later.')
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [])

  return (
    <section className="designer-directory-page" aria-labelledby="designer-directory-title">
      <div className="designer-directory-heading">
        <p className="designer-directory-eyebrow">Explore professionals</p>
        <h1 id="designer-directory-title">Interior designers</h1>
        <p className="designer-directory-intro">
          Browse interior designers and design studios. Select a listing to see its profile and portfolio.
        </p>
      </div>
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
      {!loading && !error && designers.length > 0 && (
        <div className="designer-directory-grid">
          {designers.map((designer) => (
            <article className="designer-directory-card" key={designer.id}>
              <h3>{designer.name}</h3>
              {directoryAddress(designer.address) && (
                <p className="designer-directory-address">{directoryAddress(designer.address)}</p>
              )}
              <p className="designer-directory-description">{directoryDescription(designer.description)}</p>
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
