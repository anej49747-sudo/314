import { Link, Outlet, useLocation } from 'react-router-dom'

export default function AuthLayout() {
  const { pathname } = useLocation()
  return (
    <div className="site">
      <header className="site-header">
        <div className="site-header-inner">
          <span className="brand">Renovation Matching Platform</span>
          <nav aria-label="Account navigation">
            {pathname === '/customer' || pathname.startsWith('/admin/')
              ? <span className="header-link">Account area</span>
              : <Link to="/login" className="header-link">Log in</Link>}
          </nav>
        </div>
      </header>
      <main className="auth-main"><Outlet /></main>
    </div>
  )
}
