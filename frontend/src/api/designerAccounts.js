import { client, csrfHeaders } from './client.js'

export async function currentAdmin() {
  const { data } = await client.get('/auth/admin/me')
  return data
}

export async function createDesigner(values) {
  const headers = await csrfHeaders()
  const { data } = await client.post('/admin/designers', values, { headers })
  return data
}
