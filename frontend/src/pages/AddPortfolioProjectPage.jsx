import DesignerReturnButton from '../components/DesignerReturnButton.jsx'
import { Alert, Button, Card, Form, Input, Radio, Select, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listCategories } from '../api/categories.js'
import { currentDesigner, currentDesignerArea } from '../api/designerAuthentication.js'
import { addPortfolioProject } from '../api/portfolio.js'
import { FLOOR_AREA_ERROR } from '../designer/floorAreaRules.js'
import ProjectMetadataFields from '../components/ProjectMetadataFields.jsx'
import {
  buildProjectDetails, projectCreationFailureMessage, validateProjectImages,
} from '../designer/projectCreationRules.js'

export default function AddPortfolioProjectPage() {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [authorized, setAuthorized] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [categories, setCategories] = useState([])
  const [images, setImages] = useState([])
  const [previews, setPreviews] = useState([])
  const [error, setError] = useState('')
  const [dirty, setDirty] = useState(false)

  useEffect(() => {
    let active = true
    async function initialize() {
      try {
        const designer = await currentDesigner()
        if (!active) return
        if (designer.passwordChangeRequired) {
          navigate('/designer/change-password', { replace: true })
          return
        }
        await currentDesignerArea()
        const entries = await listCategories()
        if (!active) return
        setCategories(entries)
        setAuthorized(true)
      } catch (failure) {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to load the project form. Please try again.')
      } finally {
        if (active) setLoading(false)
      }
    }
    initialize()
    return () => { active = false }
  }, [navigate])

  useEffect(() => {
    const next = images.map((file) => ({ name: file.name, url: URL.createObjectURL(file) }))
    setPreviews(next)
    return () => next.forEach((preview) => URL.revokeObjectURL(preview.url))
  }, [images])

  function onImagesSelected(event) {
    const chosen = Array.from(event.target.files ?? [])
    setImages(chosen)
    setError(validateProjectImages(chosen))
  }

  async function onSubmit(values) {
    if (submitting) return
    const validationError = validateProjectImages(images)
    if (validationError) {
      setError(validationError)
      return
    }
    setSubmitting(true)
    setError('')
    try {
      const created = await addPortfolioProject(buildProjectDetails(values), images)
      navigate('/designer/portfolio', { replace: true, state: { createdProjectName: created.title } })
    } catch (failure) {
      const fields = failure.response?.data?.fieldErrors
      if (fields) form.setFields(Object.entries(fields).map(([name, message]) => ({ name, errors: [message] })))
      setError(failure.message === FLOOR_AREA_ERROR ? failure.message : projectCreationFailureMessage(failure))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="portfolio-page">
      <DesignerReturnButton to="/designer/portfolio" confirmLeave={dirty || images.length > 0}>Back to portfolio</DesignerReturnButton>
      <p className="portfolio-eyebrow">Designer workspace</p>
      <h1>Add project</h1>
      <p className="portfolio-intro">Showcase a current or completed interior design project in your portfolio.</p>
      {loading && <div className="loading-state"><Spin /> <span>Loading project form…</span></div>}
      {error && <Alert role="alert" type="error" showIcon message={error} className="form-feedback" />}
      {authorized && !loading && <Card className="portfolio-panel" title="Project details">
        {categories.length === 0 && <Alert type="warning" showIcon className="form-feedback"
          message="Category classification pending setup. You can still create projects." />}
        <Form form={form} layout="vertical" disabled={submitting} onFinish={onSubmit} onValuesChange={() => setDirty(true)}
          initialValues={{ status: 'CURRENT' }}>
          <Form.Item name="title" label="Project title" rules={[
            { required: true, whitespace: true, message: 'Enter a project title.' },
            { max: 160, message: 'Use 160 characters or fewer.' },
          ]}>
            <Input placeholder="e.g. Contemporary living room renovation" maxLength={160} />
          </Form.Item>
          <Form.Item name="description" label="Description" rules={[
            { required: true, whitespace: true, message: 'Enter a project description.' },
            { max: 3000, message: 'Use 3000 characters or fewer.' },
          ]}>
            <Input.TextArea rows={5} showCount maxLength={3000}
              placeholder="Describe the design, renovation scope and key features" />
          </Form.Item>
          <Form.Item name="categoryId" label="Renovation category" rules={categories.length ? [{ required: true, message: 'Choose a project category.' }] : []}>
            <Select disabled={!categories.length} placeholder={categories.length ? "Select category" : "Pending setup"} options={categories.map((item) => ({
              value: item.id, label: item.name,
            }))} />
          </Form.Item>
          <ProjectMetadataFields form={form} />
          <Form.Item name="status" label="Project status" rules={[{ required: true }]}>
            <Radio.Group options={[{ label: 'Current', value: 'CURRENT' },
              { label: 'Completed', value: 'COMPLETED' }]} />
          </Form.Item>
          <div className="portfolio-upload">
            <label htmlFor="portfolio-photos">Project photos (optional)</label>
            <p>Up to 6 JPG, PNG or WebP photos. Maximum 5 MB each.</p>
            <input id="portfolio-photos" type="file" accept="image/jpeg,image/png,image/webp" multiple
              disabled={submitting} onChange={onImagesSelected} />
            {previews.length > 0 && <div className="portfolio-previews">
              {previews.map((preview) => <div key={preview.url}>
                <img src={preview.url} alt={`Preview: ${preview.name}`} />
                <span>{preview.name}</span>
              </div>)}
            </div>}
          </div>
          <div className="portfolio-form-actions">
              <Button type="primary" htmlType="submit" loading={submitting} >
              Add project
            </Button>
          </div>
        </Form>
      </Card>}
    </div>
  )
}
