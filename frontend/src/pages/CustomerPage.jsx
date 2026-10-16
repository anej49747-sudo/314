import { Alert, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import AuthCard from '../components/AuthCard.jsx'

export default function CustomerPage() {
  const [account, setAccount] = useState(null)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    let active = true
    currentCustomer().then((profile) => {
      if (active) setAccount(profile)
    }).catch((failure) => {
      if (!active) return
      if (failure.response?.status === 401 || failure.response?.status === 403) {
        navigate('/login', { replace: true })
      } else {
        setError('Unable to load your account. Please try again.')
      }
    })
    return () => { active = false }
  }, [navigate])

  return (
    <AuthCard title={account ? `Welcome, ${account.displayName}` : 'Customer area'}
      description="Your private customer area will grow as we implement more user stories.">
      {error && <Alert className="form-feedback" type="error" message={error} showIcon role="alert" />}
      {!account && !error && <div className="loading-state"><Spin /> <span>Checking your session…</span></div>}
      {account && <p className="customer-account">Signed in as <strong>{account.email}</strong>.</p>}
      <p className="auth-footer">Secure sign-out will be added in Story #19. <Link to="/register">Registration</Link></p>
    </AuthCard>
  )
}
