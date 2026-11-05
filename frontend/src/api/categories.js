import { client, csrfHeaders } from './client.js'

export async function currentManager() {
  const { data } = await client.get('/auth/manager/me')
  return data
}

export async function listCategories() {
  const { data } = await client.get('/categories')
  return data
}

export async function createCategory(values) {
  const headers = await csrfHeaders()
  const { data } = await client.post('/categories', values, { headers })
  return data
}

export async function deleteCategory(categoryId) {
  const headers = await csrfHeaders()
  const { data } = await client.delete(`/categories/${encodeURIComponent(categoryId)}`, { headers })
  return data
}

export async function updateCategory(categoryId, values) {
  const headers = await csrfHeaders()
  const { data } = await client.put(`/categories/${categoryId}`, values, { headers })
  return data
}
