import { Alert, Button, Spin } from 'antd'
import { ArrowRight, Heart, MessageSquare, SlidersHorizontal, ShieldCheck, Users } from 'lucide-react'
import { ActionCard, SectionHeading } from '../components/VisualCards.jsx'
import SavedProjectCard from '../components/SavedProjectCard.jsx'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import { getSavedProjects, removeFavourite } from '../api/favourites.js'
import { withoutSavedProject } from '../project/savedProjectsRules.js'
import { accountAreaPath } from '../auth/headerSessionRules.js'
import RecommendationHighlights from '../components/RecommendationHighlights.jsx'

const shortcuts = [
  { title: 'My preferences', detail: 'Personalise your recommendations', to: '/customer/preferences', Icon: SlidersHorizontal },
  { title: 'My favourites', detail: 'Revisit projects you saved', to: '/customer/favourites', Icon: Heart },
  { title: 'Compare designers', detail: 'Find the right studio for you', to: '/customer/compare', Icon: Users },
  { title: 'My messages', detail: 'Chat privately with designers', to: '/customer/messages', Icon: MessageSquare },
  { title: 'Account security', detail: 'Change email or password', to: '/customer/security', Icon: ShieldCheck },
]

export default function CustomerPage() {
  const navigate = useNavigate()
  const [account, setAccount] = useState(null)
  const [saved, setSaved] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [savedError, setSavedError] = useState('')
  const [savedFeedback, setSavedFeedback] = useState('')
  const [removingId, setRemovingId] = useState(null)

  useEffect(() => {
    let active = true
    async function initialize() {
      try {
        const profile = await currentCustomer()
        if (!active) return
        if (profile.role !== 'CUSTOMER') {
          navigate(accountAreaPath(profile) || '/login', { replace: true })
          return
        }
        setAccount(profile)
        try {
          const projects = await getSavedProjects()
          if (active) setSaved(projects)
        } catch {
          if (active) setSavedError('Your saved projects could not be loaded right now.')
        }
      } catch (failure) {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to load your customer workspace. Please try again.')
      } finally {
        if (active) setLoading(false)
      }
    }
    initialize()
    return () => { active = false }
  }, [navigate])

  async function onRemoveSaved(projectId) {
    if (removingId !== null) return
    setRemovingId(projectId)
    setSavedFeedback('')
    try {
      await removeFavourite(projectId)
      setSaved((current) => withoutSavedProject(current, projectId))
      setSavedFeedback('Project removed from favourites.')
    } catch (failure) {
      if ([401, 403].includes(failure.response?.status)) {
        navigate('/login', { replace: true, state: { returnTo: '/customer' } })
      } else {
        setSavedFeedback('Unable to remove the project. Please try again.')
      }
    } finally {
      setRemovingId(null)
    }
  }

  return <section className="ui-dashboard" aria-labelledby="customer-dashboard-title">
    <header className="ui-dashboard-header">
      <div>
        <p className="ui-eyebrow">My dashboard</p>
        <h1 id="customer-dashboard-title">{account ? `Welcome back, ${account.displayName}` : 'Your renovation space'}</h1>
        <p>Your renovation inspiration, recommendations and account tools.</p>
      </div>

    </header>
    {loading && <div className="loading-state" role="status"><Spin /> Loading your workspace…</div>}
    {error && <Alert type="error" role="alert" showIcon message={error} />}
    {account && <>
      <div className="ui-shortcuts" aria-label="Customer quick actions">
        {shortcuts.map(({ title, detail, to, Icon }) => <ActionCard key={to} to={to} title={title} description={detail} Icon={Icon} />)}
      </div>
      <RecommendationHighlights />
      <section className="ui-dashboard-section" aria-labelledby="customer-saved-heading">
        <SectionHeading id="customer-saved-heading" title="Your saved inspiration"
          description="Pick up where you left off with your favourite spaces."
          action={<Link to="/customer/favourites" className="ui-text-action">View all favourites <ArrowRight size={16} /></Link>} />
        {savedError && <Alert type="warning" showIcon message={savedError} />}
        {savedFeedback && <Alert showIcon role="status" type={savedFeedback.startsWith('Unable') ? 'error' : 'success'} message={savedFeedback} />}
        {!savedError && saved.length === 0 && <div className="ui-empty-state"><Heart size={28} aria-hidden="true" />
          <h3>No saved projects yet</h3><p>Find something you love and save it for later.</p>
          <Button type="primary" onClick={() => navigate('/explore')}>Explore inspiration</Button></div>}
        {!savedError && saved.length > 0 && <div className="project-search-grid ui-customer-saved" aria-label="Saved interior design projects">
          {saved.slice(0, 6).map(project => <SavedProjectCard key={project.id}
            project={project} onRemove={onRemoveSaved} removing={removingId === project.id}
            removeDisabled={removingId !== null} />)}
        </div>}
      </section>
    </>}
  </section>
}
