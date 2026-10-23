import { client, csrfHeaders } from './client.js'
import { loginHistoryParams } from '../account/accountManagementRules.js'

export async function suspendAdminAccount(accountId, reason) {
  const headers = await csrfHeaders()
  const { data } = await client.put(`/admin/accounts/${accountId}/suspend`, { reason }, { headers })
  return data
}

export async function reactivateAdminAccount(accountId) {
  const headers = await csrfHeaders()
  const { data } = await client.put(`/admin/accounts/${accountId}/reactivate`, null, { headers })
  return data
}

export async function fetchAccountLoginHistory(accountId, outcome = 'all', page = 0, signal) {
  const { data } = await client.get(`/admin/accounts/${accountId}/login-history`, {
    params: loginHistoryParams(outcome, page), signal,
  })
  return data
}
