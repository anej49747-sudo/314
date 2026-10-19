import { Alert, Button, Card, Form, Input, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCompanyInformation, saveCompanyInformation } from '../api/companyInformation.js'
import { currentDesigner, currentDesignerArea } from '../api/designerAuthentication.js'
import { buildCompanyInformationPayload, companyInformationFailureMessage } from '../designer/companyInformationRules.js'

export default function CompanyInformationPage() {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [authorized, setAuthorized] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [unsaved, setUnsaved] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

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
        const company = await getCompanyInformation()
        if (!active) return
        form.setFieldsValue(company)
        setAuthorized(true)
      } catch (failure) {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to load company information. Please try again.')
      } finally {
        if (active) setLoading(false)
      }
    }
    initialize()
    return () => { active = false }
  }, [form, navigate])

  async function onSubmit(values) {
    if (submitting) return
    setSubmitting(true)
    setError('')
    setSuccess('')
    try {
      const saved = await saveCompanyInformation(buildCompanyInformationPayload(values))
      form.setFieldsValue(saved)
      setUnsaved(false)
      setSuccess('Company information saved successfully.')
    } catch (failure) {
      const fields = failure.response?.data?.fieldErrors
      if (fields) {
        form.setFields(Object.entries(fields).map(([name, message]) => ({ name, errors: [message] })))
      }
      setError(companyInformationFailureMessage(failure))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="company-page">
      <p className="company-eyebrow">Designer workspace</p>
      <h1>Company information</h1>
      <p className="company-intro">Manage the company details customers will see on your profile.</p>
      {loading && <div className="loading-state"><Spin /> <span>Loading company information…</span></div>}
      {error && <Alert type="error" showIcon role="alert" message={error} className="form-feedback" />}
      {success && <Alert type="success" showIcon role="status" message={success} className="form-feedback" />}
      {authorized && !loading && (
        <Card className="company-panel" title="Company details">
          <Form form={form} layout="vertical" onFinish={onSubmit} disabled={submitting}
            onValuesChange={() => { setUnsaved(true); setSuccess('') }}>
            <Form.Item name="companyName" label="Company name" rules={[
              { required: true, whitespace: true, message: 'Enter a company name.' },
              { max: 150, message: 'Company name must be 150 characters or fewer.' },
            ]} extra="Required. This name will appear on your company profile.">
              <Input maxLength={150} placeholder="Enter your company name" autoComplete="organization" />
            </Form.Item>
            <Form.Item name="address" label="Company address (optional)" rules={[
              { max: 300, message: 'Company address must be 300 characters or fewer.' },
            ]}>
              <Input.TextArea rows={2} maxLength={300} placeholder="Enter your business address" />
            </Form.Item>
            <Form.Item name="description" label="Company description (optional)" rules={[
              { max: 3000, message: 'Company description must be 3000 characters or fewer.' },
            ]} extra="Briefly describe your services and design approach.">
              <Input.TextArea rows={5} maxLength={3000} showCount placeholder="Introduce your company" />
            </Form.Item>
            <div className="company-actions">
              {unsaved && <span className="company-unsaved">Unsaved changes</span>}
              <Button disabled={submitting} onClick={() => navigate('/designer')}>Cancel</Button>
              <Button type="primary" htmlType="submit" loading={submitting}>Save changes</Button>
            </div>
          </Form>
        </Card>
      )}
    </div>
  )
}
