import DesignerReturnButton from '../components/DesignerReturnButton.jsx'
import { Alert, Button, Card, Form, Input, Radio, Select, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { listCategories } from '../api/categories.js'
import { currentDesigner, currentDesignerArea } from '../api/designerAuthentication.js'
import { getMyPortfolio, updatePortfolioProject } from '../api/portfolio.js'
import ProjectMetadataFields from '../components/ProjectMetadataFields.jsx'
import { getProjectMetadata } from '../api/discovery.js'
import { FLOOR_AREA_ERROR } from '../designer/floorAreaRules.js'
import '../ui/designerV26.css'
import { buildUpdatedProjectDetails, projectEditingFailureMessage,
  validateEditedImages } from '../designer/projectEditingRules.js'

export default function EditPortfolioProjectPage() {
  const { projectId } = useParams()
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [project, setProject] = useState(null)
  const [metadata, setMetadata] = useState(null)
  const [categories, setCategories] = useState([])
  const [keptImages, setKeptImages] = useState([])
  const [newImages, setNewImages] = useState([])
  const [previews, setPreviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
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
        const [entries, availableCategories] = await Promise.all([getMyPortfolio(), listCategories()])
        if (!active) return
        const chosen = entries.find((item) => String(item.id) === projectId)
        if (!chosen) { setError('This project is not available in your portfolio.'); return }
        // Never expose an editable form with blank classification values when metadata fails to load.
        const loadedMetadata = await getProjectMetadata(chosen.id, true)
        if (!active) return
        setMetadata(loadedMetadata)
        setProject(chosen)
        setKeptImages(chosen.images ?? [])
        setCategories(availableCategories)
        form.setFieldsValue({ title: chosen.title, description: chosen.description,
          categoryId: chosen.categoryId, status: chosen.status })
      } catch (failure) {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to load this project or its classifications. No changes were made. Please refresh and try again.')
      } finally {
        if (active) setLoading(false)
      }
    }
    initialize()
    return () => { active = false }
  }, [projectId, navigate, form])

  useEffect(() => {
    const next = newImages.map((file) => ({ name: file.name, url: URL.createObjectURL(file) }))
    setPreviews(next)
    return () => next.forEach((item) => URL.revokeObjectURL(item.url))
  }, [newImages])

  function onNewImagesSelected(event) {
    const added = Array.from(event.target.files ?? [])
    const next = [...newImages, ...added]
    const problem = validateEditedImages(keptImages.map((image) => image.id), next)
    if (problem) { setError(problem); event.target.value = ''; return }
    setNewImages(next)
    setError('')
    event.target.value = ''
  }

  function removeExisting(imageId) {
    setKeptImages((current) => current.filter((image) => image.id !== imageId))
    setError('')
  }

  async function onSubmit(values) {
    if (submitting || !project) return
    const ids = keptImages.map((image) => image.id)
    const problem = validateEditedImages(ids, newImages)
    if (problem) { setError(problem); return }
    setSubmitting(true)
    setError('')
    try {
      const saved = await updatePortfolioProject(project.id,
        buildUpdatedProjectDetails(values, ids), newImages)
      navigate('/designer/portfolio', { replace: true, state: { editedProjectName: saved.title } })
    } catch (failure) {
      const fields = failure.response?.data?.fieldErrors
      if (fields) form.setFields(Object.entries(fields).map(([name, message]) => ({ name, errors: [message] })))
      setError(failure.message === FLOOR_AREA_ERROR ? failure.message : projectEditingFailureMessage(failure))
    } finally { setSubmitting(false) }
  }

  return <div className="portfolio-page">
    <DesignerReturnButton to="/designer/portfolio" confirmLeave={dirty || newImages.length > 0 || keptImages.length !== (project?.images?.length ?? 0)}>Back to portfolio</DesignerReturnButton>
    <p className="portfolio-eyebrow">Designer workspace</p>
    <h1>Edit project</h1>
    <p className="portfolio-intro">Update this project in your portfolio.</p>
    {loading && <div className="loading-state"><Spin /> <span>Loading project…</span></div>}
    {error && <Alert role="alert" type="error" showIcon message={error} className="form-feedback" />}
    {project && !loading && <Card className="portfolio-panel" title="Project details">
      <Form form={form} layout="vertical" disabled={submitting} onFinish={onSubmit} onValuesChange={() => setDirty(true)}>
        <Form.Item name="title" label="Project title" rules={[
          { required: true, whitespace: true, message: 'Enter a project title.' },
          { max: 160, message: 'Use 160 characters or fewer.' },
        ]}><Input maxLength={160} /></Form.Item>
        <Form.Item name="description" label="Description" rules={[
          { required: true, whitespace: true, message: 'Enter a project description.' },
          { max: 3000, message: 'Use 3000 characters or fewer.' },
        ]}><Input.TextArea rows={5} maxLength={3000} showCount /></Form.Item>
        <Form.Item name="categoryId" label="Renovation category">
          <Select disabled={!categories.length} placeholder={categories.length ? "Select category" : "Pending setup"} options={categories.map((item) => ({ value: item.id, label: item.name }))} />
        </Form.Item>
        <ProjectMetadataFields form={form} initial={metadata} />
        <Form.Item name="status" label="Project status" rules={[{ required: true }]}>
          <Radio.Group options={[{ label: 'Current', value: 'CURRENT' },
            { label: 'Completed', value: 'COMPLETED' }]} />
        </Form.Item>
        <div className="portfolio-upload">
          <label>Existing photos</label>
          <p>Remove individual photos if they should no longer appear on the project.</p>
          {keptImages.length === 0 && <p>No existing photos retained.</p>}
          <div className="portfolio-existing-photos">
            {keptImages.map((image) => <div key={image.id} className="portfolio-existing-photo">
              <img src={image.url} alt={`Existing photo for ${project.title}`} />
              <Button danger size="small" onClick={() => removeExisting(image.id)}>Remove photo</Button>
            </div>)}
          </div>
        </div>
        <div className="portfolio-upload">
          <label htmlFor="edit-portfolio-photos">Add photos (optional)</label>
          <p>Up to 6 total JPG, PNG or WebP photos, maximum 5 MB per file.</p>
          <input id="edit-portfolio-photos" type="file" multiple accept="image/jpeg,image/png,image/webp"
            disabled={submitting} onChange={onNewImagesSelected} />
          {previews.length > 0 && <div className="portfolio-previews">
            {previews.map((preview, index) => <div key={preview.url}>
              <img src={preview.url} alt={`New photo preview: ${preview.name}`} />
              <span>{preview.name}</span>
              <Button size="small" onClick={() => setNewImages((files) => files.filter((_, i) => i !== index))}>
                Remove
              </Button>
            </div>)}
          </div>}
        </div>
        <div className="portfolio-form-actions">
          <Button type="primary" htmlType="submit" loading={submitting}>Save changes</Button>
        </div>
      </Form>
    </Card>}
  </div>
}
