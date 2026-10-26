import { Link } from 'react-router-dom'
import { currentDesigner } from '../api/designerAuthentication.js'
import { getDesignerAccountSettings, updateDesignerAccountSettings } from '../api/designerAccountSettings.js'
import AccountSettingsFormPage from '../components/AccountSettingsFormPage.jsx'

export default function DesignerAccountSettingsPage() {
  return <AccountSettingsFormPage
    backPath="/designer"
    backText="Back to designer area"
    eyebrow="Designer workspace"
    roleLabel="Interior designer account"
    intro="Update your display name. Email and password changes have their own secure pages."
    fields="name" pageTitle="My profile"
    description="Choose how your account name appears inside Lucky Dog."
    advisory="Your email, portfolio, company details and password remain unchanged."
    footer={<>
      <span>Looking for business settings? <Link to="/designer/company-information">Manage company information →</Link></span>
      <span><Link to="/designer/security">Account security →</Link></span>
    </>}
    currentAccount={currentDesigner}
    loadAccount={getDesignerAccountSettings}
    updateAccount={updateDesignerAccountSettings}
    requirePasswordChange
  />
}
