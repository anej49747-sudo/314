import { Link } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import { getCustomerAccountSettings, updateCustomerAccountSettings } from '../api/customerAccountSettings.js'
import AccountSettingsFormPage from '../components/AccountSettingsFormPage.jsx'
import CustomerDeleteAccountSection from '../components/CustomerDeleteAccountSection.jsx'

export default function CustomerAccountSettingsPage() {
  return <AccountSettingsFormPage
    backPath="/customer"
    backText="Back to customer area"
    eyebrow="Customer workspace"
    roleLabel="Renovation Customer account"
    intro="Update your display name. Sign-in details can be changed separately in Account security."
    fields="name" pageTitle="My profile"
    description="Update the name shown on your customer account."
    advisory="Your email, saved projects and password remain unchanged."
    footer={<span><Link to="/customer/security">Account security →</Link></span>}
    afterContent={<CustomerDeleteAccountSection />}
    currentAccount={currentCustomer}
    loadAccount={getCustomerAccountSettings}
    updateAccount={updateCustomerAccountSettings}
  />
}
