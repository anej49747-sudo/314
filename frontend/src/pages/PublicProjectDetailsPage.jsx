import { Alert, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getPublicProject } from '../api/publicProjects.js'
import { designerProfilePath } from '../designer/designerDirectoryRules.js'
import {
  isValidProjectId,
  projectBackLink,
  projectDescription,
  projectStatusLabel,
} from '../project/projectDetailsRules.js'
import '../project/projectDetails.css'

export default function PublicProjectDetailsPage() {
  const { projectId } = useParams()
  const { state } = useLocation()
  const back = projectBackLink(state)
  const [project, setProject] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    async function load() {
      setProject(null)
      setLoading(true)
      setNotFound(false)
      setError('')
      if (!isValidProjectId(projectId)) {
        setNotFound(true)
        setLoading(false)
        return
      }
      try {
        const data = await getPublicProject(projectId)
        if (active) setProject(data)
      } catch (failure) {
        if (!active) return
        if (failure.response?.status === 404) setNotFound(true)
        else setError('Unable to load this project. Please try again later.')
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [projectId])

  return (
    <section className="public-project-details" aria-labelledby="public-project-title">
      <Link className="designer-profile-back" to={back.to}>← {back.label}</Link>
      {loading && (
        <div className="loading-state" role="status">
          <Spin /> <span>Loading project details…</span>
        </div>
      )}
      {!loading && notFound && (
        <div className="public-profile-message">
          <h1 id="public-project-title">Project not found</h1>
          <p>This project is not available.</p>
        </div>
      )}
      {!loading && error && (
        <div className="public-profile-message">
          <h1 id="public-project-title">Project details</h1>
          <Alert showIcon role="alert" type="error" message={error} />
        </div>
      )}
      {!loading && project && (
        <>
          <p className="public-profile-eyebrow">Interior design project</p>
          <h1 id="public-project-title">{project.title}</h1>
          <p className="public-project-lede">Explore this project's design approach and the work involved.</p>
          <div className="public-project-tags" aria-label="Project category and status">
            <span className={project.status === 'COMPLETED'
              ? 'public-profile-status public-profile-status-completed'
              : 'public-profile-status'}>
              {projectStatusLabel(project.status)}
            </span>
            <span className="public-project-category">{project.categoryName}</span>
          </div>
          <div className="public-project-grid">
            <section className="public-project-panel" aria-labelledby="public-project-about-title">
              <h2 id="public-project-about-title">About this project</h2>
              <p className="public-project-description">{projectDescription(project.description)}</p>
            </section>
            <aside className="public-project-panel" aria-labelledby="public-project-designer-title">
              <h2 id="public-project-designer-title">Designed by</h2>
              <p className="public-project-designer-name">{project.designer.name}</p>
              {project.designer.address?.trim() && (
                <p className="public-project-address">{project.designer.address.trim()}</p>
              )}
              <Link className="public-project-designer-link"
                to={designerProfilePath(project.designer.id)}>
                View designer profile <span aria-hidden="true">→</span>
              </Link>
            </aside>
          </div>
        </>
      )}
    </section>
  )
}
