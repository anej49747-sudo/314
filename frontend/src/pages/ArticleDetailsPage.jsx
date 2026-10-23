import { Alert, Button, Spin } from 'antd'
import { ArrowLeft } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getArticle } from '../api/articles.js'
import { articleDate, articleParagraphs } from '../article/articleRules.js'
import '../article/articles.css'

export default function ArticleDetailsPage() {
  const { articleId } = useParams()
  const [article, setArticle] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notFound, setNotFound] = useState(false)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    setNotFound(false)
    getArticle(articleId, controller.signal).then((data) => {
      if (!controller.signal.aborted) setArticle(data)
    }).catch((failure) => {
      if (controller.signal.aborted) return
      if (failure.response?.status === 404) setNotFound(true)
      else setError('Unable to load this article. Please try again.')
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [articleId, revision])

  return <section className="article-detail-page">
    <Link to="/articles" className="article-back"><ArrowLeft size={16} /> Back to articles</Link>
    {loading && <div className="loading-state"><Spin /> Loading article…</div>}
    {!loading && notFound && <Alert type="warning" showIcon message="Article not found" />}
    {!loading && error && <Alert type="error" role="alert" showIcon message={error}
      action={<Button size="small" onClick={() => setRevision((value) => value + 1)}>Retry</Button>} />}
    {!loading && !error && !notFound && article && <article className="article-detail">
      <h1>{article.title}</h1>
      <p className="article-detail-summary">{article.summary}</p>
      <p className="article-detail-date">Published {articleDate(article.publishedAt)}</p>
      {article.coverUrl && <img src={article.coverUrl} className="article-detail-cover"
        alt={`Cover photo for ${article.title}`} />}
      <div className="article-detail-content">
        {articleParagraphs(article.content).map((paragraph, index) => <p key={index}>{paragraph}</p>)}
      </div>
    </article>}
  </section>
}
