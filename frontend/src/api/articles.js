import { client, csrfHeaders } from './client.js'

export async function listArticles(signal) {
  const { data } = await client.get('/articles', { signal })
  return data
}

export async function getArticle(id, signal) {
  const { data } = await client.get(`/articles/${id}`, { signal })
  return data
}

export async function publishArticle(details, cover) {
  const payload = new FormData()
  payload.append('article', new Blob([JSON.stringify(details)], { type: 'application/json' }))
  if (cover) payload.append('cover', cover)
  const headers = await csrfHeaders()
  const { data } = await client.post('/manager/articles', payload, { headers, timeout: 30000 })
  return data
}
