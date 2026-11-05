import CustomerReturnButton from './CustomerReturnButton.jsx'

/** Shared outlined workspace navigation, with optional unsaved-edit confirmation. */
export default function DesignerReturnButton({
  to = '/designer', children = 'Back to dashboard', confirmLeave = false,
}) {
  return <CustomerReturnButton to={to} confirmLeave={confirmLeave}>{children}</CustomerReturnButton>
}
