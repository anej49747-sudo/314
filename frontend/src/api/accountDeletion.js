import { client, csrfHeaders } from './client.js'

export async function deleteDesignerAccount(id) {
  const headers = await csrfHeaders()
  await client.delete(`/admin/accounts/${id}`, { headers })
}

export async function deleteOwnCustomerAccount() {
  const headers = await csrfHeaders()
  await client.delete('/customer/account', { headers })
}
