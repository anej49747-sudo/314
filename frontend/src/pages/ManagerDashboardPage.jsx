import { Alert, Spin } from 'antd'
import { Award, BookOpenText, ChartNoAxesColumn, FolderPen, Layers3 } from 'lucide-react'
import { ActionCard } from '../components/VisualCards.jsx'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { currentManager } from '../api/categories.js'

const tools = [
  { to: '/manager/categories', title: 'Project classifications', detail: 'Manage renovation categories, design styles, property types and floor-area ranges.', Icon: Layers3 },
  { to: '/manager/projects', title: 'Edit projects', detail: 'Correct published project information.', Icon: FolderPen },
  { to: '/manager/articles', title: 'Renovation articles', detail: 'Publish and manage renovation guidance.', Icon: BookOpenText },
  { to: '/manager/recognitions', title: 'Designer recognition', detail: 'Feature a project and recognise outstanding designers.', Icon: Award },
  { to: '/manager/reports', title: 'Activity reports', detail: 'Understand platform traffic and engagement.', Icon: ChartNoAxesColumn },
]

export default function ManagerDashboardPage() {
  const navigate = useNavigate()
  const [state, setState] = useState('loading')
  useEffect(() => {
    let active = true
    currentManager().then(() => { if (active) setState('ready') }).catch((failure) => {
      if (!active) return
      if ([401, 403].includes(failure.response?.status)) navigate('/login', { replace: true })
      else setState('error')
    })
    return () => { active = false }
  }, [navigate])

  return <section className="ui-dashboard" aria-labelledby="manager-dashboard-title">
    <header className="ui-dashboard-header"><div><p className="ui-eyebrow">Platform management</p>
      <h1 id="manager-dashboard-title">Management dashboard</h1>
      <p>Manage published content, recognition and platform activity from one place.</p></div></header>
    {state === 'loading' && <div className="loading-state" role="status"><Spin /> Checking manager access…</div>}
    {state === 'error' && <Alert type="error" role="alert" showIcon message="Unable to verify Platform Manager access. Please try again." />}
    {state === 'ready' && <div className="ui-management-grid">
      {tools.map(({ to, title, detail, Icon }) => <ActionCard key={to} to={to} title={title}
        description={detail} Icon={Icon} variant="management" />)}
    </div>}
  </section>
}
