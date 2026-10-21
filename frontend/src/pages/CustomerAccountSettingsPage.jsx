import { currentCustomer } from '../api/authentication.js'
import { getCustomerAccountSettings, updateCustomerAccountSettings } from '../api/customerAccountSettings.js'
import AccountSettingsFormPage from '../components/AccountSettingsFormPage.jsx'

export default function CustomerAccountSettingsPage() {
  return <AccountSettingsFormPage
    backPath="/customer"
    backText="Back to customer area"
    eyebrow="Customer workspace"
    roleLabel="Renovation Customer account"
    intro="Keep your account name and sign-in email address up to date."
    description="Update the information associated with your customer login."
    advisory="Your saved projects and password remain unchanged."
    footer={<span>Password changes aren't available on this page.</span>}
    currentAccount={currentCustomer}
    loadAccount={getCustomerAccountSettings}
    updateAccount={updateCustomerAccountSettings}
  />
}
