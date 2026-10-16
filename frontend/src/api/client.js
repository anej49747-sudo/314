import axios from 'axios'

// Same-origin /api in development (Vite proxy) and deployment (reverse proxy).
// The browser stores the HttpOnly JSESSIONID cookie; the app never reads it.
export const client = axios.create({ baseURL: '/api', timeout: 10000, withCredentials: true })

export async function csrfHeaders() {
  const { data } = await client.get('/auth/csrf')
  return { [data.headerName]: data.token }
}
