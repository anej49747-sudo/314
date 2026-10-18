import { Form, Input, Select } from 'antd'
import { SALUTATIONS, validatePassword } from '../registration/registrationRules.js'

// Reusable inputs for public customer signup and later authorised account provisioning.
// The server, not these form fields, controls the account role.
export default function AccountIdentityFields({ includePassword = true }) {
  return (
    <>
      <Form.Item name="salutation" label="Salutation (optional)">
        <Select
          placeholder="Select a title (optional)"
          allowClear
          options={SALUTATIONS}
          aria-label="Salutation (optional)"
        />
      </Form.Item>
      <Form.Item name="name" label="Name" required rules={[
        { required: true, whitespace: true, message: 'Enter your name or company name.' },
        { max: 160, message: 'Name must be at most 160 characters.' },
      ]}>
        <Input autoComplete="name" maxLength={160} placeholder="Your name or company name" />
      </Form.Item>
      <Form.Item name="email" label="Email" required rules={[
        { required: true, whitespace: true, message: 'Enter your email address.' },
        { type: 'email', message: 'Enter a valid email address.' },
        { max: 254, message: 'Email must be at most 254 characters.' },
      ]}>
        <Input type="email" autoComplete="email" maxLength={254} placeholder="name@example.com" />
      </Form.Item>
      {includePassword && (
        <>
          <Form.Item name="password" label="Password" required
            extra="10–128 characters, including uppercase, lowercase, number and special character." rules={[
              { required: true, message: 'Create a password.' },
              { validator: (_, value) => {
                const feedback = validatePassword(value)
                return feedback ? Promise.reject(new Error(feedback)) : Promise.resolve()
              } },
            ]}>
            <Input.Password autoComplete="new-password" placeholder="Create password" />
          </Form.Item>
          <Form.Item name="confirmPassword" label="Confirm password" required
            dependencies={['password']} rules={[
              { required: true, message: 'Confirm your password.' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  return value === getFieldValue('password')
                    ? Promise.resolve()
                    : Promise.reject(new Error('Passwords do not match.'))
                },
              }),
            ]}>
            <Input.Password autoComplete="new-password" placeholder="Confirm password" />
          </Form.Item>
        </>
      )}
    </>
  )
}
