export const REPORT_METRICS = [
  { key: 'logins', label: 'Successful logins', hint: 'Completed customer/designer sign-ins' },
  { key: 'uniqueUsers', label: 'Unique users logged in', hint: 'Deduplicated for the entire period' },
  { key: 'newAccounts', label: 'New accounts', hint: 'Customers and interior designers' },
  { key: 'projectViews', label: 'Project views', hint: 'Qualifying project-detail views' },
  { key: 'favouritesAdded', label: 'Favourites added', hint: 'Actual first saves' },
  { key: 'favouritesRemoved', label: 'Favourites removed', hint: 'Actual successful removals' },
  { key: 'reviewsSubmitted', label: 'Reviews submitted', hint: 'Customer review submissions' },
]

const ENGAGEMENT_KEYS = new Set(['projectViews', 'favouritesAdded', 'favouritesRemoved'])

export function isEngagementMetric(key) { return ENGAGEMENT_KEYS.has(key) }

// Stable ISO date independent of the browser's local timezone.
export function todayInSingapore(now = new Date()) {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Singapore', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(now)
  const value = Object.fromEntries(parts.map(({ type, value: part }) => [type, part]))
  return `${value.year}-${value.month}-${value.day}`
}

function utcDate(value) { return new Date(`${value}T00:00:00Z`) }
function iso(d) { return d.toISOString().slice(0, 10) }

export function selectedReportPeriod(frequency, date) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return null
  const day = utcDate(date)
  if (Number.isNaN(day.getTime()) || iso(day) !== date) return null
  const begin = new Date(day)
  const end = new Date(day)
  if (frequency === 'weekly') {
    const weekday = (day.getUTCDay() + 6) % 7
    begin.setUTCDate(day.getUTCDate() - weekday)
    end.setTime(begin.getTime())
    end.setUTCDate(end.getUTCDate() + 6)
  } else if (frequency === 'monthly') {
    begin.setUTCDate(1)
    end.setTime(begin.getTime())
    end.setUTCMonth(begin.getUTCMonth() + 1)
    end.setUTCDate(0)
  } else if (frequency !== 'daily') return null
  return { startDate: iso(begin), endDate: iso(end) }
}

export function reportLabel(data) {
  return data.startDate === data.endDate ? data.startDate : `${data.startDate} to ${data.endDate}`
}

function csvCell(value) {
  const str = String(value ?? '')
  // Prevent spreadsheet formula evaluation for any future textual fields.
  const guarded = /^[=+@-]/.test(str) && !/^-?\d+(\.\d+)?$/.test(str) ? `'${str}` : str
  return `"${guarded.replaceAll('"', '""')}"`
}

export function reportCsv(data) {
  const header = ['Date', ...REPORT_METRICS.map((metric) => metric.label), 'Engagement coverage']
  const rows = data.days.map((day) => [day.date, ...REPORT_METRICS.map(({ key }) =>
    isEngagementMetric(key) && !day.engagementComplete ? `${day[key]} (partial)` : day[key],
  ), day.engagementComplete ? 'Complete' : 'Incomplete'])
  const total = ['Period total', ...REPORT_METRICS.map(({ key }) =>
    isEngagementMetric(key) && !data.engagementComplete ? `${data[key]} (partial)` : data[key],
  ), data.engagementComplete ? 'Complete' : 'Incomplete']
  return [header, ...rows, total].map((row) => row.map(csvCell).join(',')).join('\r\n') + '\r\n'
}

export function activityReportError(failure) {
  if (failure?.response?.status === 400) return 'Select a valid reporting period.'
  return 'Unable to generate the activity report. Please try again.'
}
