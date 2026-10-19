import { client, csrfHeaders } from './client.js'

export async function getMyPortfolio() {
  const { data } = await client.get('/designer/projects')
  return data
}

export async function updatePortfolioProject(projectId, details, images) {
  const payload = new FormData()
  payload.append('project', new Blob([JSON.stringify(details)], { type: 'application/json' }))
  for (const image of images) payload.append('images', image)
  const headers = await csrfHeaders()
  const { data } = await client.put(`/designer/projects/${projectId}`, payload, { headers })
  return data
}

export async function addPortfolioProject(details, images) {
  const payload = new FormData()
  payload.append('project', new Blob([JSON.stringify(details)], { type: 'application/json' }))
  for (const image of images) payload.append('images', image)
  const headers = await csrfHeaders()
  const { data } = await client.post('/designer/projects', payload, { headers })
  return data
}
