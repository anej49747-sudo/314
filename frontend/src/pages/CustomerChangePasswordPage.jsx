import { Alert, Button, Form, Input, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import { updateOwnAccountPassword } from '../api/accountSecurity.js'
import { validatePasswordChange, passwordChangeFailureMessage } from '../designer/designerLoginRules.js'
import AuthCard from '../components/AuthCard.jsx'

/** Voluntary customer password update; current password is verified server-side. */
export default function CustomerChangePasswordPage() {
  const navigate = useNavigate()
  const [ready,setReady] = useState(false)
  const [loading,setLoading] = useState(true)
  const [saving,setSaving] = useState(false)
  const [error,setError] = useState('')
  useEffect(() => {
    let live=true
    currentCustomer().then(account=> { if(live && account.role==='CUSTOMER') setReady(true) })
      .catch(err=>{
        if (!live) return
        if([401,403].includes(err.response?.status)) navigate('/login',{replace:true})
        else setError('Unable to verify your customer account.')
      }).finally(()=>{if(live)setLoading(false)})
    return ()=>{live=false}
  },[navigate])
  async function submit(values) {
    const invalid=validatePasswordChange(values)
    if(invalid){setError(invalid);return}
    setSaving(true);setError('')
    try {
      await updateOwnAccountPassword({currentPassword:values.currentPassword,newPassword:values.newPassword})
      navigate('/customer/security',{replace:true,state:{passwordChanged:true}})
    }catch(err){setError(passwordChangeFailureMessage(err))}finally{setSaving(false)}
  }
  return <AuthCard title="Change password" description="Verify your current password before setting a new one.">
    {loading && <div className="loading-state"><Spin/> Verifying account…</div>}
    {error && <Alert type="error" role="alert" showIcon message={error} className="form-feedback"/>}
    {ready && <Form layout="vertical" onFinish={submit} disabled={saving} autoComplete="off">
      <Form.Item name="currentPassword" label="Current password" rules={[{required:true,message:'Enter your current password'}]}>
        <Input.Password autoComplete="current-password"/>
      </Form.Item>
      <Form.Item name="newPassword" label="New password" rules={[{required:true,message:'Enter a new password'},
        {validator:(_,v)=> {const issue=v?validatePasswordChange({newPassword:v,confirmPassword:v}):null
          return issue?Promise.reject(new Error(issue)):Promise.resolve()}}]}>
        <Input.Password autoComplete="new-password"/>
      </Form.Item>
      <p className="form-helper">Use 10–128 characters, including uppercase, lowercase, numbers and a special character.</p>
      <Form.Item name="confirmPassword" label="Confirm new password" dependencies={['newPassword']}
        rules={[{required:true,message:'Confirm your new password'},({getFieldValue})=>({validator:(_,v)=>
          v===getFieldValue('newPassword')?Promise.resolve():Promise.reject(new Error('Passwords do not match'))})]}>
        <Input.Password autoComplete="new-password"/>
      </Form.Item>
      <Button type="primary" block htmlType="submit" loading={saving}>Change password</Button>
      <Link className="ld-password-back-link" to="/customer/security">Back to account security</Link>
    </Form>}
  </AuthCard>
}
