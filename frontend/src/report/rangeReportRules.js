// Inclusive date ranges in Singapore time, without converting dates through browser local time.
export const CUSTOMER_METRICS = [
  { key: 'customerLogins', label: 'Logins', description: 'Successful customer sign-ins' },
  { key: 'customerUniqueUsers', label: 'Unique logins', description: 'Distinct customers in the period' },
  { key: 'customerNewAccounts', label: 'New accounts', description: 'Customer registrations' },
]
export const DESIGNER_METRICS = [
  { key: 'designerLogins', label: 'Logins', description: 'Successful designer sign-ins' },
  { key: 'designerUniqueUsers', label: 'Unique logins', description: 'Distinct designers in the period' },
  { key: 'designerNewAccounts', label: 'New accounts', description: 'Designer accounts created' },
  { key: 'designerProjectsCreated', label: 'Projects created', description: 'Projects created during the selected period' },
]
export const SITE_METRICS = [
  { key: 'projectViews', label: 'Project views', description: 'Anonymous visits to project details' },
  { key: 'favouritesAdded', label: 'Favourites added', description: 'Recorded successful saves' },
  { key: 'reviewsSubmitted', label: 'Reviews submitted', description: 'Customer reviews of designers' },
]
export const RANGE_DAY_METRICS = [...CUSTOMER_METRICS, ...DESIGNER_METRICS, ...SITE_METRICS]
export function validRange(startDate, endDate) {
  const valid = (value) => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) return false
    const parsed = new Date(`${value}T00:00:00Z`)
    return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value
  }
  return valid(startDate) && valid(endDate) && startDate <= endDate
}
export function rangeCount(report, metric) {
  if (metric.key === 'customerUniqueUsers') return Number(report.customer?.uniqueUsers || 0)
  if (metric.key === 'designerUniqueUsers') return Number(report.designer?.uniqueUsers || 0)
  if (metric.key.startsWith('customer')) {
    const source = { customerLogins: 'logins', customerNewAccounts: 'newAccounts' }[metric.key]
    return Number(report.customer?.[source] || 0)
  }
  if (metric.key.startsWith('designer')) {
    const source = { designerLogins: 'logins', designerNewAccounts: 'newAccounts', designerProjectsCreated: 'projectsCreated' }[metric.key]
    return Number(report.designer?.[source] || 0)
  }
  return Number(report[metric.key] || 0)
}
function csvEscape(value) {
  const data = String(value ?? '')
  const safe = /^[=+@-]/.test(data) && !/^-?\d+(\.\d+)?$/.test(data) ? `'${data}` : data
  return `"${safe.replaceAll('"', '""')}"`
}
export function rangeReportCsv(report) {
  const header = ['Singapore date', ...RANGE_DAY_METRICS.map(x => x.label), 'Engagement coverage']
  const lines = (report.days || []).map(day => [day.date, ...RANGE_DAY_METRICS.map(x => day[x.key]),
    day.engagementComplete ? 'Complete' : 'Partial'])
  const totals = ['Period total', ...RANGE_DAY_METRICS.map(metric => rangeCount(report, metric)),
    report.engagementComplete ? 'Complete' : 'Partial']
  return [header, ...lines, totals].map(line => line.map(csvEscape).join(',')).join('\r\n') + '\r\n'
}
