import { Alert, Button, Input, Modal, Select } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentSession } from '../api/authentication.js'
import { submitDesignerReport } from '../api/designerReports.js'
import { REPORT_REASONS, reportErrorMessage, validateCustomerReport } from '../dispute/reportRules.js'
import '../dispute/designerReports.css'

export default function DesignerReportAction({ designerId, designerName }) {
  const navigate = useNavigate()
  const [access, setAccess] = useState('loading')
  const [open, setOpen] = useState(false)
  const [loginPrompt, setLoginPrompt] = useState(false)
  const [reason, setReason] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let mounted = true
    setAccess('loading')
    currentSession().then((account) => {
      if (mounted) setAccess(account.role === 'CUSTOMER' ? 'customer' : 'other')
    }).catch((failure) => {
      if (mounted) setAccess(failure.response?.status === 401 ? 'guest' : 'error')
    })
    return () => { mounted = false }
  }, [designerId])

  function openReport() {
    setError('')
    if (access === 'guest') { setLoginPrompt(true); return }
    if (access !== 'customer') {
      setError('Unable to verify customer access. Please sign in and try again.')
      return
    }
    setReason('')
    setDescription('')
    setOpen(true)
  }

  async function submit() {
    if (submitting) return
    const validation = validateCustomerReport(reason, description)
    if (validation) { setError(validation); return }
    setSubmitting(true)
    setError('')
    try {
      const saved = await submitDesignerReport(designerId, { reason, description: description.trim() })
      setSuccess(`Your private report REF-${saved.id} has been received for review.`)
      setOpen(false)
    } catch (failure) {
      if (failure.response?.status === 401) {
        setAccess('guest')
        setOpen(false)
        setLoginPrompt(true)
      } else {
        setError(reportErrorMessage(failure))
      }
    } finally {
      setSubmitting(false)
    }
  }

  return <div className="designer-report-action">
    {access === 'customer' && (
      <Button className="designer-report-button" type="primary" danger
        disabled={access === 'loading'} onClick={openReport}>Report designer</Button>
    )}
    {success && <Alert role="status" type="success" showIcon message={success} />}
    {!open && error && <Alert role="alert" type="error" showIcon message={error} />}
    <Modal open={open} title={`Report ${designerName}`} width={560}
      onCancel={() => { if (!submitting) { setOpen(false); setError('') } }}
      onOk={submit} okText="Submit report" okButtonProps={{ loading: submitting }}
      cancelButtonProps={{ disabled: submitting }} closable={!submitting}
      maskClosable={!submitting} destroyOnHidden>
      <p>Send a private concern to the User Admin team. Submitting a report does not automatically suspend a designer.</p>
      <div className="designer-report-form-field">
        <label htmlFor="designer-report-reason">Reason for reporting *</label>
        <Select id="designer-report-reason" placeholder="Select a reason" value={reason || undefined}
          onChange={setReason} options={REPORT_REASONS} style={{ width: '100%' }} />
      </div>
      <div className="designer-report-form-field">
        <label htmlFor="designer-report-description">Description *</label>
        <Input.TextArea id="designer-report-description" rows={5} maxLength={2000}
          value={description} onChange={(event) => setDescription(event.target.value)}
          placeholder="Describe your concern without sharing passwords or payment details." />
        <span className="designer-report-help">20–2,000 characters · {description.length} / 2000</span>
      </div>
      {error && <Alert role="alert" type="error" showIcon message={error} />}
      <p className="designer-report-help">Your account identity and report are visible only to authorised User Admins.</p>
    </Modal>
    <Modal open={loginPrompt} title="Sign in to report a designer" okText="Log in"
      onCancel={() => setLoginPrompt(false)} onOk={() => navigate('/login', {
        state: { returnTo: `/designers/${designerId}` },
      })}>
      Only signed-in Renovation Customers can submit designer reports.
    </Modal>
  </div>
}
