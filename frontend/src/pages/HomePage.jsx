import { Alert, Button, Spin } from 'antd'
import { ArrowRight, BookOpen, Check, FileText, Home, Sparkles } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listArticles } from '../api/articles.js'
import { getDesignerDirectory } from '../api/designerDirectory.js'
import { getPublicProjects } from '../api/publicProjects.js'
import { getPublicRecognitions } from '../api/recognitions.js'
import { articleDate } from '../article/articleRules.js'
import { DesignerAwardBadges } from '../components/RecognitionBadges.jsx'
import { directoryDescription } from '../designer/designerDirectoryRules.js'
import { homepageArticles, homepageFeaturedDetails, homepageRecognisedDesigners } from '../home/homeRules.js'
import '../home/home.css'

const emptyData = { articles: [], designers: [], projects: [], recognitions: { featuredProject: null, awards: [] } }

export default function HomePage() {
  const [data, setData] = useState(emptyData)
  const [loading, setLoading] = useState(true)
  const [failures, setFailures] = useState([])
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    let active = true
    const controller = new AbortController()
    setLoading(true)
    setFailures([])
    const requests = [
      ['articles', listArticles(controller.signal)],
      ['designers', getDesignerDirectory()],
      ['projects', getPublicProjects()],
      ['recognitions', getPublicRecognitions(controller.signal)],
    ]
    Promise.allSettled(requests.map(([, request]) => request)).then((results) => {
      if (!active) return
      const next = { ...emptyData }
      const failed = []
      results.forEach((result, index) => {
        const key = requests[index][0]
        if (result.status === 'fulfilled') next[key] = result.value
        else failed.push(key)
      })
      setData(next)
      setFailures(failed)
      setLoading(false)
    })
    return () => { active = false; controller.abort() }
  }, [revision])

  const featured = homepageFeaturedDetails(data.recognitions?.featuredProject, data.projects)
  const designers = homepageRecognisedDesigners(data.designers, data.recognitions?.awards)
  const articles = homepageArticles(data.articles)

  return <div className="lucky-homepage">
    <section className="lucky-hero" aria-labelledby="lucky-hero-title">
      <div className="lucky-container lucky-hero-grid">
        <div>
          <p className="lucky-eyebrow">Welcome to Lucky Dog</p>
          <h1 id="lucky-hero-title">Find the right interior designer for your renovation.</h1>
          <p className="lucky-hero-copy">Browse real renovation portfolios, discover designers and find the inspiration to make your next space feel like home.</p>
          <div className="lucky-actions">
            <Link className="lucky-btn lucky-btn-primary" to="/projects">Explore projects <ArrowRight size={17} aria-hidden="true" /></Link>
            <Link className="lucky-btn lucky-btn-outline" to="/designers">Find designers <ArrowRight size={17} aria-hidden="true" /></Link>
          </div>
          <div className="lucky-hero-notes">
            <span><Check size={16} aria-hidden="true" /> Explore portfolios</span>
            <span><Check size={16} aria-hidden="true" /> Compare designers</span>
            <span><Check size={16} aria-hidden="true" /> Save inspiration</span>
          </div>
        </div>
        <div className="lucky-hero-media" aria-label="Renovation inspiration photographs">
          <img className="lucky-hero-main-photo" src={featured?.coverUrl || "/homepage/living-room.jpg"} alt={featured?.title ? `Featured renovation: ${featured.title}` : "Warm and bright modern living room with natural finishes"} />
          <img className="lucky-hero-inset-photo" src={articles[0]?.coverUrl || "/homepage/kitchen.jpg"} alt={articles[0]?.coverUrl ? `Renovation inspiration: ${articles[0].title}` : "Modern neutral-toned kitchen renovation"} />
          <span className="lucky-hero-media-tag"><span aria-hidden="true">●</span> Ideas for your next home</span>
        </div>
      </div>
    </section>

    {loading && <div className="lucky-loading" role="status"><Spin /> Loading featured content…</div>}
    {!loading && failures.length > 0 && <div className="lucky-container lucky-data-error">
      <Alert role="alert" type="warning" showIcon message="Some homepage content could not be loaded."
        description="You can still browse the platform using the links below."
        action={<Button size="small" onClick={() => setRevision((v) => v + 1)}>Retry</Button>} />
    </div>}

    <section className="lucky-section" aria-labelledby="lucky-featured-heading">
      <div className="lucky-container">
        <div className="lucky-section-head">
          <div><p className="lucky-eyebrow">Handpicked inspiration</p><h2 id="lucky-featured-heading">Project of the Month</h2>
            <p>Explore a renovation project recognised by our platform.</p></div>
          <Link className="lucky-section-link" to="/projects">Browse all projects <ArrowRight size={16} /></Link>
        </div>
        {featured ? <article className="lucky-featured-card">
          <div className="lucky-featured-media">
            {featured.coverUrl ? <img src={featured.coverUrl} alt={`Featured project: ${featured.title}`} />
              : <span className="lucky-media-placeholder"><Home size={46} aria-hidden="true" /><small>No project photo uploaded</small></span>}
            <span className="lucky-featured-pill">★ Project of the Month</span>
          </div>
          <div className="lucky-featured-info">
            <p className="lucky-eyebrow">Featured renovation</p>
            <h3>{featured.title}</h3>
            <p>{featured.description || 'Discover the inspiration and details behind this featured renovation project.'}</p>
            <div className="lucky-feature-tags"><span>{featured.category}</span>{featured.status && <span>{featured.status === 'COMPLETED' ? 'Completed' : 'Current'}</span>}</div>
            {featured.designerName && <p className="lucky-featured-by">Designed by {featured.designerName}</p>}
            <Link className="lucky-btn lucky-btn-primary" to={`/projects/${encodeURIComponent(featured.id)}`}>Explore featured project <ArrowRight size={17} /></Link>
          </div>
        </article> : <div className="lucky-empty-feature">
          <span className="lucky-empty-icon"><Home size={34} /></span>
          <div><h3>No featured project this month</h3><p>Check back soon, or explore renovation projects already shared by designers.</p>
            <Link to="/projects">Explore projects <ArrowRight size={15} /></Link></div>
        </div>}
      </div>
    </section>

    <section className="lucky-section lucky-section-white" aria-labelledby="lucky-designers-heading">
      <div className="lucky-container">
        <div className="lucky-section-head">
          <div><p className="lucky-eyebrow">Designers to discover</p><h2 id="lucky-designers-heading">Recognised Interior Designers</h2>
            <p>Discover studios celebrated for their work and creativity.</p></div>
          <Link className="lucky-section-link" to="/designers">Meet all designers <ArrowRight size={16} /></Link>
        </div>
        {designers.length ? <div className="lucky-designer-grid">
          {designers.map((designer) => <article key={designer.id} className="lucky-designer-card">
            <div className="lucky-designer-heading"><span className="lucky-designer-avatar" aria-hidden="true">{(designer.name || 'D').trim().charAt(0).toUpperCase()}</span>
              <div><h3>{designer.name}</h3><span>Interior design studio</span></div></div>
            <p className="lucky-designer-description">{directoryDescription(designer.description)}</p>
            <DesignerAwardBadges awards={data.recognitions?.awards} designerId={designer.id} compact />
            <Link to={`/designers/${encodeURIComponent(designer.id)}`} className="lucky-card-link">View designer profile <ArrowRight size={16} /></Link>
          </article>)}
        </div> : <div className="lucky-empty-cards"><Sparkles size={28} aria-hidden="true" />
          <p>No designer awards have been published yet.</p><Link to="/designers">Browse interior designers <ArrowRight size={16} /></Link>
        </div>}
      </div>
    </section>

    <section className="lucky-section" aria-labelledby="lucky-articles-heading">
      <div className="lucky-container">
        <div className="lucky-section-head">
          <div><p className="lucky-eyebrow">Helpful advice</p><h2 id="lucky-articles-heading">Latest Articles</h2>
            <p>Practical renovation ideas and guidance for planning your home.</p></div>
          <Link className="lucky-section-link" to="/articles">Browse all articles <ArrowRight size={16} /></Link>
        </div>
        {articles.length ? <div className="lucky-article-grid">
          {articles.map((article) => <article className="lucky-article-card" key={article.id}>
            <div className="lucky-article-photo">{article.coverUrl
              ? <img src={article.coverUrl} alt={`Cover photo for ${article.title}`} loading="lazy" />
              : <FileText size={35} aria-hidden="true" />}</div>
            <div className="lucky-article-body"><span>Published {articleDate(article.publishedAt)}</span>
              <h3>{article.title}</h3><p>{article.summary}</p>
              <Link className="lucky-card-link" to={`/articles/${encodeURIComponent(article.id)}`}>Read article <ArrowRight size={16} /></Link>
            </div>
          </article>)}
        </div> : <div className="lucky-empty-cards"><BookOpen size={28} aria-hidden="true" />
          <p>No articles have been published yet.</p><Link to="/articles">View articles <ArrowRight size={16} /></Link>
        </div>}
      </div>
    </section>

    <section className="lucky-final-cta" aria-labelledby="lucky-join-heading"><div className="lucky-container lucky-final-inner">
      <div><h2 id="lucky-join-heading">Start exploring your renovation possibilities.</h2>
        <p>Find inspiration, discover designers and save the projects that speak to you.</p></div>
      <Link className="lucky-btn lucky-btn-primary" to="/register">Create your free account <ArrowRight size={17} /></Link>
    </div></section>
    <footer className="lucky-footer"><div className="lucky-container lucky-footer-inner">
      <span><strong>Lucky Dog</strong> · Renovation matching platform</span>
      <span>Explore · Compare · Get inspired</span>
    </div></footer>
  </div>
}
