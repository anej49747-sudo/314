import { client } from './client.js'
import { projectSearchParams } from '../project/projectSearchRules.js'

export async function getPublicProjects(titleQuery = '', categoryId = null) {
  const { data } = await client.get('/projects', {
    params: projectSearchParams(titleQuery, categoryId),
  })
  return data
}

export async function getPublicProject(projectId) {
  const { data } = await client.get(`/projects/${encodeURIComponent(projectId)}`)
  return data
}
