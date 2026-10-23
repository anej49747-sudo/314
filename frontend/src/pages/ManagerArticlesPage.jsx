import { Alert, Button, Card, Form, Input, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listArticles, publishArticle } from '../api/articles.js'
import { currentManager } from '../api/categories.js'
import { articleDate, articleFailureMessage, articlePayload, validateCover } from '../article/articleRules.js'
import '../article/articles.css'

export default function ManagerArticlesPage() {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [authorized, setAuthorized] = useState(false)
  const [checking, setChecking] = useState(true)
  const [entries, setEntries] = useState([])
  const [cover, setCover] = useState(null)
  const [preview, setPreview] = useState('')
  const [coverError, setCoverError] = useState('')
  const [publishing, setPublishing] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let active = true
    currentManager().then(() => { if (active) setAuthorized(true) }).catch((failure) => {
      if (!active) return
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setError('Unable to verify Platform Manager access.')
    }).finally(() => { if (active) setChecking(false) })
    return () => { active = false }
  }, [navigate])

  useEffect(() => {
    if (!authorized) return undefined
    let active = true
    listArticles().then((data) => { if (active) setEntries(data) })
      .catch(() => { if (active) setError('Unable to load published articles.') })
    return () => { active = false }
  }, [authorized])

  useEffect(() => {
    if (!cover) { setPreview(''); return undefined }
    const objectUrl = URL.createObjectURL(cover)
    setPreview(objectUrl)
    return () => URL.revokeObjectURL(objectUrl)
  }, [cover])

  function chooseCover(event) {
    const file = event.target.files?.[0] || null
    const validation = validateCover(file)
    setCoverError(validation)
    setCover(validation ? null : file)
    // Allow re-selecting an identical file after removal.
    event.target.value = ''
  }

  async function onPublish(values) {
    if (publishing) return
    setError('')
    setSuccess('')
    if (coverError) return
    let details
    try { details = articlePayload(values) } catch (failure) { setError(failure.message); return }
    setPublishing(true)
    try {
      const published = await publishArticle(details, cover)
      setEntries((previous) => [published, ...previous])
      setSuccess(`“${published.title}” published successfully. Visitors can read it now.`)
      form.resetFields()
      setCover(null)
      setCoverError('')
    } catch (failure) {
      setError(articleFailureMessage(failure))
    } finally { setPublishing(false) }
  }

  return <section className="manager-articles-page">
    <p className="articles-eyebrow">Platform management</p>
    <h1>Create Article</h1>
    <p className="articles-lead">Publish renovation guidance for visitors. Articles appear immediately in Articles.</p>
    {checking && <div className="loading-state"><Spin /> Checking manager access…</div>}
    {error && <Alert className="article-feedback" type="error" role="alert" showIcon message={error} />}
    {success && <Alert className="article-feedback" type="success" role="status" showIcon message={success} />}
    {authorized && !checking && <div className="manager-articles-grid">
      <Card title="Write and publish" className="articles-editor">
        <Form form={form} layout="vertical" disabled={publishing} onFinish={onPublish}>
          <Form.Item name="title" label="Article title" rules={[{ required: true, whitespace: true },
            { min: 5, max: 120, message: 'Title must be 5–120 characters.' }]}>
            <Input maxLength={120} showCount placeholder="e.g. Planning a renovation budget" />
          </Form.Item>
          <Form.Item name="summary" label="Short summary" rules={[{ required: true, whitespace: true },
            { min: 20, max: 240, message: 'Summary must be 20–240 characters.' }]}>
            <Input.TextArea maxLength={240} showCount rows={3}
              placeholder="Shown on the Articles page" />
          </Form.Item>
          <Form.Item name="content" label="Article content" rules={[{ required: true, whitespace: true },
            { min: 100, max: 10000, message: 'Content must be 100–10,000 characters.' }]}>
            <Input.TextArea rows={11} maxLength={10000} showCount
              placeholder="Write your article here. Separate paragraphs with a blank line." />
          </Form.Item>
          <div className="article-photo-field">
            <label htmlFor="article-cover">Cover photo <span>(Optional — one photo)</span></label>
            <input id="article-cover" type="file" accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
              onChange={chooseCover} aria-describedby="article-cover-help" disabled={publishing} />
            <p id="article-cover-help">JPG, PNG or WebP. Maximum 5 MB. Appears on the article card and full article.</p>
            {coverError && <Alert role="alert" showIcon type="error" message={coverError}
              action={<Button size="small" onClick={() => setCoverError('')}>Continue without photo</Button>} />}
            {cover && preview && <div className="article-cover-preview">
              <img src={preview} alt="Selected article cover preview" />
              <div><strong>Cover photo selected</strong><p>{cover.name}</p>
                <Button type="link" danger onClick={() => { setCover(null); setCoverError('') }}
                  disabled={publishing}>Remove photo</Button></div>
            </div>}
          </div>
          <div className="article-publish-actions">
            <Button type="primary" htmlType="submit" loading={publishing} disabled={Boolean(coverError)}>
              Publish article
            </Button>
          </div>
        </Form>
      </Card>
      <Card title={`Published articles (${entries.length})`} className="articles-published">
        {entries.length === 0 ? <p>No published articles yet.</p> : entries.map((article) =>
          <div key={article.id} className="article-published-row">
            {article.coverUrl && <img src={article.coverUrl} alt="" />}
            <div><span className="articles-published-tag">Published</span>
              <h3><Link to={`/articles/${article.id}`}>{article.title}</Link></h3>
              <p>{articleDate(article.publishedAt)}</p></div>
          </div>)}
      </Card>
    </div>}
  </section>
}
