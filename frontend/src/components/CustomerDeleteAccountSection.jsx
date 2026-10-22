import { Alert, Button, Card, Input, Modal, message } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { deleteOwnCustomerAccount } from '../api/accountDeletion.js'
import { accountDeletionError, canPermanentlyDelete } from '../account/accountDeletionRules.js'
import '../account/accountDeletion.css'

export default function CustomerDeleteAccountSection() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  function openDialog() {
    setConfirmation('')
    setError('')
    setOpen(true)
  }

  async function confirmDelete() {
    if (busy || !canPermanentlyDelete(confirmation)) return
    setBusy(true)
    setError('')
    try {
      await deleteOwnCustomerAccount()
      setOpen(false)
      message.success('Your account was permanently deleted.')
      navigate('/login', { replace: true })
    } catch (failure) {
      setError(accountDeletionError(failure))
      if ([401, 403].includes(failure.response?.status)) {
        setOpen(false)
        navigate('/login', { replace: true })
      }
    } finally {
      setBusy(false)
    }
  }

  return <>
    <Card className="account-deletion-panel" title="Delete account">
      <p>Remove your Customer account permanently. Your saved projects, reviews, reports and
        personally linked login history will be removed.</p>
      <p>Anonymous platform activity totals will remain so historical reports continue to work.
        This action cannot be undone.</p>
      <Button danger type="primary" onClick={openDialog}>Delete my account</Button>
    </Card>
    <Modal open={open} title="Permanently delete your account?" okText="Permanently delete account"
      okButtonProps={{ danger: true, disabled: !canPermanentlyDelete(confirmation) }}
      okType="primary" confirmLoading={busy} closable={!busy}
      onCancel={() => { if (!busy) setOpen(false) }} onOk={confirmDelete}>
      <p>This deletes your Customer account and personal records, and signs you out.
        Your saved projects and reviews cannot be recovered.</p>
      <p>Anonymous historical platform activity statistics will remain.</p>
      <label className="account-deletion-confirm-label" htmlFor="customer-delete-confirm">
        Type <strong>DELETE</strong> to confirm
      </label>
      <Input id="customer-delete-confirm" autoComplete="off" value={confirmation}
        onChange={(event) => setConfirmation(event.target.value)} disabled={busy} />
      {error && <Alert role="alert" type="error" showIcon message={error} className="account-deletion-error" />}
    </Modal>
  </>
}
