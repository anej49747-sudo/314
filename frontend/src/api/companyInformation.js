import { client, csrfHeaders } from './client.js'

export async function getCompanyInformation() {
  const { data } = await client.get('/designer/company-information')
  return data
}

export async function saveCompanyInformation(values) {
  const headers = await csrfHeaders()
  const { data } = await client.put('/designer/company-information', values, { headers })
  return data
}
