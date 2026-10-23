import { Alert, Button, Input, Spin } from 'antd'
import { FileText, Search } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { listArticles } from '../api/articles.js'
import { articleDate, filterArticles } from '../article/articleRules.js'
import '../article/articles.css'

export default function ArticlesPage() {
  const [entries, setEntries] = useState([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    listArticles(controller.signal).then((data) => {
      if (!controller.signal.aborted) setEntries(data)
    }).catch(() => {
      if (!controller.signal.aborted) setError('Unable to load articles. Please try again.')
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [refresh])

  const articles = useMemo(() => filterArticles(entries, query), [entries, query])

  return <section className="articles-page">
    <p className="articles-eyebrow">Renovation advice and guidance</p>
    <h1>Articles</h1>
    <p className="articles-lead">Practical ideas and guidance for planning a renovation.</p>
    <div className="articles-toolbar">
      <Input allowClear prefix={<Search size={17} />} aria-label="Search articles"
        placeholder="Search articles" value={query} onChange={(event) => setQuery(event.target.value)} />
      {!loading && !error && <span role="status">{articles.length} article{articles.length === 1 ? '' : 's'}</span>}
    </div>
    {loading && <div className="loading-state" role="status"><Spin /> Loading articles…</div>}
    {error && <Alert showIcon type="error" role="alert" message={error}
      action={<Button size="small" onClick={() => setRefresh((count) => count + 1)}>Retry</Button>} />}
    {!loading && !error && articles.length === 0 && <div className="articles-empty">
      {entries.length ? 'No articles match your search.' : 'No articles published yet.'}
    </div>}
    {!loading && !error && articles.length > 0 && <div className="articles-grid">
      {articles.map((article) => <article className={`articles-card${article.coverUrl ? ' has-photo' : ''}`} key={article.id}>
        {article.coverUrl ? <img className="articles-card-cover" src={article.coverUrl}
          alt={`Cover photo for ${article.title}`} /> : <span className="articles-icon"><FileText size={22} /></span>}
        <div className="articles-card-body">
          <h2>{article.title}</h2>
          <p>{article.summary}</p>
          <span className="articles-date">Published {articleDate(article.publishedAt)}</span>
          <Link className="articles-read" to={`/articles/${article.id}`}>Read article →</Link>
        </div>
      </article>)}
    </div>}
  </section>
}
