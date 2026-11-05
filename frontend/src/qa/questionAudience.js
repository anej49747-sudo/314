// Public project Q&A is readable by every role, but only visitors and
// customers should see the customer-facing "Ask a question" prompt.
export function questionAudience(session) {
  if (session?.role === 'CUSTOMER') return 'customer'
  if (session?.role) return 'other'
  return 'visitor'
}

export function showsQuestionPrompt(audience) {
  return audience === 'customer' || audience === 'visitor'
}
