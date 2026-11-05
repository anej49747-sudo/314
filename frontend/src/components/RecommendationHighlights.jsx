import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getDesignerRecommendations, getRecommendations } from '../api/discovery.js'
import '../ui/discovery.css'
function Score({ value }) { return value == null ? null : <div className="discovery-score"><div><span>Your match</span><strong>{value}%</strong></div><div className="discovery-score-track"><span style={{ width: `${value}%` }}/></div></div> }
export default function RecommendationHighlights({ designersOnly = false }) {
  const [projects, setProjects] = useState([]); const [designers, setDesigners] = useState([]); const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    const request = designersOnly ? getDesignerRecommendations(4).then(d => ({ designers: d }))
      : Promise.all([getRecommendations({ size: 4 }), getDesignerRecommendations(4)]).then(([p, d]) => ({ projects: p.items, designers: d }))
    request.then(result => {
      if (active) { setProjects(result.projects ?? []); setDesigners(result.designers ?? []) }
    }).catch(() => { if (active) setError('Recommendations are currently unavailable.') })
    return () => { active = false }
  }, [designersOnly])
  return <section className="discovery-highlights"><h2>{designersOnly ? 'Recommended designers' : 'Recommended for you'}</h2>
    {error && <p role="status">{error}</p>}
    {!error && <>{!designersOnly && <><h3>Interior design projects</h3><div className="discovery-recommend-grid">{projects.map(p => <Link className="discovery-recommend-card" to={`/projects/${p.id}`} key={p.id}>
      {p.photos?.[0] ? <img src={p.photos[0]} alt="" loading="lazy" /> : <div className="discovery-missing">Photo pending setup</div>}
      <div><strong>{p.title}</strong><p>{p.designerName}</p><Score value={p.score}/></div></Link>)}</div>
      {!projects.length && <p>No projects available yet. <Link to="/explore">Explore the feed</Link> or set your preferences to get started.</p>}</>}
      <h3>Interior designers</h3><div className="discovery-recommend-grid">{designers.map(d => <Link className="discovery-recommend-card" key={d.id} to={`/designers/${d.id}`}><div><strong>{d.name}</strong><Score value={d.score}/></div></Link>)}</div>
      {!designers.length && <p>No designers with eligible projects yet.</p>}</>}
  </section>
}
