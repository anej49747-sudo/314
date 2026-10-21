import { client } from './client.js'
import { projectSearchParams } from '../project/projectSearchRules.js'
import { createInFlightRequestDeduper } from '../project/inFlightRequestDeduper.js'

const loadProjectOnce = createInFlightRequestDeduper()

export async function getPublicProjects(titleQuery = '', categoryId = null) {
  const { data } = await client.get('/projects', {
    params: projectSearchParams(titleQuery, categoryId),
  })
  return data
}

export function getPublicProject(projectId) {
  // React StrictMode can run the loading effect twice in development. On a
  // first anonymous visit, parallel requests could create separate sessions
  // before the browser receives its first JSESSIONID cookie (two views).
  return loadProjectOnce(String(projectId), async () => {
    const { data } = await client.get(`/projects/${encodeURIComponent(projectId)}`)
    return data
  })
}
