import { Link, useLocation } from 'react-router-dom'
import { designerProfilePath } from '../designer/designerDirectoryRules.js'

// Project details and their API will be delivered in Story #6, not Story #3.
export default function DesignerProjectPlaceholderPage() {
  const { state } = useLocation()
  const backTo = state?.designerId ? designerProfilePath(state.designerId) : '/designers'
  return (
    <section className="designer-profile-placeholder" aria-labelledby="project-placeholder-title">
      <Link to={backTo} className="designer-profile-back">
        ← Back to {state?.designerId ? 'designer profile' : 'all interior designers'}
      </Link>
      <div className="designer-profile-pending">
        <p className="public-profile-eyebrow">Interior design project</p>
        <h1 id="project-placeholder-title">{state?.projectTitle || 'Project details'}</h1>
        <p>Project details are coming in Story #6. This is a placeholder, not a completed project details page.</p>
      </div>
    </section>
  )
}
