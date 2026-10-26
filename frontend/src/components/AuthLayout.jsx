import { Alert, Button, Dropdown } from 'antd'
import { ChevronDown, House, LogOut, Menu, UserRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { currentSession, logoutSession } from '../api/authentication.js'
import { accountAreaPath, headerSessionFailure } from '../auth/headerSessionRules.js'
import { logoutFailureMessage } from '../auth/logoutRules.js'

export default function AuthLayout() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [session, setSession] = useState({ status: 'loading', account: null })
  const [loggingOut, setLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [navOpen, setNavOpen] = useState(false)

  useEffect(() => {
    let active = true
    currentSession().then((account) => {
      if (active) setSession({ status: 'authenticated', account })
    }).catch((failure) => {
      if (active) setSession({ status: headerSessionFailure(failure.response?.status), account: null })
    })
    return () => { active = false }
  }, [pathname, refreshKey])

  useEffect(() => { setNavOpen(false) }, [pathname])

  const account = session.status === 'authenticated' ? session.account : null
  const accountPath = accountAreaPath(account)
  const isCustomer = account?.role === 'CUSTOMER'
  const isAdmin = account?.role === 'USER_ADMIN'
  const isManager = account?.role === 'PLATFORM_MANAGER'
  const isDesigner = account?.role === 'INTERIOR_DESIGNER'

  async function onLogout() {
    if (loggingOut) return
    setLoggingOut(true)
    setLogoutError('')
    try {
      await logoutSession()
      setSession({ status: 'guest', account: null })
      navigate('/login', { replace: true, state: { justLoggedOut: true } })
    } catch (failure) {
      setLogoutError(logoutFailureMessage(failure))
    } finally {
      setLoggingOut(false)
    }
  }

  // Keep account options intentionally minimal; role landing pages stay in the header.
  const menuItems = account ? [
    { key: 'logout', danger: true, icon: <LogOut size={16} />,
      label: 'Log out', disabled: loggingOut, onClick: onLogout },
  ] : []

  function navLink(to, label, current = pathname === to) {
    return <Link to={to} className="header-link" aria-current={current ? 'page' : undefined}>{label}</Link>
  }

  return <div className="site">
    <header className="site-header">
      <div className="site-header-inner">
        <Link to="/" className="brand brand-link" aria-label="Lucky Dog home">
          <span className="brand-house"><House size={21} strokeWidth={2} aria-hidden="true" /></span>
          Lucky Dog
        </Link>
        <button type="button" className="ui-mobile-nav-toggle" aria-expanded={navOpen}
          aria-controls="lucky-site-nav" aria-label={navOpen ? 'Close navigation' : 'Open navigation'}
          onClick={() => setNavOpen(open => !open)}><Menu size={22} /></button>
        <nav id="lucky-site-nav" className={`site-nav ${navOpen ? 'ui-nav-open' : ''}`} aria-label="Main navigation">
          {!isAdmin && <>
            {navLink('/', 'Home')}
            {navLink('/designers', 'Interior designers', pathname.startsWith('/designers'))}
            {navLink('/projects', 'Explore projects', pathname.startsWith('/projects'))}
            {navLink('/articles', 'Articles', pathname.startsWith('/articles'))}
          </>}
          {isCustomer && navLink('/customer', 'My dashboard', pathname === '/customer' || pathname.startsWith('/customer/'))}
          {isDesigner && navLink('/designer', 'My dashboard', pathname === '/designer' || pathname.startsWith('/designer/'))}
          {isManager && navLink('/manager', 'Management', pathname.startsWith('/manager'))}
          {isAdmin && <>
            {navLink('/admin/reports', 'Designer Reports')}
            {navLink('/admin/accounts', 'Account Management', pathname.startsWith('/admin/accounts') || pathname.startsWith('/admin/designers/new'))}
          </>}
          {accountPath && <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <Button type="text" className="ui-account-menu-trigger" aria-label={`Account menu for ${account.displayName || account.email}`}>
              <span className="ui-header-avatar"><UserRound size={17} aria-hidden="true" /></span>
              <span className="ui-account-trigger-name">{account.displayName || 'My account'}</span>
              <ChevronDown size={15} aria-hidden="true" />
            </Button>
          </Dropdown>}
          {!accountPath && session.status === 'guest' && navLink('/login', 'Log in')}
          {!accountPath && session.status === 'error' && <Button type="link" className="header-link"
            onClick={() => setRefreshKey(key => key + 1)}>Retry account status</Button>}
        </nav>
      </div>
      {logoutError && <Alert className="site-logout-error" type="error" showIcon role="alert"
        message={logoutError} closable onClose={() => setLogoutError('')} />}
    </header>
    <main className="auth-main"><Outlet /></main>
  </div>
}
