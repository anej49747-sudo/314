import { client, csrfHeaders } from './client.js'

/** Uses role-checked API and never sends credentials through GET or the URL. */
export async function updateOwnAccountPassword(values) {
  const headers = await csrfHeaders()
  await client.post('/auth/account/change-password', values, { headers })
}
