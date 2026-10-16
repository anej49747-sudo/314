import { Alert } from 'antd'
import { Link, useLocation } from 'react-router-dom'
import AuthCard from '../components/AuthCard.jsx'

export default function LoginPage() {
  const location = useLocation()
  return (
    <AuthCard title="Log in" description="Customer sign-in is coming in Story #18.">
      {location.state?.justRegistered && (
        <Alert className="form-feedback" type="success" showIcon role="status"
          message="Your account was created successfully. You can log in once sign-in is available." />
      )}
      <p className="placeholder-description">
        Authentication is not available yet. Registration is enabled, but this page does not issue sessions or tokens.
      </p>
      <p className="auth-footer">New to the platform? <Link to="/register">Create an account</Link></p>
    </AuthCard>
  )
}
