export default function AuthCard({ title, description, children }) {
  return (
    <section className="auth-card" aria-labelledby="auth-heading">
      <h1 id="auth-heading">{title}</h1>
      {description && <p className="auth-description">{description}</p>}
      {children}
    </section>
  )
}
