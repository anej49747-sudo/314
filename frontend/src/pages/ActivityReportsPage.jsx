import ManagerReturnButton from '../components/ManagerReturnButton.jsx'
import { Alert, Button, Card, Select, Spin, Table } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentManager } from '../api/categories.js'
import { fetchActivityRange } from '../api/activityReports.js'
import DateRangeCalendar from '../components/DateRangeCalendar.jsx'
import { previousMonthInSingapore, selectedReportPeriod, todayInSingapore } from '../report/activityReportRules.js'
import { CUSTOMER_METRICS, DESIGNER_METRICS, SITE_METRICS, RANGE_DAY_METRICS,
  rangeCount, rangeReportCsv, validRange } from '../report/rangeReportRules.js'
import '../report/activityReports.css'

const format = n => Number(n || 0).toLocaleString('en-SG')
const lastMonth = () => {
  const d = previousMonthInSingapore()
  const period = selectedReportPeriod('monthly', d)
  return [period.startDate, period.endDate]
}
const currentMonth = () => {
  const today = todayInSingapore()
  return [`${today.slice(0, 7)}-01`, today]
}
function StatGrid({ title, subtitle, metrics, report }) {
  return <section className="ld-report-group" aria-label={title}>
    <div className="ld-report-group-heading"><h2>{title}</h2><p>{subtitle}</p></div>
    <div className="activity-stats">{metrics.map(item => <Card key={item.key} className="activity-stat">
      <div className="activity-stat-label">{item.label}</div>
      <div className="activity-stat-value">{format(rangeCount(report, item))}</div>
      <div className="activity-stat-hint">{item.description}</div>
    </Card>)}</div>
  </section>
}
function TrendGraph({ days = [], metric }) {
  const values = days.map(x => Number(x[metric] || 0))
  if (!days.length) return <p className="activity-chart-empty">No recorded days for this date range.</p>
  const width = 780, height = 270, left = 56, right = 756, top = 22, bottom = 222
  const max = Math.max(...values, 0), step = Math.max(1, Math.ceil(max / 4)), ceiling = step * 4
  // Sample only the chart when ranges are very long. Totals and the daily table remain complete.
  const skip = Math.max(1, Math.ceil(days.length / 80))
  const indices = days.map((_, i) => i).filter(i => i % skip === 0 || i === days.length - 1)
  const pts = indices.map(i => ({ x: left + i / Math.max(1, days.length - 1) * (right - left),
    y: bottom - values[i] / ceiling * (bottom - top), value: values[i], date: days[i].date }))
  return <div className="activity-chart" role="group" aria-label={`Daily ${metric} from ${days[0].date} to ${days.at(-1).date}`}>
    <svg role="img" aria-label={`Trend for ${metric}; maximum ${format(max)}`} viewBox={`0 0 ${width} ${height}`}>
      {Array.from({length:5},(_,i)=> <g key={i}><line x1={left} x2={right} y1={top+(bottom-top)*i/4} y2={top+(bottom-top)*i/4} stroke="#dfe7f0" />
        <text x={left-10} y={top+(bottom-top)*i/4+4} textAnchor="end" className="activity-chart-axis-label">{format(step*(4-i))}</text></g>)}
      <polyline points={pts.map(({x,y}) => `${x},${y}`).join(' ')} fill="none" stroke="#365b82" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round" />
      {pts.map(p => <circle key={p.date} cx={p.x} cy={p.y} r="3.5" fill="#365b82"><title>{p.date}: {format(p.value)}</title></circle>)}
      <text x={left} y={height-13} textAnchor="start" className="activity-chart-axis-label">{days[0].date}</text>
      <text x={right} y={height-13} textAnchor="end" className="activity-chart-axis-label">{days.at(-1).date}</text>
    </svg>
    {max === 0 && <p className="activity-chart-caption">No activity was recorded for this metric in the selected date range.</p>}
    {skip > 1 && <p className="activity-chart-caption">Chart samples dates for readability. Full daily counts are listed below.</p>}
  </div>
}

