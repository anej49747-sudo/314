import { Alert, Button, Spin } from 'antd'
import { ArrowRight, Heart, MessageSquare, Search, ShieldCheck, Users } from 'lucide-react'
import { ActionCard, ImagePreviewCard, SectionHeading } from '../components/VisualCards.jsx'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import { getSavedProjects } from '../api/favourites.js'
import { accountAreaPath } from '../auth/headerSessionRules.js'

const shortcuts = [
  { title: 'Explore projects', detail: 'Discover inspiring renovations', to: '/projects', Icon: Search },
  { title: 'My favourites', detail: 'Revisit projects you saved', to: '/customer/favourites', Icon: Heart },
  { title: 'Compare designers', detail: 'Find the right studio for you', to: '/customer/compare', Icon: Users },
  { title: 'My messages', detail: 'Chat privately with designers', to: '/customer/messages', Icon: MessageSquare },
]

export default function CustomerPage() {
  const navigate = useNavigate()
  const [account, setAccount] = useState(null)
  const [saved, setSaved] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [savedError, setSavedError] = useState('')

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

  return <section className="ui-dashboard" aria-labelledby="customer-dashboard-title">
    <header className="ui-dashboard-header">
      <div>
        <p className="ui-eyebrow">My space</p>
        <h1 id="customer-dashboard-title">{account ? `Welcome back, ${account.displayName}` : 'Your renovation space'}</h1>
        <p>Explore design ideas, keep your favourites close and discover the right interior designer.</p>
      </div>

    </header>
    {loading && <div className="loading-state" role="status"><Spin /> Loading your workspace…</div>}
    {error && <Alert type="error" role="alert" showIcon message={error} />}
    {account && <>
      <div className="ui-shortcuts" aria-label="Customer quick actions">
        {shortcuts.map(({ title, detail, to, Icon }) => <ActionCard key={to} to={to} title={title} description={detail} Icon={Icon} />)}
      </div>
      <Link to="/customer/security" className="ui-security-link"><ShieldCheck size={18} aria-hidden="true" /> Account security — change email or password <ArrowRight size={16} aria-hidden="true" /></Link>
      <section className="ui-dashboard-section" aria-labelledby="customer-saved-heading">
        <SectionHeading id="customer-saved-heading" title="Your saved inspiration"
          description="Pick up where you left off with your favourite spaces."
          action={<Link to="/customer/favourites" className="ui-text-action">View all favourites <ArrowRight size={16} /></Link>} />
        {savedError && <Alert type="warning" showIcon message={savedError} />}
        {!savedError && saved.length === 0 && <div className="ui-empty-state"><Heart size={28} aria-hidden="true" />
          <h3>No saved projects yet</h3><p>Find something you love and save it for later.</p>
          <Button type="primary" onClick={() => navigate('/projects')}>Explore projects</Button></div>}
        {!savedError && saved.length > 0 && <div className="ui-image-grid ui-customer-saved">
          {saved.slice(0, 6).map(project => <ImagePreviewCard key={project.id}
            to={`/projects/${project.id}`} imageUrl={project.coverImageUrl}
            title={project.title} subtitle={project.categoryName} fallbackIcon={Heart} />)}
        </div>}
      </section>
    </>}
  </section>
}
