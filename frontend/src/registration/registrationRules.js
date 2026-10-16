export const SALUTATIONS = [
  { label: 'Mr', value: 'Mr' },
  { label: 'Mrs', value: 'Mrs' },
  { label: 'Ms', value: 'Ms' },
  { label: 'Mdm', value: 'Mdm' },
  { label: 'Mx', value: 'Mx' },
  { label: 'Dr', value: 'Dr' },
  { label: 'Prof', value: 'Prof' },
]

// Frontend guidance only; the Spring Boot service enforces the policy again.
export function validatePassword(value) {
  if (typeof value !== 'string' || [...value].length < 10 || [...value].length > 128) {
    return 'Use 10–128 characters.'
  }
  if (!/[A-Z]/.test(value)) return 'Include at least one uppercase letter.'
  if (!/[a-z]/.test(value)) return 'Include at least one lowercase letter.'
  if (!/[0-9]/.test(value)) return 'Include at least one number.'
  if (!/[\p{P}\p{S}]/u.test(value)) return 'Include at least one special character.'
  return null
}

export function buildRegistrationPayload(values) {
  return {
    salutation: values.salutation || null,
    name: values.name.trim(),
    email: values.email.trim(),
    password: values.password, // Never trim or modify passwords.
  }
}