export default function ActivityReportsPage() {
  const navigate = useNavigate()
  const [[startDate,endDate],setRange] = useState(lastMonth)
  const [applied,setApplied] = useState(lastMonth)
  const [authorized,setAuthorized] = useState(false)
  const [checking,setChecking] = useState(true)
  const [loading,setLoading] = useState(false)
  const [error,setError] = useState('')
  const [report,setReport] = useState(null)
  const [metric,setMetric] = useState('projectViews')
  useEffect(() => {
    let live = true
    currentManager().then(() => { if(live) setAuthorized(true) }).catch(err => {
      if(!live) return
      if([401,403].includes(err.response?.status)) navigate('/login',{replace:true})
      else setError('Unable to verify Platform Manager access.')
    }).finally(() => { if(live) setChecking(false) })
    return () => { live = false }
  },[navigate])
  useEffect(() => {
    if(!authorized) return undefined
    const controller = new AbortController()
    setLoading(true);setError('')
    fetchActivityRange(applied[0],applied[1],controller.signal).then(data=>{
      if(!controller.signal.aborted) setReport(data)
    }).catch(err=>{
      if(controller.signal.aborted) return
      if([401,403].includes(err.response?.status)) navigate('/login',{replace:true})
      else setError(err.response?.data?.message || 'Unable to load this reporting period.')
    }).finally(()=>{if(!controller.signal.aborted)setLoading(false)})
    return ()=>controller.abort()
  },[applied,authorized,navigate])
  const valid = validRange(startDate,endDate)
  const hasActivity = Boolean(report && RANGE_DAY_METRICS.some(m => rangeCount(report,m) > 0))
  const columns = useMemo(()=>[
    {title:'Date (SGT)',dataIndex:'date',key:'date',width:126,fixed:'left'},
    ...RANGE_DAY_METRICS.map(m=>({title:m.label,dataIndex:m.key,key:m.key,width:142,align:'right',render:v=>format(v)})),
  ],[])
  function downloadCsv() {
    if(!report)return
    const blob=new Blob(['\uFEFF',rangeReportCsv(report)],{type:'text/csv;charset=utf-8'})
    const url=URL.createObjectURL(blob),link=document.createElement('a')
    link.href=url;link.download=`luckydog-activity-${report.startDate}-${report.endDate}.csv`
    link.click();URL.revokeObjectURL(url)
  }
  return <div className="activity-reports-page">
    <ManagerReturnButton />
    <p className="activity-eyebrow">Platform management / Reports</p>
    <h1>Activity reports</h1>
    <p className="activity-intro">Explore customer engagement and designer activity over any inclusive date range, using Singapore calendar dates.</p>
    {checking && <div className="loading-state"><Spin/> Checking manager access…</div>}
    {error && <Alert role="alert" showIcon type="error" className="activity-alert" message={error} />}
    {authorized && <>
      <Card title="Choose reporting dates" className="activity-card">
        <p className="activity-helper">Select a start and end directly on the calendar. Click the same day twice for a one-day report, or choose dates across months.</p>
        <div className="activity-date-shortcuts">
          <Button onClick={()=>setRange(lastMonth())}>Previous month</Button>
          <Button onClick={()=>setRange(currentMonth())}>Current month</Button>
        </div>
        <form onSubmit={e=>{e.preventDefault();if(valid)setApplied([startDate,endDate])}}>
          <div className="activity-controls">
            <DateRangeCalendar startDate={startDate} endDate={endDate} onChange={(start,end)=>setRange([start,end])}/>
            <Button type="primary" htmlType="submit" disabled={!valid} loading={loading}>Generate report</Button>
            <Button onClick={downloadCsv} disabled={!report||loading}>Export CSV</Button>
          </div>
        </form>
        <p className="activity-period-hint">{valid ? `Selected: ${startDate} – ${endDate} (inclusive, Singapore time)` : 'Choose a valid date range.'}</p>
      </Card>
      {loading && <div className="loading-state"><Spin/> Generating report…</div>}
      {report && !loading && <>
        <div className="activity-section-header"><h2>Activity overview</h2><span>{report.startDate} – {report.endDate} · Singapore time</span></div>
        {!hasActivity && <Alert showIcon type="info" className="activity-alert" message="No recorded activity for these dates" description="Choose August or September 2026 to explore the seeded historical data." />}
        <StatGrid title="Customer activity" subtitle="Verified customer logins and registrations" report={report} metrics={CUSTOMER_METRICS} />
        <StatGrid title="Interior designer activity" subtitle="Designer sign-ins, registrations and newly created projects" report={report} metrics={DESIGNER_METRICS}/>
        <StatGrid title="Site engagement" subtitle="Project views are anonymous and cannot safely be assigned to customer or designer roles" report={report} metrics={SITE_METRICS}/>
        {!report.engagementComplete && <Alert showIcon type="warning" className="activity-alert" message="Historical coverage may be partial" description={report.coverageNotice} />}
        {report.engagementComplete && <p className="activity-helper">{report.coverageNotice}</p>}
        <Card className="activity-card activity-trend" title="Daily activity trend" extra={<Select aria-label="Chart metric" value={metric} onChange={setMetric}
          style={{minWidth:220}} options={RANGE_DAY_METRICS.map(m=>({value:m.key,label:m.label}))}/> }>
          <TrendGraph days={report.days} metric={metric}/>
        </Card>
        <Card className="activity-card" title="Daily breakdown">
          <Table rowKey="date" size="small" columns={columns} dataSource={report.days} scroll={{x:1460}} pagination={{pageSize:14,showSizeChanger:true}} />
          <p className="activity-helper">Unique users on each day can repeat across dates. The summary above deduplicates across the entire selected period.</p>
        </Card>
      </>}
    </>}
  </div>
}
