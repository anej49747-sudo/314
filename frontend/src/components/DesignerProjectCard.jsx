import { Button } from 'antd'
import { Eye, Heart } from 'lucide-react'
import { Link } from 'react-router-dom'
import ProjectCardLayout from './ProjectCardLayout.jsx'
import { interestMetricCount } from '../designer/projectInterestRules.js'
import '../ui/designerV26.css'

/** Designer-specific controls around the same layout used by Customer favourites. */
export default function DesignerProjectCard({ project, recent = false, onEdit, onDelete }) {
  const metrics = !recent && <div className="portfolio-project-metrics" aria-label={`Interest metrics for ${project.title}`}>
    <div className="portfolio-project-metric"><Eye size={18} aria-hidden="true" />
      <span><strong>{interestMetricCount(project.viewCount)}</strong> Project views</span>
    </div>
    <div className="portfolio-project-metric"><Heart size={18} aria-hidden="true" />
      <span><strong>{interestMetricCount(project.favouriteCount)}</strong> Favourites</span>
    </div>
  </div>
  const actions = <div className="portfolio-project-actions">
    <Link to={`/projects/${project.id}`} target="_blank" rel="noopener noreferrer"
      className="portfolio-preview-link" aria-label={`Preview ${project.title} as a visitor`}>
      <Eye size={15} aria-hidden="true" /> Preview
    </Link>
    <Button size="small" onClick={() => onEdit(project.id)}>Edit project</Button>
    {!recent && <Button size="small" danger onClick={() => onDelete(project)}>
      Delete project
    </Button>}
  </div>
  return <ProjectCardLayout project={project} className="designer-project-card"
    afterDescription={metrics} footer={actions} />
}
