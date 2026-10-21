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
    intro="Keep your account name and sign-in email address up to date. Your company information is managed separately."
    description="Update the information associated with your designer login."
    advisory="Portfolio, company information and your password remain unchanged."
    footer={<>
      <span>Looking for business settings? <Link to="/designer/company-information">Manage company information →</Link></span>
      <span>Password changes aren't available on this page.</span>
    </>}
    currentAccount={currentDesigner}
    loadAccount={getDesignerAccountSettings}
    updateAccount={updateDesignerAccountSettings}
    requirePasswordChange
  />
}
