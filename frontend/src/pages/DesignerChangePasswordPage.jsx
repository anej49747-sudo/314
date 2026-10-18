import { Alert, Button, Form, Input, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { changeDesignerPassword, currentDesigner } from '../api/designerAuthentication.js'
import AuthCard from '../components/AuthCard.jsx'
import { buildPasswordChangePayload, passwordChangeFailureMessage, validatePasswordChange } from '../designer/designerLoginRules.js'

export default function DesignerChangePasswordPage() {
  const navigate = useNavigate()
  const [checking, setChecking] = useState(true)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let active = true
    currentDesigner().then((account) => {
      if (active && !account.passwordChangeRequired) navigate('/designer', { replace: true })
    }).catch((failure) => {
      if (!active) return
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setError('Unable to verify your session. Please try again.')
    }).finally(() => { if (active) setChecking(false) })
    return () => { active = false }
  }, [navigate])

  async function onSubmit(values) {
    if (submitting) return
    const invalid = validatePasswordChange(values)
    if (invalid) { setError(invalid); return }
    setSubmitting(true)
    setError('')
    try {
      await changeDesignerPassword(buildPasswordChangePayload(values))
      navigate('/designer', { replace: true, state: { passwordChanged: true } })
    } catch (failure) {
      setError(passwordChangeFailureMessage(failure))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard title="Set your new password"
      description="For your security, choose a new password before accessing designer features.">
      {checking && !error && <div className="loading-state"><Spin /> <span>Checking your account…</span></div>}
      {error && <Alert className="form-feedback" type="error" showIcon role="alert" message={error} />}
      {!checking && <Form layout="vertical" onFinish={onSubmit} disabled={submitting} autoComplete="off">
        <Form.Item name="newPassword" label="New password" rules={[{ required: true, message: 'Enter a new password' },
          { validator: (_, value) => {
            const message = value ? validatePasswordChange({ newPassword: value, confirmPassword: value }) : null
            return message ? Promise.reject(new Error(message)) : Promise.resolve()
          } }]}>
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <p className="form-helper">10–128 characters with uppercase, lowercase, number and special character.</p>
        <Form.Item name="confirmPassword" label="Confirm new password"
          dependencies={['newPassword']} rules={[{ required: true, message: 'Confirm your new password' },
            ({ getFieldValue }) => ({ validator(_, value) {
              return value === getFieldValue('newPassword') ? Promise.resolve() : Promise.reject(new Error('Passwords do not match'))
            } })]}>
          <Input.Password autoComplete="new-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={submitting} className="submit-button">Set password</Button>
      </Form>}
    </AuthCard>
  )
}
