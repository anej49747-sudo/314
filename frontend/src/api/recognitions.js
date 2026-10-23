import { client, csrfHeaders } from './client.js'

export async function getPublicRecognitions(signal) {
  const { data } = await client.get('/recognitions', { signal })
  return data
}

export async function getManagerFeature(month) {
  const { data } = await client.get(`/manager/recognitions/projects/${encodeURIComponent(month)}`)
  return data
}

export async function setManagerFeature(month, projectId) {
  const headers = await csrfHeaders()
  const { data } = await client.put(`/manager/recognitions/projects/${encodeURIComponent(month)}`,
    { projectId }, { headers })
  return data
}

export async function removeManagerFeature(month) {
  const headers = await csrfHeaders()
  await client.delete(`/manager/recognitions/projects/${encodeURIComponent(month)}`, { headers })
}

export async function getManagerAwards() {
  const { data } = await client.get('/manager/recognitions/awards')
  return data
}

export async function grantManagerAward(designerId, title) {
  const headers = await csrfHeaders()
  const { data } = await client.post('/manager/recognitions/awards', { designerId, title }, { headers })
  return data
}

export async function removeManagerAward(awardId) {
  const headers = await csrfHeaders()
  await client.delete(`/manager/recognitions/awards/${encodeURIComponent(awardId)}`, { headers })
}
