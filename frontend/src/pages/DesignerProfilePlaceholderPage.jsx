import { Link, useLocation } from 'react-router-dom'

// Story #3 will replace this placeholder with the real public profile view.
export default function DesignerProfilePlaceholderPage() {
  const { state } = useLocation()
  return (
    <section className="designer-profile-placeholder">
      <Link to="/designers" className="designer-profile-back">← Back to all interior designers</Link>
      <h1>{state?.directoryName || 'Designer profile'}</h1>
      <div className="designer-profile-pending" role="status">
        <h2>Profile coming soon</h2>
        <p>Detailed designer profiles and portfolios will be available here soon.</p>
      </div>
    </section>
  )
}
