import { Link, Outlet, useLocation } from 'react-router-dom'

export default function AuthLayout() {
  const { pathname } = useLocation()
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
            {pathname === '/customer' || pathname.startsWith('/admin/') || pathname === '/designer' || pathname.startsWith('/designer/') || pathname.startsWith('/manager/')
              ? <span className="header-link">Account area</span>
              : <>
                  <Link to="/login" className="header-link">Log in</Link>
                  <Link to="/register" className="header-link site-register-link">Create account</Link>
                </>}
          </nav>
        </div>
      </header>
      <main className="auth-main"><Outlet /></main>
    </div>
  )
}
