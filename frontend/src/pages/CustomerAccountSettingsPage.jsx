import { currentCustomer } from '../api/authentication.js'
import { getCustomerAccountSettings, updateCustomerAccountSettings } from '../api/customerAccountSettings.js'
import AccountSettingsFormPage from '../components/AccountSettingsFormPage.jsx'

export default function CustomerAccountSettingsPage() {
  return <AccountSettingsFormPage
    backPath="/customer/security"
    backText="Back to account security"
    customerReturnButton
    eyebrow="Account security"
    roleLabel=""
    intro=""
    fields="name" pageTitle="Change name"
    description=""
    advisory=""
    footer={null}
    currentAccount={currentCustomer}
    loadAccount={getCustomerAccountSettings}
    updateAccount={updateCustomerAccountSettings}
  />
}
