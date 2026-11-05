/** Customer routes share one section highlight, even on nested account screens. */
export function isCustomerWorkspacePath(pathname) {
  return pathname === '/customer' || pathname.startsWith('/customer/')
}

/** Visitors may register; authenticated accounts should never see a sign-up prompt. */
export function homeFooterAction(sessionStatus, destination) {
  if (sessionStatus === 'guest') return { to: '/register', label: 'Create your free account' }
  if (destination) return {
    to: destination,
    label: destination === '/customer' || destination === '/designer' ? 'My dashboard' : 'My workspace',
  }
  return { to: '/projects', label: 'Browse projects' }
}
