import { Alert, Button, Form, Input, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createDesigner, currentAdmin } from '../api/designerAccounts.js'
import { logoutCustomer } from '../api/authentication.js'
import AccountIdentityFields from '../components/AccountIdentityFields.jsx'
import AuthCard from '../components/AuthCard.jsx'
import { buildDesignerPayload, designerCreationFailureMessage } from '../designer/designerAccountRules.js'

export default function CreateDesignerPage() {
  const [form] = Form.useForm()
  const [authorized, setAuthorized] = useState(false)
  const [checking, setChecking] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const [error, setError] = useState('')
  const [created, setCreated] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    let active = true
    currentAdmin().then(() => { if (active) setAuthorized(true) }).catch((failure) => {
      if (!active) return
      if (failure.response?.status === 401 || failure.response?.status === 403) {
        navigate('/login', { replace: true })
      } else {
        setError('Unable to verify administrator access. Please try again.')
      }
    }).finally(() => { if (active) setChecking(false) })
    return () => { active = false }
  }, [navigate])

  async function onSubmit(values) {
    if (submitting) return
    setSubmitting(true)
    setError('')
    setCreated(null)
    try {
      const result = await createDesigner(buildDesignerPayload(values))
      setCreated(result)
      form.resetFields()
    } catch (failure) {
      const fields = failure.response?.data?.fieldErrors
      if (fields && typeof fields === 'object') {
        form.setFields(Object.entries(fields).map(([name, message]) => ({ name, errors: [message] })))
      }
      setError(designerCreationFailureMessage(failure))
    } finally {
      setSubmitting(false)
    }
  }

  async function onLogout() {
    if (loggingOut) return
    setLoggingOut(true)
    setError('')
    try {
      await logoutCustomer()
      navigate('/login', { replace: true, state: { justLoggedOut: true } })
    } catch {
      setError('Unable to log out. Please try again.')
      setLoggingOut(false)
    }
  }

  return (
    <AuthCard title="Create designer account" description="For approved Interior Designers only. The password is generated securely and displayed once.">
      {checking && <div className="loading-state"><Spin /> <span>Verifying administrator access…</span></div>}
      {error && <Alert type="error" showIcon message={error} role="alert" className="form-feedback" />}
      {authorized && <>
        {created && <Alert type="success" showIcon className="form-feedback" message="Designer account created"
          description={<div className="one-time-password">
            <p><strong>{created.displayName}</strong> ({created.email})</p>
            <p>Copy this initial password now. It cannot be displayed again after you leave or create another account. Share it securely with the approved designer.</p>
            <Input.Password readOnly aria-label="One-time designer password"
              autoComplete="off" value={created.initialPassword} />
          </div>} />}
        {created ? (
          <Button block onClick={() => setCreated(null)}>
            I've saved the password — create another
          </Button>
        ) : (
          <Form form={form} layout="vertical" onFinish={onSubmit} disabled={submitting} autoComplete="off">
            <AccountIdentityFields includePassword={false} />
            <Button htmlType="submit" type="primary" block loading={submitting} className="submit-button">
              Create designer account
            </Button>
          </Form>
        )}
        <p className="auth-footer"><Button type="link" loading={loggingOut} onClick={onLogout}>Log out</Button></p>
      </>}
      {!checking && !authorized && <p className="auth-footer"><Link to="/login">Back to login</Link></p>}
    </AuthCard>
  )
}
