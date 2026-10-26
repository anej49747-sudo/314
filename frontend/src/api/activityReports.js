import { client } from './client.js'

export async function fetchActivityReport(frequency, date, signal) {
  const { data } = await client.get('/manager/reports/activity', {
    params: { frequency, date }, signal,
  })
  return data
}

/** Arbitrary inclusive Singapore calendar date range; original periodic API remains available. */
export async function fetchActivityRange(startDate, endDate, signal) {
  const { data } = await client.get('/manager/reports/activity/range', {
    params: { startDate, endDate }, signal,
  })
  return data
}
