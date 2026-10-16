import { Alert, Button, Form } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registerCustomer } from '../api/registration.js'
import AuthCard from '../components/AuthCard.jsx'
import AccountIdentityFields from '../components/AccountIdentityFields.jsx'
import { buildRegistrationPayload } from '../registration/registrationRules.js'

export default function RegisterPage() {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function onSubmit(values) {
    if (submitting) return
    setSubmitting(true)
    setError('')
    try {
      await registerCustomer(buildRegistrationPayload(values))
      navigate('/login', { replace: true, state: { justRegistered: true } })
    } catch (failure) {
      const fieldErrors = failure.response?.data?.fieldErrors
      if (fieldErrors && typeof fieldErrors === 'object') {
        form.setFields(Object.entries(fieldErrors).map(([name, message]) => ({ name, errors: [message] })))
      }
      if (failure.response?.status === 409) {
        setError('An account with this email already exists. Please log in or use another email.')
      } else if (failure.response?.status === 400) {
        setError('Please correct the highlighted fields and try again.')
      } else {
        setError('Unable to create your account right now. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard title="Create your account" description="Register as a Renovation Customer to get started.">
      {error && <Alert type="error" showIcon message={error} role="alert" className="form-feedback" />}
      <Form
        form={form}
        layout="vertical"
        onFinish={onSubmit}
        autoComplete="on"
        disabled={submitting}
      >
        <AccountIdentityFields />
        <Button htmlType="submit" type="primary" block loading={submitting} className="submit-button">
          Create account
        </Button>
      </Form>
      <p className="auth-footer">Already have an account? <Link to="/login">Log in</Link></p>
    </AuthCard>
  )
}
