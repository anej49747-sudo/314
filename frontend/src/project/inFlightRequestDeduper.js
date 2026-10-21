/**
 * Share a pending request for the same key. Once settled, allow later requests;
 * the backend continues to decide whether a visit counts in that HTTP session.
 */
export function createInFlightRequestDeduper() {
  const inFlight = new Map()

  return (key, load) => {
    const existing = inFlight.get(key)
    if (existing) return existing

    const pending = Promise.resolve().then(load)
    inFlight.set(key, pending)

    const release = () => {
      if (inFlight.get(key) === pending) inFlight.delete(key)
    }
    // Attach both settlement handlers so a failed load can be retried.
    pending.then(release, release)
    return pending
  }
}
