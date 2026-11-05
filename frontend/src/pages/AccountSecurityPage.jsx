import DesignerReturnButton from '../components/DesignerReturnButton.jsx'
import { Alert, Spin } from 'antd'
import { KeyRound, Mail, Trash2, UserRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import { currentDesigner } from '../api/designerAuthentication.js'
import { accountAreaPath } from '../auth/headerSessionRules.js'
import CustomerReturnButton from '../components/CustomerReturnButton.jsx'

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
    {customer ? <CustomerReturnButton /> : <DesignerReturnButton />}
    <p className="ui-eyebrow">{customer ? 'My dashboard' : 'Designer workspace'}</p>
    <h1 id="security-heading">Account security</h1>
    {location.state?.passwordChanged && <Alert showIcon type="success" role="status" message="Password updated successfully." className="form-feedback"/>}
    {loading && <div className="loading-state"><Spin/> Verifying account…</div>}
    {error && <Alert type="error" showIcon message={error} />}
    {!loading && !error && (customer ? <section className="ld-customer-security-hub" aria-label="Account settings">
      <div className="ld-customer-security-options">
        <Link to="/customer/account"><UserRound size={22} aria-hidden="true"/><span><strong>Change name</strong></span><span aria-hidden="true">→</span></Link>
        <Link to="/customer/change-email"><Mail size={22} aria-hidden="true"/><span><strong>Change email</strong></span><span aria-hidden="true">→</span></Link>
        <Link to="/customer/change-password"><KeyRound size={22} aria-hidden="true"/><span><strong>Change password</strong></span><span aria-hidden="true">→</span></Link>
        <Link className="ld-delete-account-option" to="/customer/delete-account"><Trash2 size={22} aria-hidden="true"/><span><strong>Delete account</strong></span><span aria-hidden="true">→</span></Link>
      </div>
    </section> : <section className="ld-customer-security-hub" aria-label="Account settings">
      <div className="ld-customer-security-options">
        <Link to="/designer/account"><UserRound size={22} aria-hidden="true"/><span><strong>Change name</strong></span><span aria-hidden="true">→</span></Link>
        <Link to="/designer/change-email"><Mail size={22} aria-hidden="true"/><span><strong>Change email</strong></span><span aria-hidden="true">→</span></Link>
        <Link to="/designer/change-password"><KeyRound size={22} aria-hidden="true"/><span><strong>Change password</strong></span><span aria-hidden="true">→</span></Link>
      </div>
    </section>)}
  </section>
}
