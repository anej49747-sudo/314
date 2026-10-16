export function buildLoginPayload(values) {
  return {
    email: String(values.email ?? '').trim().toLowerCase(),
    // Do not normalize or trim passwords; the submitted bytes must match the stored secret.
    password: values.password,
  }
}

export function loginFailureMessage(failure) {
  const status = failure?.response?.status
  if (status === 401) return 'Invalid email or password.'
  if (status === 400) return 'Please enter a valid email and password.'
  if (status === 403) return 'Your security session has expired. Refresh the page and try again.'
  return 'Unable to log in right now. Please try again.'
}
