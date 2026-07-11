import { useEffect, useState } from 'react'
import { httpClient } from '../../../api/httpClient'

/**
 * The label endpoint is auth-protected, so a plain <img src="..."> tag can't
 * carry the Authorization header - this fetches it through the same Axios
 * instance (with the interceptor-attached bearer token) and exposes it as an
 * object URL instead.
 */
export function useLabelImage(assetId: string, format: 'png' | 'svg' | 'pdf', size = '50x25') {
  const [url, setUrl] = useState<string | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let objectUrl: string | null = null
    let cancelled = false
    setLoading(true)
    setError(null)

    httpClient
      .get(`/assets/${assetId}/label`, { params: { format, size }, responseType: 'blob' })
      .then((response) => {
        if (cancelled) return
        objectUrl = URL.createObjectURL(response.data as Blob)
        setUrl(objectUrl)
      })
      .catch((err) => {
        if (!cancelled) setError(err)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [assetId, format, size])

  return { url, error, loading }
}
