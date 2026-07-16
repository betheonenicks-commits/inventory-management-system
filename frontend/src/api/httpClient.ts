import axios, { type AxiosError } from 'axios'
import { useAuthStore } from '../auth/authStore'
import { type ApiProblem, isApiProblem } from './errors'

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
})

httpClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const data = error.response?.data
    if (error.response?.status === 401) {
      useAuthStore.getState().clearSession()
      if (window.location.pathname !== '/login') {
        // US-NTF-10: carry the intended destination through login so an
        // expired session never dead-ends a deep link on the homepage.
        const next = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?next=${next}`
      }
    }
    if (isApiProblem(data)) {
      return Promise.reject(data as ApiProblem)
    }
    return Promise.reject(error)
  },
)
