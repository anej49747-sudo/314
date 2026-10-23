import { compactDesignerAwards, designerAwards, isFeaturedProject } from '../recognition/recognitionRules.js'
import '../recognition/recognition.css'

export function FeaturedProjectBadge({ featuredProject, projectId }) {
  return isFeaturedProject(featuredProject, projectId)
    ? <span className="recognition-featured-badge">★ Project of the Month</span> : null
}

export function DesignerAwardBadges({ awards, designerId, compact = false }) {
  const all = designerAwards(awards, designerId)
  if (!all.length) return null
  const { displayed, overflow } = compactDesignerAwards(awards, designerId)
  const shown = compact ? displayed : all
  return <div className="recognition-awards" aria-label={`Awards (${all.length})`}>
    {shown.map((award) => <span className="recognition-award-badge" key={award.id}
      title={compact ? award.title : undefined}>
      ★ {award.title}{!compact && award.awardedAt &&
        <small> · {new Date(award.awardedAt).toLocaleDateString('en-SG', { timeZone: 'Asia/Singapore', year: 'numeric', month: 'short', day: 'numeric' })}</small>}
    </span>)}
    {compact && overflow > 0 && <span className="recognition-overflow">+{overflow} more</span>}
  </div>
}
