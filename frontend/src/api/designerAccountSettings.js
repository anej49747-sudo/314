import { client, csrfHeaders } from './client.js'

export async function getDesignerAccountSettings() {
  const { data } = await client.get('/designer/account')
  return data
}

export async function updateDesignerAccountSettings(values) {
  const headers = await csrfHeaders()
  const { data } = await client.put('/designer/account', values, { headers })
  return data
}
