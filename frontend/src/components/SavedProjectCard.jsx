import { Button } from 'antd'
import { Heart } from 'lucide-react'
import { Link } from 'react-router-dom'
import ProjectCardLayout from './ProjectCardLayout.jsx'
import { projectSearchPath } from '../project/projectSearchRules.js'
import '../project/savedProjects.css'

/** Identical saved-project presentation on the dashboard and in My Favourites. */
export default function SavedProjectCard({ project, onRemove, removing = false, removeDisabled = false }) {
  const detailsPath = projectSearchPath(project.id)
  return <ProjectCardLayout project={project} className="saved-project-card"
    afterDescription={<div className="saved-project-card-designer">
      <span className="project-search-designer-label">Designed by</span>
      <span className="project-search-designer-name">{project.designer?.name || 'Interior designer'}</span>
    </div>}
    footer={<div className="saved-project-card-actions">
      <Link to={detailsPath} aria-label={`View ${project.title} project details`}>
        View project <span aria-hidden="true">→</span>
      </Link>
      {onRemove && <Button type="text" danger size="small"
        loading={removing} disabled={removeDisabled}
        onClick={() => onRemove(project.id)}
        aria-label={`Remove ${project.title} from favourites`}>
        <Heart size={15} fill="currentColor" aria-hidden="true" /> Remove
      </Button>}
    </div>} />
}
