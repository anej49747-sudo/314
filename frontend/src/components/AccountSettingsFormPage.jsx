import { Alert, Button, Card, Form, Input, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { buildAccountSettingsPayload, accountSettingsChanged,
  accountSettingsFailureMessage } from '../account/accountSettingsRules.js'
import '../designer/designerAccountSettings.css'

/** Shared Story #14/#21 visual form; role checks are also enforced by the backend. */
export default function AccountSettingsFormPage({
  backPath, backText, eyebrow, roleLabel, intro, description, advisory,
  footer, afterContent, currentAccount, loadAccount, updateAccount, requirePasswordChange = false,
}) {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [initial, setInitial] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [saving, setSaving] = useState(false)
  const [changed, setChanged] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    async function initialize() {
      setLoading(true)
      setLoadError('')
      try {
        const account = await currentAccount()
        if (!active) return
        if (requirePasswordChange && account.passwordChangeRequired) {
          navigate('/designer/change-password', { replace: true })
          return
        }
        const details = await loadAccount()
        if (!active) return
        form.setFieldsValue({ displayName: details.displayName, email: details.email })
        setInitial(details)
        setChanged(false)
      } catch (failure) {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setLoadError('Unable to load account details. Please try again.')
      } finally {
        if (active) setLoading(false)
      }
    }
    initialize()
    return () => { active = false }
  }, [form, navigate, reloadKey, currentAccount, loadAccount, requirePasswordChange])

  async function onSave(values) {
    if (saving || !changed) return
    setSaving(true)
    setError('')
    setSuccess('')
    try {
      const saved = await updateAccount(buildAccountSettingsPayload(values))
      if (saved.signInRequired) {
        navigate('/login', { replace: true, state: { accountEmailChanged: true } })
        return
      }
      const details = { displayName: saved.displayName, email: saved.email }
      form.setFieldsValue(details)
      setInitial(details)
      setChanged(false)
      setSuccess('Account details saved successfully.')
    } catch (failure) {
      const fieldErrors = failure.response?.data?.fieldErrors
      if (fieldErrors) {
        form.setFields(Object.entries(fieldErrors).map(([name, message]) => ({ name, errors: [message] })))
      }
      setError(accountSettingsFailureMessage(failure))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="designer-settings-page">
      <Link className="designer-settings-back" to={backPath}>← {backText}</Link>
      <p className="company-eyebrow">{eyebrow}</p>
      <div className="designer-settings-heading">
        <h1>My account</h1>
        <span className="designer-settings-role">{roleLabel}</span>
      </div>
      <p className="company-intro">{intro}</p>
      {loadError && <Alert type="error" showIcon role="alert" className="form-feedback"
        message={loadError} action={<Button size="small" onClick={() => setReloadKey((key) => key + 1)}>Retry</Button>} />}
      {error && <Alert type="error" showIcon role="alert" className="form-feedback" message={error} />}
      {success && <Alert type="success" showIcon role="status" className="form-feedback" message={success} />}
      <Card className="designer-settings-panel" title="Account details">
        <p className="designer-settings-card-description">{description}</p>
        {loading && <div className="loading-state" role="status"><Spin /> <span>Loading account details…</span></div>}
        {!loading && initial && <Form form={form} layout="vertical" disabled={saving} onFinish={onSave}
          onValuesChange={(_, values) => { setChanged(accountSettingsChanged(initial, values)); setSuccess('') }}>
          <Form.Item name="displayName" label="Display name" rules={[
            { required: true, whitespace: true, message: 'Enter your display name.' },
            { max: 160, message: 'Display name must be 160 characters or fewer.' },
          ]} extra="Shown in your account area. Does not change other profile information.">
            <Input maxLength={160} autoComplete="name" placeholder="Enter your display name" />
          </Form.Item>
          <Form.Item name="email" label="Email address" rules={[
            { required: true, whitespace: true, message: 'Enter your email address.' },
            { type: 'email', message: 'Enter a valid email address.' },
            { max: 254, message: 'Email must be 254 characters or fewer.' },
          ]} extra="Used to sign in. If changed, you'll need to sign in again with the new address.">
            <Input maxLength={254} autoComplete="email" inputMode="email" placeholder="name@example.com" />
          </Form.Item>
          <div className="designer-settings-advisory">
            <strong>Only your account details change here.</strong> {advisory}
          </div>
          <div className="designer-settings-actions">
            <span className="designer-settings-hint">{changed ? 'Unsaved changes' : 'Your details are up to date'}</span>
            <Button onClick={() => navigate(backPath)}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={saving} disabled={!changed}>Save changes</Button>
          </div>
        </Form>}
      </Card>
      {!loading && initial && afterContent}
      <div className="designer-settings-footer">{footer}</div>
    </div>
  )
}
