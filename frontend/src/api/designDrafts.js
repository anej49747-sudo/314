import { client, csrfHeaders } from './client.js'

const base = '/customer/designs'
const idPath = (id) => `${base}/${encodeURIComponent(id)}`
export async function listDesignDrafts() {
  const { data } = await client.get(base)
  return data
}
export async function getDesignDraft(id) {
  const { data } = await client.get(idPath(id))
  return data
}
export async function createDesignDraft(payload) {
  const { data } = await client.post(base, payload, { headers: await csrfHeaders() })
  return data
}
export async function saveDesignDraft(id, payload) {
  const { data } = await client.put(idPath(id), payload, { headers: await csrfHeaders() })
  return data
}
export async function deleteDesignDraft(id) {
  await client.delete(idPath(id), { headers: await csrfHeaders() })
}
