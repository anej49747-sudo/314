import { Alert, Button, Card, Form, Select, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getCatalogue, getPreferences, setPreferences, skipPreferences } from '../api/discovery.js'
import CustomerReturnButton from '../components/CustomerReturnButton.jsx'
import { sizeRangeLabel } from '../discovery/presentationRules.js'
import '../ui/discovery.css'
const spec = [
  { name: 'categoryIds', kind: 'categories', label: 'Renovation interests', multiple: true },
  { name: 'styleIds', kind: 'styles', label: 'Design styles', multiple: true },
  { name: 'propertyTypeIds', kind: 'property-types', label: 'Property types', multiple: true },
  { name: 'sizeRangeId', kind: 'size-ranges', label: 'Preferred floor area', multiple: false },
]
export default function PreferencesPage({ onboarding = false }) {
  const navigate = useNavigate(); const [form] = Form.useForm()
  const [catalogues, setCatalogues] = useState({})
  const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false); const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    Promise.all([getPreferences(), ...spec.map(x => getCatalogue(x.kind))]).then(([prefs, ...lists]) => {
      if (!active) return
      setCatalogues(Object.fromEntries(spec.map((item, index) => [item.kind, lists[index]])))
      form.setFieldsValue(prefs)
      if (onboarding && prefs.onboardingState !== 'PENDING') navigate('/customer', { replace: true })
    }).catch(() => { if (active) setError('Unable to load your preferences. Please sign in as a customer.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [form, navigate, onboarding])
  async function save(values) { setBusy(true); setError(''); try {
    await setPreferences({ categoryIds: values.categoryIds || [], styleIds: values.styleIds || [], propertyTypeIds: values.propertyTypeIds || [], sizeRangeId: values.sizeRangeId || null })
    navigate('/customer', { replace: true })
  } catch (failure) { setError(failure.response?.data?.message || 'Could not save preferences.') } finally { setBusy(false) } }
  async function skip() { setBusy(true); setError(''); try { await skipPreferences(); navigate('/customer', { replace: true }) }
    catch { setError('Unable to skip setup right now.') } finally { setBusy(false) } }
  return <section className="discovery-page discovery-preferences">
    {!onboarding && <CustomerReturnButton />}
    <p className="discovery-eyebrow">{onboarding ? 'Welcome to Lucky Dog' : 'Your account'}</p>
    <h1>{onboarding ? 'Tell us what inspires you' : 'My preferences'}</h1>
    <p>Choose what interests you. Every answer is optional, and you can change it later.</p>
    {loading && <Spin />}{error && <Alert type="error" showIcon message={error} />}
    {!loading && <Card><Form form={form} layout="vertical" onFinish={save}>
      {spec.map(field => <Form.Item key={field.name} label={field.label} name={field.name}>
        <Select mode={field.multiple ? 'multiple' : undefined} allowClear placeholder={(catalogues[field.kind] || []).length ? 'Select interests' : 'Pending setup'}
          disabled={!(catalogues[field.kind] || []).length} options={(catalogues[field.kind] || []).map(x => ({ value: x.id, label: field.kind === 'size-ranges' ? sizeRangeLabel(x) : x.name }))} />
      </Form.Item>)}
      <div className="discovery-actions"><Button type="primary" loading={busy} htmlType="submit">Save preferences</Button>
        {onboarding && <Button disabled={busy} onClick={skip}>Skip for now</Button>}</div>
    </Form></Card>}
  </section>
}
