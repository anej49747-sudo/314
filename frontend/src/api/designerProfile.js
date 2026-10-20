import { client } from './client.js'

export async function getDesignerProfile(designerId) {
  const { data } = await client.get(`/designers/${encodeURIComponent(designerId)}`)
  return data
}
