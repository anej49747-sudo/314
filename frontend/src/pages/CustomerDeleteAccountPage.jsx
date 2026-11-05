import { Alert, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import CustomerDeleteAccountSection from '../components/CustomerDeleteAccountSection.jsx'
import CustomerReturnButton from '../components/CustomerReturnButton.jsx'

/** Separate destructive action; name editing cannot accidentally expose it. */
export default function CustomerDeleteAccountPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [verified, setVerified] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    currentCustomer().then((account) => {
      if (!active) return
      if (account.role !== 'CUSTOMER') navigate('/login', { replace: true })
      else setVerified(true)
    }).catch((failure) => {
      if (!active) return
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setError('Unable to verify your customer account. Please try again.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [navigate])

  return <section className="ui-dashboard ld-delete-account-page" aria-label="Delete customer account">
    <CustomerReturnButton to="/customer/security">Back to account security</CustomerReturnButton>
    {loading && <div className="loading-state" role="status"><Spin /> Verifying account…</div>}
    {error && <Alert type="error" showIcon role="alert" message={error} />}
    {!loading && verified && !error && <CustomerDeleteAccountSection />}
  </section>
}
