import { Alert, Button, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { currentDesigner, currentDesignerArea } from '../api/designerAuthentication.js'
import { logoutSession } from '../api/authentication.js'
import { logoutFailureMessage } from '../auth/logoutRules.js'
import AuthCard from '../components/AuthCard.jsx'

export default function DesignerPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [account, setAccount] = useState(null)
  const [error, setError] = useState('')
  const [loggingOut, setLoggingOut] = useState(false)

  useEffect(() => {
    let active = true
    currentDesigner().then(async (profile) => {
      if (!active) return
      if (profile.passwordChangeRequired) {
        navigate('/designer/change-password', { replace: true })
        return
      }
      const allowed = await currentDesignerArea()
      if (active) setAccount(allowed)
    }).catch((failure) => {
      if (!active) return
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setError('Unable to load the designer area. Please try again.')
    })
    return () => { active = false }
  }, [navigate])

  async function onLogout() {
    if (loggingOut) return
    setLoggingOut(true)
    setError('')
    try {
      await logoutSession()
      navigate('/login', { replace: true, state: { justLoggedOut: true } })
    } catch (failure) {
      setError(logoutFailureMessage(failure))
      setLoggingOut(false)
    }
  }

  return (
    <AuthCard title={account ? `Welcome, ${account.displayName}` : 'Designer area'}
      description="Your designer workspace will grow as we implement company and portfolio management.">
      {location.state?.passwordChanged && <Alert className="form-feedback" type="success" showIcon role="status" message="Password changed successfully." />}
      {error && <Alert className="form-feedback" type="error" showIcon role="alert" message={error} />}
      {!account && !error && <div className="loading-state"><Spin /> <span>Checking your account…</span></div>}
      {account && <>
        <p className="customer-account">Signed in as <strong>{account.email}</strong>.</p>
        <Button type="primary" block className="submit-button" onClick={() => navigate('/designer/company-information')}>
          Manage company information
        </Button>
        <Button block className="submit-button" loading={loggingOut} onClick={onLogout}>Log out</Button>
      </>}
    </AuthCard>
  )
}
