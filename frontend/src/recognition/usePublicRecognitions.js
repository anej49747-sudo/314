import { useEffect, useState } from 'react'
import { getPublicRecognitions } from '../api/recognitions.js'

export function usePublicRecognitions() {
  const [recognitions, setRecognitions] = useState({ featuredProject: null, awards: [] })
  useEffect(() => {
    const controller = new AbortController()
    getPublicRecognitions(controller.signal).then((data) => {
      if (!controller.signal.aborted) setRecognitions(data)
    }).catch(() => {
      // Recognition is supplementary: public browsing remains usable during an API failure.
    })
    return () => controller.abort()
  }, [])
  return recognitions
}
