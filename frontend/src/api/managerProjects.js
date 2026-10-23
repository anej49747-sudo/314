import { client, csrfHeaders } from './client.js'

export async function listManagerProjects(signal) {
  const { data } = await client.get('/manager/projects', { signal })
  return data
}

export async function correctManagerProject(projectId, values) {
  const headers = await csrfHeaders()
  const { data } = await client.put(`/manager/projects/${encodeURIComponent(projectId)}`, values, { headers })
  return data
}
