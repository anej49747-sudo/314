import { client, csrfHeaders } from './client.js'

export async function getSavedProjects() {
  const { data } = await client.get('/customer/favourites')
  return data
}

export async function getFavouriteStatus(projectId) {
  const { data } = await client.get(`/customer/favourites/${encodeURIComponent(projectId)}`)
  return data
}

export async function saveFavourite(projectId) {
  const headers = await csrfHeaders()
  await client.put(`/customer/favourites/${encodeURIComponent(projectId)}`, null, { headers })
}

export async function removeFavourite(projectId) {
  const headers = await csrfHeaders()
  await client.delete(`/customer/favourites/${encodeURIComponent(projectId)}`, { headers })
}
