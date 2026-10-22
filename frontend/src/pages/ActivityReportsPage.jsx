import { Alert, Button, Card, Input, Radio, Select, Spin, Table } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentManager } from '../api/categories.js'
import { fetchActivityReport } from '../api/activityReports.js'
import {
  REPORT_METRICS, activityReportError, isEngagementMetric, reportCsv,
  reportLabel, selectedReportPeriod, todayInSingapore,
} from '../report/activityReportRules.js'
import '../report/activityReports.css'

const fmt = (value) => Number(value ?? 0).toLocaleString('en-SG')

function TrendGraph({ days, metric }) {
  const values = days.map((day) => day[metric] ?? 0)
  const max = Math.max(1, ...values)
  const width = 720
  const height = 188
  const points = values.map((value, i) => {
    const x = 30 + (i * (width - 60) / Math.max(1, values.length - 1))
    const y = 15 + (max - value) * (height - 35) / max
    return [x, y]
  })
  if (!days.length) return <p>No data available for this period.</p>
  return <div className="activity-chart" role="img" aria-label={`${REPORT_METRICS.find((m) => m.key === metric)?.label ?? 'Activity'} trend. Values: ${values.join(', ')}`}>
    <svg viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none">
      {[0, 1, 2, 3].map((line) => <line key={line} x1="22" x2="710"
        y1={18 + line * 44} y2={18 + line * 44} stroke="#E3EAF2" />)}
      <polyline fill="none" stroke="#365B82" strokeWidth="3" strokeLinejoin="round"
        strokeLinecap="round" points={points.map(([x, y]) => `${x},${y}`).join(' ')} />
      {points.map(([x, y], i) => <circle key={days[i].date} cx={x} cy={y} r="3.5" fill="#365B82" />)}
    </svg>
    <div className="activity-chart-labels"><span>{days[0].date}</span><span>{days.at(-1).date}</span></div>
  </div>
}

export default function ActivityReportsPage() {
  const navigate = useNavigate()
  const [authorized, setAuthorized] = useState(false)
  const [checking, setChecking] = useState(true)
  const [frequency, setFrequency] = useState('weekly')
  const [date, setDate] = useState(() => todayInSingapore())
  const [applied, setApplied] = useState(() => ({ frequency: 'weekly', date: todayInSingapore() }))
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [metric, setMetric] = useState('projectViews')

  useEffect(() => {
    let active = true
    currentManager().then(() => { if (active) setAuthorized(true) })
      .catch((failure) => {
        if (!active) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError('Unable to verify Platform Manager access. Please try again.')
      }).finally(() => { if (active) setChecking(false) })
    return () => { active = false }
  }, [navigate])

  useEffect(() => {
    if (!authorized) return undefined
    const controller = new AbortController()
    setLoading(true)
    setError('')
    fetchActivityReport(applied.frequency, applied.date, controller.signal)
      .then((data) => { if (!controller.signal.aborted) setReport(data) })
      .catch((failure) => {
        if (controller.signal.aborted) return
        if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
        else setError(activityReportError(failure))
      }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => { controller.abort() }
  }, [authorized, applied, navigate])

  const period = useMemo(() => selectedReportPeriod(frequency, date), [frequency, date])
  const columns = [
    { title: 'Date (SGT)', dataIndex: 'date', key: 'date', fixed: 'left', width: 128 },
    ...REPORT_METRICS.map(({ key, label }) => ({
      title: label, dataIndex: key, key, align: 'right', width: 145,
      render: (value, row) => <span title={isEngagementMetric(key) && !row.engagementComplete ? 'Partial historical coverage' : undefined}>
        {fmt(value)}{isEngagementMetric(key) && !row.engagementComplete ? ' *' : ''}
      </span>,
    })),
  ]

  function downloadCsv() {
    if (!report) return
    const blob = new Blob(['\uFEFF', reportCsv(report)], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `user-activity-${report.frequency}-${report.startDate}.csv`
    anchor.click()
    URL.revokeObjectURL(url)
  }

  return <div className="activity-reports-page">
    <p className="activity-eyebrow">Platform management / Reports</p>
    <h1>User activity reports</h1>
    <p className="activity-intro">Generate daily, weekly and monthly reports on user activity, project interest and engagement.</p>
    {error && <Alert type="error" showIcon role="alert" message={error} className="activity-alert" />}
    {checking && <div className="loading-state"><Spin /> Checking manager access…</div>}
    {authorized && <>
      <Card className="activity-card" title="Generate report">
        <p className="activity-helper">Choose a period. Reporting uses Asia/Singapore calendar days.</p>
        <form onSubmit={(event) => { event.preventDefault(); if (period) setApplied({ frequency, date }) }}>
          <div className="activity-controls">
            <label className="activity-field">Report frequency
              <Radio.Group value={frequency} onChange={(event) => setFrequency(event.target.value)}>
                <Radio.Button value="daily">Daily</Radio.Button>
                <Radio.Button value="weekly">Weekly</Radio.Button>
                <Radio.Button value="monthly">Monthly</Radio.Button>
              </Radio.Group>
            </label>
            <label className="activity-field">{frequency === 'daily' ? 'Select date' : frequency === 'weekly' ? 'Select a date in the week' : 'Select a date in the month'}
              <Input type="date" value={date} max={todayInSingapore()} onChange={(event) => setDate(event.target.value)} />
            </label>
            <Button type="primary" htmlType="submit" loading={loading} disabled={!period}>Generate report</Button>
            <Button onClick={downloadCsv} disabled={!report || loading}>Export CSV</Button>
          </div>
        </form>
        {period && <p className="activity-period-hint">Selected period: {period.startDate} to {period.endDate} · Singapore Time</p>}
      </Card>
      {loading && <div className="loading-state"><Spin /> Generating report…</div>}
      {report && !loading && <>
        <div className="activity-section-header"><h2>Activity overview</h2>
          <span>{reportLabel(report)} · {report.frequency} report</span></div>
        <div className="activity-stats">
          {REPORT_METRICS.map(({ key, label, hint }) => <Card key={key} className="activity-stat">
            <div className="activity-stat-label">{label}</div>
            <div className="activity-stat-value">{fmt(report[key])}{isEngagementMetric(key) && !report.engagementComplete ? <sup>*</sup> : null}</div>
            <div className="activity-stat-hint">{hint}</div>
          </Card>)}
        </div>
        <Alert type="info" showIcon className="activity-alert" message="Historical data coverage"
          description={<>{report.coverageNotice}{!report.engagementComplete ? ' * Engagement totals for this period are observed events only and are not historically complete.' : ''}</>} />
        <Card className="activity-card activity-trend" title="Activity trend" extra={<Select
          aria-label="Display metric" value={metric} onChange={setMetric} style={{ minWidth: 210 }}
          options={REPORT_METRICS.map(({ key, label }) => ({ value: key, label }))} />}>
          <TrendGraph days={report.days} metric={metric} />
        </Card>
        <Card className="activity-card" title="Daily activity breakdown">
          <Table rowKey="date" size="small" columns={columns} dataSource={report.days}
            pagination={false} scroll={{ x: 1160 }} />
          <p className="activity-helper">* Partial coverage: tracking began after the start of this day or period. Unique users across the full period may be less than the sum of daily unique users.</p>
        </Card>
      </>}
    </>}
  </div>
}
