import { client } from './client.js'

export async function getPublicProject(projectId) {
  const { data } = await client.get(`/projects/${encodeURIComponent(projectId)}`)
  return data
}
