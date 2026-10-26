import { Alert, Spin } from 'antd'
import { KeyRound, Mail, UserRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import { currentDesigner } from '../api/designerAuthentication.js'
import { accountAreaPath } from '../auth/headerSessionRules.js'
import { ActionCard } from '../components/VisualCards.jsx'

/** Dedicated account hub. Password/email changes are never mixed into the same form. */
export default function AccountSecurityPage({ role }) {
  const navigate = useNavigate(), location = useLocation()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const customer = role === 'customer'
  useEffect(() => {
    let live = true
    const current = customer ? currentCustomer : currentDesigner
    current().then(profile => {
      if (!live) return
      if (profile.role !== (customer ? 'CUSTOMER' : 'INTERIOR_DESIGNER')) {
        navigate(accountAreaPath(profile) || '/login', {replace:true});return
      }
      if (!customer && profile.passwordChangeRequired) navigate('/designer/change-password',{replace:true})
    }).catch(err => {
      if (!live) return
      if ([401,403].includes(err.response?.status)) navigate('/login',{replace:true})
      else setError('Unable to open account security.')
    }).finally(() => { if (live) setLoading(false) })
    return () => {live=false}
  },[navigate,customer])
  return <section className="ui-dashboard ld-account-security" aria-labelledby="security-heading">
    <p className="ui-eyebrow">{customer ? 'My space' : 'Designer workspace'}</p>
    <h1 id="security-heading">Account security</h1>
    <p className="ld-security-intro">Manage your profile and sign-in details. Email and password each have a dedicated page.</p>
    {location.state?.passwordChanged && <Alert showIcon type="success" role="status" message="Password updated successfully." className="form-feedback"/>}
    {loading && <div className="loading-state"><Spin/> Verifying account…</div>}
    {error && <Alert type="error" showIcon message={error} />}
    {!loading && !error && <div className="ui-shortcuts ld-security-actions">
      <ActionCard to={`/${role}/account`} title="Display name" description="Update how your name appears" Icon={UserRound} />
      <ActionCard to={`/${role}/change-email`} title="Change email" description="Update your sign-in address" Icon={Mail} />
      <ActionCard to={`/${role}/change-password`} title="Change password" description="Verify your current password first" Icon={KeyRound} />
    </div>}
  </section>
}
