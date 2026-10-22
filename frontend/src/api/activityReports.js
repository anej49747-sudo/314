import { client } from './client.js'

export async function fetchActivityReport(frequency, date, signal) {
  const { data } = await client.get('/manager/reports/activity', {
    params: { frequency, date }, signal,
  })
  return data
}
