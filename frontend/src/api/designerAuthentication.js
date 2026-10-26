import { client, csrfHeaders } from './client.js'

export async function currentDesigner() {
  const { data } = await client.get('/auth/designer/me')
  return data
}

export async function currentDesignerArea() {
  const { data } = await client.get('/designer/area')
  return data
}

export async function changeDesignerPassword(values) {
  const headers = await csrfHeaders()
  await client.post('/auth/change-password', values, { headers })
}

export async function updateOwnDesignerPassword(values) {
  const headers = await csrfHeaders()
  await client.post('/auth/update-own-password', values, { headers })
}
