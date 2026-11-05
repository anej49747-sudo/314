import { Alert, Button, Empty, Modal, Spin } from 'antd'
import { ChevronLeft, ChevronRight, Heart } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { currentSession } from '../api/authentication.js'
import { getExploreProjects } from '../api/discovery.js'
import { removeFavourite, saveFavourite } from '../api/favourites.js'
import { projectStatusLabel } from '../project/projectDetailsRules.js'
import { freshReelProjects, reelPhotoIndex } from '../discovery/reelRules.js'
import ProjectAttributes from '../components/ProjectAttributes.jsx'
import '../ui/discovery.css'

const BATCH_SIZE = 6

export default function ExplorePage() {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [photosByProject, setPhotosByProject] = useState({})
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [customer, setCustomer] = useState(false)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [favouriteBusyId, setFavouriteBusyId] = useState(null)
  const [loginPromptOpen, setLoginPromptOpen] = useState(false)
  const seen = useRef(new Set())
  const fetching = useRef(false)
  const changingFavourite = useRef(false)
  const feedRef = useRef(null)

  useEffect(() => {
    currentSession().then(session => setCustomer(session.role === 'CUSTOMER')).catch(() => setCustomer(false))
  }, [])

  // Explore is a general photo feed, not a list of personalised matches.
  // Exclude already loaded IDs so the scroll session never repeats a project.
  const load = useCallback(async () => {
    if (fetching.current) return
    fetching.current = true
    setLoading(true)
    setError('')
    try {
      const data = await getExploreProjects({ page: 0, size: BATCH_SIZE, excludeIds: [...seen.current] })
      const fresh = freshReelProjects(seen.current, data.items ?? [])
      fresh.forEach(project => seen.current.add(project.id))
      setItems(current => [...current, ...fresh])
      setHasMore(Boolean(data.hasMore) && fresh.length > 0)
    } catch {
      setError('Unable to load Explore projects. Please try again.')
    } finally {
      fetching.current = false
      setLoading(false)
    }
  }, [])

  useEffect(() => { void load() }, [load])

  function onFeedScroll() {
    const feed = feedRef.current
    if (!feed || !hasMore || fetching.current || error) return
    const distanceToEnd = feed.scrollHeight - feed.scrollTop - feed.clientHeight
    if (distanceToEnd <= feed.clientHeight * 1.5) void load()
  }

  function movePhoto(id, delta, count) {
    setPhotosByProject(current => ({
      ...current,
      [id]: reelPhotoIndex(current[id] ?? 0, delta, count),
    }))
  }

  async function favourite(project) {
    if (!customer) { setLoginPromptOpen(true); return }
    if (changingFavourite.current) return
    changingFavourite.current = true
    setFavouriteBusyId(project.id)
    setActionError('')
    try {
      if (project.saved) await removeFavourite(project.id)
      else await saveFavourite(project.id)
      // Keep this post visible in this browsing session after either action.
      setItems(current => current.map(item => item.id === project.id ? { ...item, saved: !project.saved } : item))
    } catch (failure) {
      if ([401, 403].includes(failure.response?.status)) {
        setCustomer(false)
        setLoginPromptOpen(true)
      } else setActionError('Could not update favourites. Please try again.')
    } finally {
      changingFavourite.current = false
      setFavouriteBusyId(null)
    }
  }

  return <section className="discovery-page">
    <p className="discovery-eyebrow">Discover inspiration</p>
    <h1>Explore</h1>
    <div className="discovery-explore-heading">
      <p>Browse renovation ideas and save projects you like.</p>
      <Link to="/projects">Search all projects</Link>
    </div>
    {actionError && <Alert type="error" role="alert" showIcon message={actionError} closable onClose={() => setActionError('')} />}
    {!items.length && loading && <div className="discovery-reel-loading"><Spin /></div>}
    {!items.length && !loading && !error && <Empty description="Pending setup — no project photos available yet." />}

    {items.length > 0 && <div
      className="discovery-reel-feed"
      ref={feedRef}
      onScroll={onFeedScroll}
      role="region"
      aria-label="Explore projects: scroll vertically for the next project"
      tabIndex={0}
    >
      {items.map(project => {
        const photos = project.photos ?? []
        const selected = reelPhotoIndex(photosByProject[project.id] ?? 0, 0, photos.length)
        return <article className="discovery-reel-post" key={project.id}>
          <div
            className="discovery-reel-photo"
            onTouchStart={event => {
              event.currentTarget.dataset.touchX = event.touches[0].clientX
              event.currentTarget.dataset.touchY = event.touches[0].clientY
            }}
            onTouchEnd={event => {
              const fromX = Number(event.currentTarget.dataset.touchX)
              const fromY = Number(event.currentTarget.dataset.touchY)
              const deltaX = event.changedTouches[0].clientX - fromX
              const deltaY = event.changedTouches[0].clientY - fromY
              // Vertical swipes scroll the native feed. Horizontal swipes change photos.
              if (Math.abs(deltaX) > 45 && Math.abs(deltaX) > Math.abs(deltaY)) {
                movePhoto(project.id, deltaX < 0 ? 1 : -1, photos.length)
              }
            }}
          >
            <span className="discovery-photo-placeholder">Photo pending setup</span>
            {photos.length > 0 && <img
              key={`${project.id}-${selected}`}
              src={photos[selected]}
              alt={`${project.title}, photo ${selected + 1}`}
              loading="lazy"
              onError={event => { event.currentTarget.style.display = 'none' }}
            />}
            {photos.length > 1 && <>
              <div className="discovery-photo-nav">
                <Button shape="circle" icon={<ChevronLeft size={18}/>} disabled={selected === 0} onClick={() => movePhoto(project.id, -1, photos.length)} aria-label="Previous photograph" />
                <Button shape="circle" icon={<ChevronRight size={18}/>} disabled={selected === photos.length - 1} onClick={() => movePhoto(project.id, 1, photos.length)} aria-label="Next photograph" />
              </div>
              <span className="discovery-photo-count">{selected + 1} / {photos.length}</span>
            </>}
          </div>
          <div className="discovery-reel-info">
            <p className="discovery-reel-overline">Project discovery</p>
            <div className="discovery-title-row">
              <div className="discovery-reel-title">
                <h2>{project.title}</h2>
                <p className="discovery-reel-status">{projectStatusLabel(project.status)}</p>
              </div>
            </div>
            <ProjectAttributes project={{ categoryName: project.categoryName, metadata: project.metadata }} />
            <div className="discovery-reel-description">
              <h3>About this project</h3>
              <p>{project.description?.trim() || 'No project description is available.'}</p>
            </div>
            <div className="discovery-reel-designer">
              <span>Designed by</span>
              <Link to={`/designers/${project.designerId}`}>{project.designerName || 'View designer profile'}</Link>
            </div>
            <div className="discovery-actions">
              <Button className={`discovery-reel-favourite${project.saved ? ' saved' : ''}`}
                icon={<Heart size={17} fill={project.saved ? 'currentColor' : 'none'} />}
                aria-pressed={Boolean(project.saved)} aria-label={project.saved ? 'Remove from favourites' : 'Save to favourites'}
                loading={favouriteBusyId === project.id} disabled={favouriteBusyId != null}
                onClick={() => favourite(project)}>{project.saved ? 'Saved to Favourites' : 'Save to Favourites'}</Button>
              <Button onClick={() => navigate(`/projects/${project.id}`)}>View project details</Button>
            </div>
          </div>
        </article>
      })}
    </div>}

    {items.length > 0 && <div className="discovery-reel-footer" role="status" aria-live="polite">
      {loading && <><Spin size="small" /> Loading more projects…</>}
      {!loading && error && <><span>{error}</span><Button onClick={() => load()}>Retry loading</Button></>}
      {!loading && !error && !hasMore && <span>You've reached the end of the available projects.</span>}
    </div>}
    {!items.length && !loading && error && <div className="discovery-reel-footer" role="alert">
      <span>{error}</span><Button onClick={() => load()}>Retry loading</Button>
    </div>}
    <Modal open={loginPromptOpen} title="Log in to save this project" destroyOnHidden
      onCancel={() => setLoginPromptOpen(false)}
      footer={<><Button onClick={() => setLoginPromptOpen(false)}>Cancel</Button>
        <Button type="primary" onClick={() => navigate('/login', { state: { returnTo: '/explore' } })}>Log in</Button></>}>
      <p>Log in with a renovation customer account to add projects to your favourites.</p>
    </Modal>
  </section>
}
