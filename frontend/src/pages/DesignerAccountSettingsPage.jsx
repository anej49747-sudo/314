import { currentDesigner } from '../api/designerAuthentication.js'
import { getDesignerAccountSettings, updateDesignerAccountSettings } from '../api/designerAccountSettings.js'
import AccountSettingsFormPage from '../components/AccountSettingsFormPage.jsx'

export default function DesignerAccountSettingsPage() {
  return <AccountSettingsFormPage
    backPath="/designer/security"
    designerReturnButton
    eyebrow="Account security"
    intro="Update your display name. Email and password changes have their own secure pages."
    fields="name" pageTitle="Change name"
    description="Choose how your account name appears inside Lucky Dog."
    advisory="Your email, portfolio, company details and password remain unchanged."
    currentAccount={currentDesigner}
    loadAccount={getDesignerAccountSettings}
    updateAccount={updateDesignerAccountSettings}
    requirePasswordChange
  />
}
