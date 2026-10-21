import { client, csrfHeaders } from './client.js'

export async function getCustomerAccountSettings() {
  const { data } = await client.get('/customer/account')
  return data
}

export async function updateCustomerAccountSettings(values) {
  const headers = await csrfHeaders()
  const { data } = await client.put('/customer/account', values, { headers })
  return data
}
