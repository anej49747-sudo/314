import { client } from './client.js'

export async function getMyPortfolio() {
  const { data } = await client.get('/designer/projects')
  return data
}
