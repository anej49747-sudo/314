import { Alert, Button } from 'antd'
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

  useEffect(() => {
    let active = true
    currentSession().then((account) => {
      if (active) setSession({ status: 'authenticated', account })
    }).catch((failure) => {
      if (active) setSession({ status: headerSessionFailure(failure.response?.status), account: null })
    })
    return () => { active = false }
  }, [pathname, refreshKey])

  const account = session.status === 'authenticated' ? session.account : null
  const accountPath = accountAreaPath(account)
  const isCustomer = account?.role === 'CUSTOMER'

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

  return (
    <div className="site">
      <header className="site-header">
        <div className="site-header-inner">
          <span className="brand">Renovation Matching Platform</span>
          <nav className="site-nav" aria-label="Main navigation">
            <Link to="/designers" className="header-link"
              aria-current={pathname.startsWith('/designers') ? 'page' : undefined}>
              Interior designers
            </Link>
            <Link to="/projects" className="header-link"
              aria-current={pathname === '/projects' ? 'page' : undefined}>
              Explore projects
            </Link>
            {isCustomer && (
              <Link to="/customer/compare" className="header-link"
                aria-current={pathname === '/customer/compare' ? 'page' : undefined}>
                Compare designers
              </Link>
            )}
            {isCustomer && (
              <Link to="/customer/favourites" className="header-link"
                aria-current={pathname === '/customer/favourites' ? 'page' : undefined}>
                My Favourites
              </Link>
            )}
            {accountPath ? <>
              <Link to={accountPath} className="header-link"
                aria-current={pathname === accountPath ? 'page' : undefined}>Account area</Link>
              <Button type="text" className="header-link site-logout-link"
                onClick={onLogout} loading={loggingOut} disabled={loggingOut}>Log out</Button>
            </> : session.status === 'guest' ? <>
              <Link to="/login" className="header-link">Log in</Link>
              <Link to="/register" className="header-link site-register-link">Create account</Link>
            </> : session.status === 'error' ? (
              <Button type="link" className="header-link" onClick={() => setRefreshKey((key) => key + 1)}>
                Retry account status
              </Button>
            ) : null}
          </nav>
        </div>
        {logoutError && <Alert className="site-logout-error" type="error" showIcon role="alert"
          message={logoutError} closable onClose={() => setLogoutError('')} />}
      </header>
      <main className="auth-main"><Outlet /></main>
    </div>
  )
}
