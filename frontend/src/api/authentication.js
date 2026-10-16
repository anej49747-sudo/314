import { client, csrfHeaders } from './client.js'

export async function loginCustomer(credentials) {
  const headers = await csrfHeaders()
  const { data } = await client.post('/login', credentials, { headers })
  return data
}

export async function currentCustomer() {
  const { data } = await client.get('/auth/me')
  return data
}

export async function logoutCustomer() {
  const headers = await csrfHeaders()
  await client.post('/logout', null, { headers })
}
