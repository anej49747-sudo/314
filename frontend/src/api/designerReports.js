import { client, csrfHeaders } from './client.js'

export async function submitDesignerReport(designerId, payload) {
  const headers = await csrfHeaders()
  const { data } = await client.post(`/designers/${encodeURIComponent(designerId)}/reports`, payload, { headers })
  return data
}

export async function searchDesignerReports({ status = 'OPEN', q = '', page = 0 }, signal) {
  const { data } = await client.get('/admin/designer-reports', {
    params: { status, q, page }, signal,
  })
  return data
}

export async function getDesignerReport(reportId) {
  const { data } = await client.get(`/admin/designer-reports/${encodeURIComponent(reportId)}`)
  return data
}

export async function decideDesignerReport(reportId, payload) {
  const headers = await csrfHeaders()
  const { data } = await client.put(
    `/admin/designer-reports/${encodeURIComponent(reportId)}/decision`, payload, { headers },
  )
  return data
}
