import { client } from './client.js'

export async function registerCustomer(data) {
  const response = await client.post('/register', data)
  return response.data
}
