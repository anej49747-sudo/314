import { Link } from 'react-router-dom'
import '../project/projectDetails.css'

// Search/listing UI belongs to Story #5; details work independently via /projects/:projectId.
export default function ProjectsListingPlaceholderPage() {
  return (
    <section className="public-project-details" aria-labelledby="projects-pending-title">
      <p className="public-profile-eyebrow">Explore projects</p>
      <h1 id="projects-pending-title">Interior design projects</h1>
      <div className="public-project-panel public-project-listing-pending">
        <h2>Project browsing is coming soon</h2>
        <p>Project search and listings will be introduced in Story #5. You can already open a project directly or from an interior designer's portfolio.</p>
        <Link className="public-project-designer-link" to="/designers">
          Browse interior designers <span aria-hidden="true">→</span>
        </Link>
      </div>
    </section>
  )
}
