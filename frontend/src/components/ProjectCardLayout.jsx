import { Link } from 'react-router-dom'
import ProjectAttributes from './ProjectAttributes.jsx'
import { projectStatusLabel } from '../project/projectDetailsRules.js'
import { projectSearchDescription, projectSearchPath } from '../project/projectSearchRules.js'
import '../project/projectSearch.css'

/** Shared project presentation. Callers supply role-appropriate metrics and actions. */
export default function ProjectCardLayout({ project, className = '', afterDescription, footer, description = true }) {
  const cover = project.coverImageUrl ?? project.images?.[0]?.url
  const href = projectSearchPath(project.id)
  return <article className={`project-search-card ${className}`.trim()}>
    {cover && <Link to={href} className="project-search-cover-link" aria-label={`View ${project.title}`}>
      <img className="project-search-cover" src={cover} alt={`Cover photo for ${project.title}`} loading="lazy" />
    </Link>}
    <div className="project-search-tags">
      <span className={project.status === 'COMPLETED'
        ? 'project-search-status project-search-status-completed'
        : 'project-search-status project-search-status-current'}>
        {projectStatusLabel(project.status)}
      </span>
    </div>
    <h3>{project.title}</h3>
    <ProjectAttributes project={project} />
    {description && <p className="project-search-description">{projectSearchDescription(project.description)}</p>}
    {afterDescription}
    {footer}
  </article>
}
