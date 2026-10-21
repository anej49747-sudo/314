import { Alert, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { loginCustomer } from '../api/authentication.js'
import { buildLoginPayload, loginFailureMessage } from '../auth/loginRules.js'
import AuthCard from '../components/AuthCard.jsx'
import { designerLoginDestination } from '../designer/designerLoginRules.js'
import { customerLoginDestination } from '../project/favouriteRules.js'

export default function LoginPage() {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const location = useLocation()
  const navigate = useNavigate()

  async function onSubmit(values) {
    if (submitting) return
    setSubmitting(true)
    setError('')
    try {
      const account = await loginCustomer(buildLoginPayload(values))
      if (account.role === 'CUSTOMER') {
        navigate(customerLoginDestination(location.state), { replace: true })
      } else if (account.role === 'USER_ADMIN') {
        navigate('/admin/designers/new', { replace: true })
      } else if (account.role === 'INTERIOR_DESIGNER') {
        navigate(designerLoginDestination(account), { replace: true })
      } else if (account.role === 'PLATFORM_MANAGER') {
        navigate('/manager/categories', { replace: true })
      } else {
        setError('This account does not have access to this area.')
      }
    } catch (failure) {
      setError(loginFailureMessage(failure))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard title="Log in" description="Sign in with your account email and password.">
      {location.state?.justRegistered && (
        <Alert className="form-feedback" type="success" showIcon role="status"
          message="Account created successfully. You can now log in." />
      )}
      {location.state?.accountEmailChanged && (
        <Alert className="form-feedback" type="success" showIcon role="status"
          message="Email address updated. Please sign in again using your new email address." />
      )}
      {location.state?.justLoggedOut && (
        <Alert className="form-feedback" type="success" showIcon role="status"
          message="You have logged out successfully." />
      )}
      {error && <Alert className="form-feedback" type="error" showIcon role="alert" message={error} />}
      <Form layout="vertical" onFinish={onSubmit} autoComplete="on" disabled={submitting}>
        <Form.Item name="email" label="Email" rules={[
          { required: true, message: 'Email is required' },
          { type: 'email', message: 'Enter a valid email address' },
        ]}>
          <Input autoComplete="email" inputMode="email" maxLength={254} placeholder="name@example.com" />
        </Form.Item>
        <Form.Item name="password" label="Password" rules={[
          { required: true, message: 'Password is required' },
        ]}>
          <Input.Password autoComplete="current-password" placeholder="Enter your password" />
        </Form.Item>
        <Button htmlType="submit" type="primary" block loading={submitting} className="submit-button">
          Log in
        </Button>
      </Form>
      <p className="auth-footer">New to the platform? <Link to="/register">Create an account</Link></p>
    </AuthCard>
  )
}
