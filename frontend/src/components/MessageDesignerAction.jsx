import { Alert, Button, Modal } from 'antd'
import { MessageSquare } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentSession } from '../api/authentication.js'

export default function MessageDesignerAction({ designerId, designerName }) {
  const navigate = useNavigate()
  const [access, setAccess] = useState('loading')
  const [loginPrompt, setLoginPrompt] = useState(false)

  useEffect(() => {
    let active = true
    setAccess('loading')
    currentSession().then((account) => {
      if (active) setAccess(account.role === 'CUSTOMER' ? 'customer' : 'other')
    }).catch((failure) => {
      if (active) setAccess(failure.response?.status === 401 ? 'guest' : 'error')
    })
    return () => { active = false }
  }, [designerId])

  const destination = `/customer/messages?designerId=${designerId}`
  function contact() {
    if (access === 'customer') navigate(destination)
    else if (access === 'guest') setLoginPrompt(true)
  }

  if (access === 'other') return null
  return <>
    <Button type="primary" icon={<MessageSquare size={17} />} onClick={contact}
      disabled={access === 'loading' || access === 'error'}>
      Message designer
    </Button>
    {access === 'error' && <Alert type="warning" showIcon role="alert"
      message="Unable to check your session. Refresh the page to try messaging." />}
    <Modal open={loginPrompt} onCancel={() => setLoginPrompt(false)}
      title="Log in to message a designer" footer={<>
        <Button onClick={() => setLoginPrompt(false)}>Cancel</Button>
        <Button type="primary" onClick={() => navigate('/login', { state: { returnTo: destination } })}>
          Log in
        </Button>
      </>}>
      <p>Log in with a Renovation Customer account to send a private message to {designerName}.</p>
    </Modal>
  </>
}
