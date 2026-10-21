import { Alert, Button, Input, Modal, Rate } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentSession } from '../api/authentication.js'
import { submitDesignerReview } from '../api/designerReviews.js'
import {
  buildReviewPayload,
  MAX_REVIEW_COMMENT_LENGTH,
  reviewRatingLabel,
  reviewSubmitError,
  validReviewRating,
} from '../designer/designerReviewRules.js'
import '../designer/designerReview.css'

export default function DesignerReviewAction({ designerId, designerName, onSubmitted }) {
  const navigate = useNavigate()
  const [access, setAccess] = useState('loading')
  const [reviewOpen, setReviewOpen] = useState(false)
  const [loginOpen, setLoginOpen] = useState(false)
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  const [attempted, setAttempted] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let active = true
    setAccess('loading')
    currentSession().then((account) => {
      if (active) setAccess(account.role === 'CUSTOMER' ? 'customer' : 'other')
    }).catch((failure) => {
      if (active) setAccess(failure.response?.status === 401 ? 'guest' : 'error')
    })
    return () => { active = false }
  }, [designerId])

  function openReview() {
    if (access === 'loading') return
    if (access === 'guest') { setLoginOpen(true); return }
    if (access === 'error') {
      setError('Unable to check your customer session. Please refresh the page and try again.')
      return
    }
    if (access !== 'customer') {
      setError('Only signed-in Renovation Customers can submit reviews.')
      return
    }
    setRating(0)
    setComment('')
    setAttempted(false)
    setError('')
    setReviewOpen(true)
  }

  function closeReview() {
    if (submitting) return
    setReviewOpen(false)
    setError('')
  }

  async function submit() {
    if (submitting) return
    setAttempted(true)
    const values = buildReviewPayload(rating, comment)
    if (!values) return
    setSubmitting(true)
    setError('')
    try {
      await submitDesignerReview(designerId, values)
      onSubmitted(`Your ${rating}-star review was submitted successfully.`)
      setReviewOpen(false)
    } catch (failure) {
      if (failure.response?.status === 401) {
        setAccess('guest')
        setReviewOpen(false)
        setLoginOpen(true)
      } else {
        if (failure.response?.status === 403) setAccess('other')
        setError(reviewSubmitError(failure.response?.status, failure.response?.data?.message))
      }
    } finally {
      setSubmitting(false)
    }
  }

  const commentTooLong = comment.length > MAX_REVIEW_COMMENT_LENGTH
  const missingRating = attempted && !validReviewRating(rating)

  return <>
    {access !== 'other' && <div className="designer-review-action">
      <Button type="primary" onClick={openReview} disabled={access === 'loading'}>
        <span aria-hidden="true">☆</span> {access === 'guest' ? 'Log in to write a review' : 'Write a review'}
      </Button>
      <span className="designer-review-action-note">For renovation customers</span>
      {error && !reviewOpen && <Alert role="alert" type="error" showIcon message={error} />}
    </div>}
    <Modal className="designer-review-modal" width={520} open={reviewOpen} destroyOnHidden
      title={null} onCancel={closeReview} closable={!submitting} maskClosable={!submitting}
      okText="Submit review" okButtonProps={{ loading: submitting, disabled: submitting }}
      onOk={submit} cancelButtonProps={{ disabled: submitting }}>
      <div className="designer-review-modal-header">
        <p className="designer-review-modal-eyebrow">Customer review</p>
        <h2>Review this designer</h2>
        <p className="designer-review-modal-subtitle">Share your experience with <strong>{designerName}</strong>.</p>
      </div>
      <hr className="designer-review-divider" />
      {error && <Alert role="alert" type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
      <p className="designer-review-field-label" id="designer-review-rating-label">Your rating <span className="designer-review-required">*</span></p>
      <Rate className="designer-review-stars" value={rating} onChange={(value) => { setRating(value); setError('') }}
        aria-labelledby="designer-review-rating-label" tooltips={['Poor', 'Fair', 'Good', 'Very good', 'Excellent']} />
      <p className="designer-review-rating-feedback" aria-live="polite">
        {validReviewRating(rating) ? `${rating} out of 5 — ${reviewRatingLabel(rating)}` : 'Select a rating from 1 to 5 stars.'}
      </p>
      {missingRating && <p className="designer-review-error-text" role="alert">Please select a star rating before submitting.</p>}
      <label className="designer-review-field-label" htmlFor="designer-review-comment">
        Written review <span className="designer-review-optional">(optional)</span>
      </label>
      <Input.TextArea className="designer-review-comment" id="designer-review-comment" rows={4}
        value={comment} onChange={(event) => setComment(event.target.value)}
        placeholder="What would you like to share about this designer?" aria-invalid={commentTooLong} />
      <p className="designer-review-help">You can submit a rating without writing a comment. Maximum {MAX_REVIEW_COMMENT_LENGTH} characters.</p>
      {commentTooLong && <p className="designer-review-error-text" role="alert">Comment must be {MAX_REVIEW_COMMENT_LENGTH} characters or fewer.</p>}
    </Modal>
    <Modal open={loginOpen} onCancel={() => setLoginOpen(false)} destroyOnHidden
      title="Log in to leave a review" footer={<>
        <Button onClick={() => setLoginOpen(false)}>Cancel</Button>
        <Button type="primary" onClick={() => navigate('/login', { state: { returnTo: `/designers/${designerId}` } })}>Log in</Button>
      </>}>
      <p>Log in with a Renovation Customer account to submit a designer review.</p>
    </Modal>
  </>
}
