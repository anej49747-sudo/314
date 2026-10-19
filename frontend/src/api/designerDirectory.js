import { client } from './client.js'

export async function getDesignerDirectory() {
  const { data } = await client.get('/designers')
  return data
}
