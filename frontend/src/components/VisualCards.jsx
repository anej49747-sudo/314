import { ArrowRight, Image as ImageIcon } from 'lucide-react'
import { Link } from 'react-router-dom'

/** A consistent, keyboard-accessible action card for customer/designer/manager hubs. */
export function ActionCard({ to, title, description, Icon, variant = 'default' }) {
  return <Link to={to} className={`ui-shortcut-card${variant === 'management' ? ' ui-management-card' : ''}`}>
    <span className="ui-shortcut-icon"><Icon size={22} aria-hidden="true" /></span>
    <span className="ui-shortcut-title">{title}</span>
    <span className="ui-shortcut-detail">{description}</span>
    <ArrowRight className="ui-shortcut-arrow" size={18} aria-hidden="true" />
  </Link>
}

/** Image-forward preview shared by customer and designer dashboard cards. */
export function ImagePreviewCard({ to, imageUrl, title, subtitle, subtitleLabel = null, status = null, fallbackIcon: Fallback = ImageIcon }) {
  return <Link to={to} className="ui-image-card">
    {imageUrl ? <img src={imageUrl} alt={`Interior design for ${title}`} loading="lazy" />
      : <span className="ui-image-empty"><Fallback size={28} aria-hidden="true" /></span>}
    <span className="ui-image-card-content">{status && <span className={`project-search-status ${status === 'COMPLETED' ? 'project-search-status-completed' : 'project-search-status-current'}`}>{status === 'COMPLETED' ? 'Completed' : 'Current'}</span>}<strong>{title}</strong>{subtitle && <span className="ui-image-subtitle">{subtitleLabel && <span className="ld-category-kicker">{subtitleLabel}</span>}{subtitle}</span>}
      <span className="ui-image-card-cta">View details <ArrowRight size={15} aria-hidden="true" /></span></span>
  </Link>
}

export function SectionHeading({ id, title, description, action }) {
  return <div className="ui-section-heading"><div><h2 id={id}>{title}</h2>{description && <p>{description}</p>}</div>{action}</div>
}
