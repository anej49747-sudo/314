import { client, csrfHeaders } from './client.js'

export async function estimateBudget(choices) {
  const headers = await csrfHeaders()
  const { data } = await client.post('/customer/budget/estimate', choices, { headers })
  return data
}
