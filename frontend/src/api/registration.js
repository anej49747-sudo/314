import axios from 'axios'

const client = axios.create({ baseURL: '/api', timeout: 10000 })

export async function registerCustomer(data) {
  const response = await client.post('/register', data)
  return response.data
}
