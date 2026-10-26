import { Link } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import { currentDesigner } from '../api/designerAuthentication.js'
import { getCustomerAccountSettings, updateCustomerAccountSettings } from '../api/customerAccountSettings.js'
import { getDesignerAccountSettings, updateDesignerAccountSettings } from '../api/designerAccountSettings.js'
import AccountSettingsFormPage from '../components/AccountSettingsFormPage.jsx'

export default function ChangeAccountEmailPage({ role }) {
  const customer = role === 'customer'
  return <AccountSettingsFormPage
    backPath={`/${role}/security`} backText="Back to account security"
    eyebrow={customer ? 'Customer account' : 'Designer workspace'} roleLabel={customer ? 'Renovation Customer' : 'Interior Designer'}
    pageTitle="Change email address" fields="email"
    intro="Change the address you use to sign in. After saving, sign in again with your new email."
    description="Enter your new sign-in email address."
    advisory="Your display name, saved content and password remain unchanged."
    currentAccount={customer ? currentCustomer : currentDesigner}
    loadAccount={customer ? getCustomerAccountSettings : getDesignerAccountSettings}
    updateAccount={customer ? updateCustomerAccountSettings : updateDesignerAccountSettings}
    requirePasswordChange={!customer}
    footer={<Link to={`/${role}/security`}>Return to account security →</Link>}
  />
}
