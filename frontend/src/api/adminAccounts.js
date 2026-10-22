import { client } from './client.js'
import { buildAccountSearchParams } from '../account/accountSearchRules.js'

export async function searchAdminAccounts(filters, page = 0, signal) {
  const { data } = await client.get('/admin/accounts', {
    params: buildAccountSearchParams(filters, page),
    signal,
  })
  return data
}
