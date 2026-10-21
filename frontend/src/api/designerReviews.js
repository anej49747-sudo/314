import { client, csrfHeaders } from './client.js'

// Story #46 only creates reviews. Public review retrieval belongs to Story #10.
export async function submitDesignerReview(designerId, values) {
  const headers = await csrfHeaders()
  const { data } = await client.post(`/designers/${encodeURIComponent(designerId)}/reviews`, values, { headers })
  return data
}
