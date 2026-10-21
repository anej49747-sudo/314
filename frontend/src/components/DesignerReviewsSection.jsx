import { Alert, Button, Spin } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { getPublicDesignerReviews } from '../api/designerReviews.js'
import {
  publicReviewCountLabel,
  publicReviewDate,
  publicReviewDistribution,
  publicReviewRemainingText,
  publicReviewStars,
} from '../designer/designerReviewListRules.js'
import '../designer/designerReviewList.css'

function Stars({ rating }) {
  const stars = publicReviewStars(rating)
  return <span className="customer-review-stars" role="img" aria-label={`${rating} out of 5 stars`}>
    {stars.filled}<span className="customer-review-empty-stars">{stars.empty}</span>
  </span>
}

export default function DesignerReviewsSection({ designerId, refreshKey }) {
  const [response, setResponse] = useState(null)
  const [reviews, setReviews] = useState([])
  const [state, setState] = useState('loading')
  const [retryKey, setRetryKey] = useState(0)
  const [nextPage, setNextPage] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [moreError, setMoreError] = useState('')
  const generation = useRef(0)

  useEffect(() => {
    const current = ++generation.current
    setResponse(null)
    setReviews([])
    setNextPage(1)
    setHasMore(false)
    setLoadingMore(false)
    setMoreError('')
    setState('loading')
    getPublicDesignerReviews(designerId, 0).then((result) => {
      if (current !== generation.current) return
      setResponse(result)
      setReviews(result.reviews)
      setHasMore(result.hasMore)
      setState('ready')
    }).catch(() => {
      if (current === generation.current) setState('error')
    })
    return () => { generation.current += 1 }
  }, [designerId, refreshKey, retryKey])

  async function showMore() {
    if (loadingMore || !hasMore) return
    const current = generation.current
    setLoadingMore(true)
    setMoreError('')
    try {
      const result = await getPublicDesignerReviews(designerId, nextPage)
      if (current !== generation.current) return
      setReviews((existing) => [...existing, ...result.reviews])
      setNextPage((page) => page + 1)
      setHasMore(result.hasMore)
    } catch {
      if (current === generation.current) setMoreError('Unable to load more reviews. Please try again.')
    } finally {
      if (current === generation.current) setLoadingMore(false)
    }
  }

  const count = response?.totalReviews ?? 0
  const distribution = publicReviewDistribution(response)

  return <section className="public-profile-panel customer-reviews-panel" aria-labelledby="customer-reviews-title">
    <div className="public-profile-section-heading customer-reviews-heading">
      <div>
        <h2 id="customer-reviews-title">Customer reviews</h2>
        <p>Feedback shared by renovation customers</p>
      </div>
      {state === 'ready' && <span className="customer-reviews-count">{publicReviewCountLabel(count)}</span>}
    </div>

    {state === 'loading' && <div className="loading-state" role="status">
      <Spin /> <span>Loading customer reviews…</span>
    </div>}
    {state === 'error' && <div className="customer-reviews-load-error" role="alert">
      <div><strong>Unable to load reviews</strong><p>Please try again to view customer feedback.</p></div>
      <Button onClick={() => setRetryKey((value) => value + 1)}>Try again</Button>
    </div>}
    {state === 'ready' && count === 0 && <div className="customer-reviews-empty">
      <span aria-hidden="true" className="customer-reviews-empty-icon">☆</span>
      <h3>No reviews yet</h3>
      <p>Customer ratings and comments will appear here once submitted.</p>
    </div>}
    {state === 'ready' && count > 0 && <>
      <div className="customer-reviews-overview">
        <div className="customer-reviews-overview-left">
          <div className="customer-reviews-score"><strong>{response.averageRating.toFixed(1)}</strong><span>/ 5</span></div>
          <div className="customer-reviews-score-stars">
            <Stars rating={Math.round(response.averageRating)} /> <small>Average rating</small>
          </div>
          <p className="customer-reviews-overview-caption">Based on {publicReviewCountLabel(count)}</p>
        </div>
        <div className="customer-reviews-distribution" aria-label="Distribution of star ratings">
          {distribution.map(({ rating, count: number, percent }) => <div className="customer-reviews-rating-row" key={rating}>
            <span className="customer-reviews-rating-label">{rating} <span aria-hidden="true">★</span></span>
            <div className="customer-reviews-rating-track" role="img" aria-label={`${rating} stars: ${publicReviewCountLabel(number)}`}>
              <div className="customer-reviews-rating-fill" style={{ width: `${percent}%` }} />
            </div>
            <span className="customer-reviews-rating-count">{number}</span>
          </div>)}
        </div>
      </div>
      <h3 className="customer-reviews-list-title">Recent reviews · Newest first</h3>
      <div className="customer-reviews-list" aria-label="Customer reviews, newest first">
        {reviews.map((review, index) => <article className="customer-reviews-item" key={`${review.createdAt}-${index}`}>
          <div className="customer-reviews-item-top">
            <p className="customer-reviews-author">Renovation Customer</p>
            <time className="customer-reviews-date" dateTime={review.createdAt}>{publicReviewDate(review.createdAt)}</time>
          </div>
          <div className="customer-reviews-item-stars">
            <Stars rating={review.rating} /> <span>{review.rating} out of 5</span>
          </div>
          {review.comment ? <p className="customer-reviews-comment">{review.comment}</p>
            : <p className="customer-reviews-no-comment">Rating only · No written comment</p>}
        </article>)}
      </div>
      {moreError && <Alert className="customer-reviews-more-error" role="alert" type="error" showIcon message={moreError} />}
      {hasMore && <Button className="customer-reviews-more" loading={loadingMore} disabled={loadingMore} onClick={showMore}>
        {publicReviewRemainingText(count, reviews.length)}
      </Button>}
      <p className="customer-reviews-shown">Showing {reviews.length} of {count} reviews</p>
    </>}
  </section>
}
