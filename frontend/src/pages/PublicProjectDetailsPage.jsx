import { Alert, Button, Modal, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { getPublicProject } from '../api/publicProjects.js'
import { getFavouriteStatus, removeFavourite, saveFavourite } from '../api/favourites.js'
import { designerProfilePath } from '../designer/designerDirectoryRules.js'
import {
  isValidProjectId,
  projectBackLink,
  projectDescription,
  projectStatusLabel,
} from '../project/projectDetailsRules.js'
import { photoAlt, projectPhotos, shiftedPhotoIndex } from '../project/photoGalleryRules.js'
import { favouriteAccessState, favouriteButtonLabel, favouriteResultMessage } from '../project/favouriteRules.js'
import '../project/projectDetails.css'
import '../project/favouriteControls.css'

export default function PublicProjectDetailsPage() {
  const { projectId } = useParams()
  const { state } = useLocation()
  const navigate = useNavigate()
  const back = projectBackLink(state)
  const [project, setProject] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState('')
  const [openPhotoIndex, setOpenPhotoIndex] = useState(null)
  const [favouriteAccess, setFavouriteAccess] = useState('loading')
  const [isFavourite, setIsFavourite] = useState(false)
  const [favouriteBusy, setFavouriteBusy] = useState(false)
  const [favouriteFeedback, setFavouriteFeedback] = useState(null)
  const [favouriteRefreshKey, setFavouriteRefreshKey] = useState(0)
  const [loginPromptOpen, setLoginPromptOpen] = useState(false)

  useEffect(() => {
    let active = true
    async function load() {
      setProject(null)
      setOpenPhotoIndex(null)
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

  useEffect(() => {
    if (!project || String(project.id) !== projectId) return undefined
    let active = true
    setFavouriteAccess('loading')
    setIsFavourite(false)
    setFavouriteFeedback(null)
    getFavouriteStatus(projectId).then((result) => {
      if (!active) return
      setIsFavourite(result.saved === true)
      setFavouriteAccess('customer')
    }).catch((failure) => {
      if (active) setFavouriteAccess(favouriteAccessState(failure.response?.status))
    })
    return () => { active = false }
  }, [project, projectId, favouriteRefreshKey])

  async function onToggleFavourite() {
    if (favouriteAccess === 'visitor') {
      setLoginPromptOpen(true)
      return
    }
    if (favouriteAccess !== 'customer' || favouriteBusy) return
    setFavouriteBusy(true)
    setFavouriteFeedback(null)
    try {
      if (isFavourite) await removeFavourite(projectId)
      else await saveFavourite(projectId)
      const nextSaved = !isFavourite
      setIsFavourite(nextSaved)
      setFavouriteFeedback({ type: 'success', message: favouriteResultMessage(nextSaved) })
    } catch (failure) {
      if (failure.response?.status === 401 || failure.response?.status === 403) {
        setFavouriteAccess('visitor')
        setFavouriteFeedback({ type: 'error', message: 'Please log in with a customer account to save projects.' })
      } else {
        setFavouriteFeedback({ type: 'error', message: 'Unable to update favourites. Please try again.' })
      }
    } finally {
      setFavouriteBusy(false)
    }
  }

  const photos = projectPhotos(project)
  const selectedPhoto = openPhotoIndex === null ? null : (photos[openPhotoIndex] ?? null)

  useEffect(() => {
    if (openPhotoIndex === null) return undefined
    function onKeyDown(event) {
      if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
        event.preventDefault()
        const delta = event.key === 'ArrowLeft' ? -1 : 1
        setOpenPhotoIndex((index) => shiftedPhotoIndex(index, delta, photos.length))
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [openPhotoIndex, photos.length])

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
          <div className="public-project-favourite-area">
            <Button type="default"
              className={`public-project-favourite-button${isFavourite ? ' saved' : ''}`}
              loading={favouriteBusy} disabled={favouriteBusy || favouriteAccess === 'loading' || favouriteAccess === 'unavailable'}
              onClick={onToggleFavourite} aria-pressed={favouriteAccess === 'customer' ? isFavourite : undefined}>
              <span className="favourite-heart" aria-hidden="true">{isFavourite ? '♥' : '♡'}</span>
              {favouriteButtonLabel(isFavourite)}
            </Button>
            {favouriteAccess === 'loading' && <span className="public-project-favourite-hint">Checking saved status…</span>}
            {favouriteAccess === 'visitor' && <span className="public-project-favourite-hint">Log in to save this project.</span>}
            {favouriteAccess === 'unavailable' && <Button type="link" onClick={() => setFavouriteRefreshKey((key) => key + 1)}>Retry saved status</Button>}
          </div>
          {favouriteFeedback && <Alert className="public-project-favourite-feedback"
            showIcon role="status" type={favouriteFeedback.type} message={favouriteFeedback.message} />}
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
          <section className="public-project-photos-panel" aria-labelledby="public-project-photos-title">
            <div className="public-project-photos-heading">
              <h2 id="public-project-photos-title">Project photos</h2>
              {photos.length > 0 && <span>{photos.length} {photos.length === 1 ? 'photo' : 'photos'}</span>}
            </div>
            {photos.length === 0 ? (
              <p className="public-project-no-photos">No project photos have been uploaded yet.</p>
            ) : (
              <div className="public-project-photo-grid">
                {photos.map((photo, index) => (
                  <button className={`public-project-photo-tile${index === 0 ? ' featured' : ''}`}
                    key={photo.id} type="button" onClick={() => setOpenPhotoIndex(index)}
                    aria-label={`Enlarge photo ${index + 1} of ${photos.length}`}>
                    <img src={photo.url} alt={photoAlt(project.title, index)} loading="lazy" />
                    <span className="public-project-photo-caption">Photo {index + 1}</span>
                  </button>
                ))}
              </div>
            )}
          </section>
        </>
      )}
      <Modal open={loginPromptOpen} onCancel={() => setLoginPromptOpen(false)}
        title="Log in to save this project" destroyOnHidden
        footer={<>
          <Button onClick={() => setLoginPromptOpen(false)}>Cancel</Button>
          <Button type="primary" onClick={() => navigate('/login', {
            state: { returnTo: `/projects/${projectId}` },
          })}>Log in</Button>
        </>}>
        <p>Log in with a renovation customer account to save this project to your favourites.</p>
      </Modal>
      <Modal open={selectedPhoto !== null} onCancel={() => setOpenPhotoIndex(null)}
        footer={null} width={900} className="public-project-lightbox" destroyOnHidden
        title={project ? `${project.title} — Photo ${openPhotoIndex + 1} of ${photos.length}` : 'Project photo'}>
        {selectedPhoto && project && <>
          <div className="public-project-lightbox-image">
            <img src={selectedPhoto.url} alt={photoAlt(project.title, openPhotoIndex)} />
          </div>
          <div className="public-project-lightbox-actions">
            <Button onClick={() => setOpenPhotoIndex((index) =>
              shiftedPhotoIndex(index, -1, photos.length))} aria-label="Previous photo">
              ← Previous
            </Button>
            <span aria-live="polite">Photo {openPhotoIndex + 1} of {photos.length}</span>
            <Button onClick={() => setOpenPhotoIndex((index) =>
              shiftedPhotoIndex(index, 1, photos.length))} aria-label="Next photo">
              Next →
            </Button>
          </div>
          <p className="public-project-lightbox-hint">Use the left and right arrow keys to browse photos.</p>
        </>}
      </Modal>
    </section>
  )
}
