import { Modal } from 'antd'
import { ArrowLeft } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'

/** Button-shaped return navigation; optionally protect edited account forms. */
export default function CustomerReturnButton({
  to = '/customer', children = 'Back to dashboard', confirmLeave = false,
}) {
  const navigate = useNavigate()
  function handleClick(event) {
    if (!confirmLeave || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
    event.preventDefault()
    Modal.confirm({
      title: 'Discard unsaved changes?',
      content: 'Any changes you made on this page will be lost.',
      okText: 'Discard changes',
      okButtonProps: { danger: true },
      cancelText: 'Stay on page',
      onOk: () => navigate(to),
    })
  }
  return <Link className="ld-customer-return-button" to={to} onClick={handleClick}>
    <ArrowLeft size={17} aria-hidden="true" />
    <span>{children}</span>
  </Link>
}
