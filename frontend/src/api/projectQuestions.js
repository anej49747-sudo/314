import { client, csrfHeaders } from './client.js'

export async function getAnsweredProjectQuestions(projectId, { query = '', order = 'newest', page = 0, size = 3 } = {}) {
  const { data } = await client.get(`/projects/${projectId}/questions`, {
    params: { query, order, page, size },
  })
  return data
}

export async function getMyPendingProjectQuestions(projectId) {
  const { data } = await client.get(`/customer/projects/${projectId}/questions`)
  return data
}

export async function askProjectQuestion(projectId, question) {
  const headers = await csrfHeaders()
  const { data } = await client.post(`/projects/${projectId}/questions`, { question }, { headers })
  return data
}

export async function getDesignerProjectQuestions({ status = 'pending', projectId, query = '', page = 0 } = {}) {
  const { data } = await client.get('/designer/project-questions', {
    params: { status, projectId: projectId || undefined, query, page },
  })
  return data
}

export async function answerProjectQuestion(questionId, answer) {
  const headers = await csrfHeaders()
  const { data } = await client.post(`/designer/project-questions/${questionId}/answer`, { answer }, { headers })
  return data
}
