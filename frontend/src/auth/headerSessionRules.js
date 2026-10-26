// The shared header should follow the server session, not the current URL.
export function accountAreaPath(account) {
  if (!account) return null
  switch (account.role) {
    case 'CUSTOMER': return '/customer'
    case 'INTERIOR_DESIGNER':
      return account.passwordChangeRequired ? '/designer/change-password' : '/designer'
    case 'USER_ADMIN': return '/admin/accounts'
    case 'PLATFORM_MANAGER': return '/manager'
    default: return null
  }
}

export function headerSessionFailure(status) {
  return status === 401 ? 'guest' : 'error'
}
