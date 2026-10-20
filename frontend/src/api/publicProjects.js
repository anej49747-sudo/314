import { client } from './client.js'

export async function getPublicProjects(titleQuery = '') {
  const query = titleQuery.trim()
  const { data } = await client.get('/projects', { params: query ? { q: query } : {} })
  return data
}

export async function getPublicProject(projectId) {
  const { data } = await client.get(`/projects/${encodeURIComponent(projectId)}`)
  return data
}
