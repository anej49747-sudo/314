import { Alert, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getDesignerProfile } from '../api/designerProfile.js'
import DesignerReviewAction from '../components/DesignerReviewAction.jsx'
import { directoryAddress } from '../designer/designerDirectoryRules.js'
import {
  isValidDesignerId,
  profileIntroduction,
  profileProjectCountLabel,
  publicProjectPath,
} from '../designer/designerProfileRules.js'
import { portfolioStatusLabel } from '../designer/portfolioRules.js'

export default function DesignerProfilePage() {
  const { designerId } = useParams()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState('')
  const [reviewSuccess, setReviewSuccess] = useState('')

  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true)
      setProfile(null)
      setNotFound(false)
      setError('')
      setReviewSuccess('')
      if (!isValidDesignerId(designerId)) {
        setNotFound(true)
        setLoading(false)
        return
      }
      try {
        const entry = await getDesignerProfile(designerId)
        if (active) setProfile(entry)
      } catch (failure) {
        if (!active) return
        if (failure.response?.status === 404) setNotFound(true)
        else setError('Unable to load this designer profile. Please try again later.')
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [designerId])

  return (
    <section className="public-designer-profile" aria-labelledby="public-profile-title">
      <Link className="designer-profile-back" to="/designers">← Back to all interior designers</Link>
      {loading && <div className="loading-state" role="status"><Spin /> <span>Loading designer profile…</span></div>}
      {!loading && notFound && (
        <div className="public-profile-message">
          <h1 id="public-profile-title">Designer not found</h1>
          <p>This interior designer is not available.</p>
        </div>
      )}
      {!loading && error && (
        <div className="public-profile-message">
          <h1 id="public-profile-title">Designer profile</h1>
          <Alert type="error" showIcon role="alert" message={error} />
        </div>
      )}
      {!loading && profile && (
        <>
          <p className="public-profile-eyebrow">Interior designer profile</p>
          <div className="public-profile-review-head">
            <div>
              <h1 id="public-profile-title">{profile.name}</h1>
              <p className="public-profile-lede">Learn about this designer and explore the projects in their portfolio.</p>
            </div>
            <DesignerReviewAction designerId={profile.id} designerName={profile.name}
              onSubmitted={setReviewSuccess} />
          </div>
          {reviewSuccess && <Alert className="designer-review-success" role="status" type="success"
            showIcon message="Review submitted" description={reviewSuccess} />}
          <div className="public-profile-stack">
            <section className="public-profile-panel" aria-labelledby="profile-company-title">
              <h2 id="profile-company-title">Company info</h2>
              <p className="public-profile-info-label">Display name</p>
              <p className="public-profile-info-value">{profile.name}</p>
              {directoryAddress(profile.address) && (
                <div className="public-profile-info-address">
                  <p className="public-profile-info-label">Address</p>
                  <p className="public-profile-info-value">{directoryAddress(profile.address)}</p>
                </div>
              )}
            </section>
            <section className="public-profile-panel" aria-labelledby="profile-about-title">
              <h2 id="profile-about-title">About the designer</h2>
              <p className="public-profile-description">{profileIntroduction(profile.description)}</p>
            </section>
            <section className="public-profile-panel" aria-labelledby="profile-projects-title">
              <div className="public-profile-section-heading">
                <h2 id="profile-projects-title">Portfolio projects</h2>
                <span>{profileProjectCountLabel(profile.projects.length)}</span>
              </div>
              {profile.projects.length === 0 ? (
                <p className="public-profile-empty">No portfolio projects have been added yet.</p>
              ) : (
                <div className="public-profile-project-grid">
                  {profile.projects.map((project) => (
                    <article className="public-profile-project" key={project.id}>
                      <span className={project.status === 'COMPLETED'
                        ? 'public-profile-status public-profile-status-completed'
                        : 'public-profile-status'}>
                        {portfolioStatusLabel(project.status)}
                      </span>
                      <h3>{project.title}</h3>
                      <p className="public-profile-project-category">{project.categoryName}</p>
                      <Link className="public-profile-project-link"
                        to={publicProjectPath(project.id)}
                        state={{ designerId: profile.id, projectTitle: project.title }}>
                        View project details <span aria-hidden="true">→</span>
                      </Link>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </div>
        </>
      )}
    </section>
  )
}
