import { client, csrfHeaders } from './client.js'
export async function getCatalogue(kind) { const { data } = await client.get(`/discovery/catalogues/${kind}`); return data }
export async function saveCatalogue(kind, value, id = null) {
  const headers = await csrfHeaders()
  const { data } = id == null
    ? await client.post(`/discovery/catalogues/${kind}`, value, { headers })
    : await client.put(`/discovery/catalogues/${kind}/${id}`, value, { headers })
  return data
}
export async function deleteCatalogue(kind, id) { const headers = await csrfHeaders(); await client.delete(`/discovery/catalogues/${kind}/${id}`, { headers }) }
export async function getPreferences() { const { data } = await client.get('/discovery/customer/preferences'); return data }
export async function setPreferences(value) { const headers = await csrfHeaders(); const { data } = await client.put('/discovery/customer/preferences', value, { headers }); return data }
export async function skipPreferences() { const headers = await csrfHeaders(); await client.post('/discovery/customer/preferences/skip', null, { headers }) }
export async function getRecommendations({ page = 0, size = 8, reel = false, excludeIds = [] } = {}) { const { data } = await client.get('/discovery/projects', { params: { page, size, reel, excludeIds: excludeIds.join(',') } }); return data }
// General public photo discovery. This endpoint never returns match scores.
export async function getExploreProjects({ page = 0, size = 6, excludeIds = [] } = {}) {
  const { data } = await client.get('/discovery/explore', { params: { page, size, excludeIds: excludeIds.join(',') } })
  return data
}
export async function getDesignerRecommendations(limit = 6) { const { data } = await client.get('/discovery/designers', { params: { limit } }); return data }
export async function getProjectMetadata(id, own = false) { const { data } = await client.get(own ? `/discovery/designer/projects/${id}/metadata` : `/discovery/projects/${id}/metadata`); return data }
