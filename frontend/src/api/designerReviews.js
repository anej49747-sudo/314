import { client, csrfHeaders } from './client.js'

// Story #46 submission is customer-only; Story #10 retrieval is public.
export async function submitDesignerReview(designerId, values) {
  const headers = await csrfHeaders()
  const { data } = await client.post(`/designers/${encodeURIComponent(designerId)}/reviews`, values, { headers })
  return data
}

// Public endpoint: no CSRF token or authenticated session is required.
export async function getPublicDesignerReviews(designerId, page = 0) {
  const { data } = await client.get(`/designers/${encodeURIComponent(designerId)}/reviews`, {
    params: { page },
  })
  return data
}
