import { client, csrfHeaders } from './client.js'

const base = '/customer/moodboards'
const safeId = (value) => encodeURIComponent(value)

export async function listMoodboards() {
  const { data } = await client.get(base)
  return data
}
export async function loadMoodboard(id) {
  const { data } = await client.get(`${base}/${safeId(id)}`)
  return data
}
export async function createMoodboard(title) {
  const { data } = await client.post(base, { title }, { headers: await csrfHeaders() })
  return data
}
export async function saveMoodboard(id, payload) {
  const { data } = await client.put(`${base}/${safeId(id)}`, payload, { headers: await csrfHeaders() })
  return data
}
export async function deleteMoodboard(id) {
  await client.delete(`${base}/${safeId(id)}`, { headers: await csrfHeaders() })
}
export async function getMoodboardPhotos() {
  // This endpoint does not touch project view counters.
  const { data } = await client.get(`${base}/photos`)
  return data
}
