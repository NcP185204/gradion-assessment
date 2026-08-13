import { useEffect, useRef, useState, useCallback } from 'react'
import * as api from '../api/client'

/**
 * Polls the project detail while any step is RUNNING.
 *
 * The backend step is async: the HTTP POST returns immediately with RUNNING,
 * then the StepRunner fires Gemini calls and updates the DB. This hook
 * refreshes on a short interval while a step is in-flight and stops once
 * everything settles, so the user sees portraits land one by one instead of
 * one blocking wait.
 */
export function useProjectPolling(projectId, intervalMs = 2000) {
  const [project, setProject] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const timerRef = useRef(null)

  const fetchProject = useCallback(async () => {
    try {
      const data = await api.getProject(projectId)
      setProject(data)
      setError(null)
      return data
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to load project')
      return null
    } finally {
      setLoading(false)
    }
  }, [projectId])

  useEffect(() => {
    let cancelled = false
    setLoading(true)

    async function tick() {
      const data = await fetchProject()
      if (cancelled) return
      const running = data?.steps?.some((s) => s.status === 'RUNNING')
      if (running) {
        timerRef.current = setTimeout(tick, intervalMs)
      }
    }

    tick()
    return () => {
      cancelled = true
      if (timerRef.current) clearTimeout(timerRef.current)
    }
  }, [fetchProject, intervalMs])

  const refresh = useCallback(async () => {
    setLoading(true)
    await fetchProject()
  }, [fetchProject])

  return { project, loading, error, refresh }
}
