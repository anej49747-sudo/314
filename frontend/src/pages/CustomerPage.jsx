import { Alert, Button, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { currentCustomer, logoutCustomer } from '../api/authentication.js'
import { logoutFailureMessage } from '../auth/logoutRules.js'
import AuthCard from '../components/AuthCard.jsx'

export default function CustomerPage() {
  const [account, setAccount] = useState(null)
  const [error, setError] = useState('')
  const [loggingOut, setLoggingOut] = useState(false)
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

  async function onLogout() {
    if (loggingOut) return
    setLoggingOut(true)
    setError('')
    try {
      await logoutCustomer()
      navigate('/login', { replace: true, state: { justLoggedOut: true } })
    } catch (failure) {
      setError(logoutFailureMessage(failure))
      setLoggingOut(false)
    }
  }

  return (
    <AuthCard title={account ? `Welcome, ${account.displayName}` : 'Customer area'}
      description="Your private customer area will grow as we implement more user stories.">
      {error && <Alert className="form-feedback" type="error" message={error} showIcon role="alert" />}
      {!account && !error && <div className="loading-state"><Spin /> <span>Checking your session…</span></div>}
      {account && (
        <>
          <p className="customer-account">Signed in as <strong>{account.email}</strong>.</p>
          <Button type="primary" block className="submit-button" onClick={() => navigate('/customer/account')}>
            My account
          </Button>
          <p><Link to="/customer/favourites">View My Favourites →</Link></p>
          <Button type="primary" block loading={loggingOut} disabled={loggingOut}
            onClick={onLogout} className="submit-button">
            Log out
          </Button>
        </>
      )}
      <p className="auth-footer"><Link to="/register">Registration</Link></p>
    </AuthCard>
  )
}
