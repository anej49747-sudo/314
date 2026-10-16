import { Link, Outlet } from 'react-router-dom'

export default function AuthLayout() {
  return (
    <div className="site">
      <header className="site-header">
        <div className="site-header-inner">
          <span className="brand">Renovation Matching Platform</span>
          <nav aria-label="Account navigation">
            <Link to="/login" className="header-link">Log in</Link>
          </nav>
        </div>
      </header>
      <main className="auth-main">
        <Outlet />
      </main>
    </div>
  )
}
