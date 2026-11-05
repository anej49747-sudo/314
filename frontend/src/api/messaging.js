import { client, csrfHeaders } from './client.js'

export async function listConversations() {
  const { data } = await client.get('/messages/conversations')
  return data
}

export async function getConversationMessages(conversationId, before) {
  const { data } = await client.get(`/messages/conversations/${conversationId}/messages`, {
    params: before ? { before } : {},
  })
  return data
}

export async function contactDesigner(designerId, body) {
  const headers = await csrfHeaders()
  const { data } = await client.post(`/messages/designers/${designerId}/messages`, { body }, { headers })
  return data
}

export async function sendConversationMessage(conversationId, body) {
  const headers = await csrfHeaders()
  const { data } = await client.post(`/messages/conversations/${conversationId}/messages`, { body }, { headers })
  return data
}
