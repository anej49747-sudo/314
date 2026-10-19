import { Alert, Button, Card, Form, Input, List, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createCategory, currentManager, listCategories } from '../api/categories.js'
import { logoutSession } from '../api/authentication.js'
import { buildCategoryPayload, categoryCreationFailureMessage } from '../category/categoryRules.js'

export default function ProjectCategoriesPage() {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const [authorized, setAuthorized] = useState(false)
  const [loading, setLoading] = useState(true)
  const [categories, setCategories] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let active = true
    async function initialize() {
      try {
        await currentManager()
        if (!active) return
        setAuthorized(true)
        const entries = await listCategories()
        if (active) setCategories(entries)
      } catch (failure) {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to load the categories. Please try again.')
      } finally {
        if (active) setLoading(false)
      }
    }
    initialize()
    return () => { active = false }
  }, [navigate])

  async function onSubmit(values) {
    if (submitting) return
    setSubmitting(true)
    setError('')
    setSuccess('')
    try {
      const created = await createCategory(buildCategoryPayload(values))
      form.resetFields()
      setSuccess(`Category “${created.name}” created successfully.`)
      try {
        setCategories(await listCategories())
      } catch {
        setError('Category saved, but the list could not be refreshed. Reload the page to retry.')
      }
    } catch (failure) {
      const fields = failure.response?.data?.fieldErrors
      if (fields?.name) form.setFields([{ name: 'name', errors: [fields.name] }])
      setError(categoryCreationFailureMessage(failure))
    } finally {
      setSubmitting(false)
    }
  }

  async function onLogout() {
    if (loggingOut) return
    setLoggingOut(true)
    setError('')
    try {
      await logoutSession()
      navigate('/login', { replace: true, state: { justLoggedOut: true } })
    } catch {
      setError('Unable to log out. Please try again.')
      setLoggingOut(false)
    }
  }

  return (
    <div className="categories-page">
      <p className="categories-eyebrow">Platform management</p>
      <h1>Project categories</h1>
      <p className="categories-intro">Create categories to help organise interior design projects for browsing.</p>
      {loading && <div className="loading-state"><Spin /> <span>Checking manager access…</span></div>}
      {error && <Alert type="error" showIcon role="alert" message={error} className="form-feedback" />}
      {success && <Alert type="success" showIcon role="status" message={success} className="form-feedback" />}
      {authorized && !loading && <>
        <Card className="categories-panel" title="Create a category">
          <p className="categories-description">Enter a clear category name, then create the category.</p>
          <Form form={form} layout="vertical" onFinish={onSubmit} disabled={submitting}>
            <Form.Item name="name" label="Category name" rules={[
              { required: true, whitespace: true, message: 'Enter a category name.' },
              { max: 100, message: 'Category name must be 100 characters or fewer.' },
            ]} extra="Required. Category names should be unique.">
              <Input placeholder="Enter category name" maxLength={100} autoComplete="off" />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>Create category</Button>
          </Form>
        </Card>
        <Card className="categories-panel" title={`Categories (${categories.length})`}>
          <List dataSource={categories} locale={{ emptyText: 'No categories yet. Create your first category above.' }}
            renderItem={(category) => <List.Item key={category.id}>{category.name}</List.Item>} />
        </Card>
        <div className="categories-footer"><Button type="link" loading={loggingOut} onClick={onLogout}>Log out</Button></div>
      </>}
    </div>
  )
}
