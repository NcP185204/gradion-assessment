import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import * as api from '../api/client'
import { useAuth } from '../context/AuthContext'
import ErrorBanner from '../components/ErrorBanner'

const STATUS_LABELS = {
  CREATED: 'Draft',
  IN_PROGRESS: 'In progress',
  DONE: 'Done',
}

function statusClass(status) {
  return String(status || 'CREATED').toLowerCase().replace('_', '-')
}

function StepDots({ steps }) {
  return (
    <div className="step-dots" aria-label={`${countDone(steps)} of 5 steps done`}>
      {(steps || []).map((s) => (
        <span
          key={s.stepNumber}
          className={`dot ${s.status.toLowerCase()}`}
          title={`Step ${s.stepNumber}: ${s.status}`}
        />
      ))}
    </div>
  )
}

function countDone(steps) {
  return (steps || []).filter((s) => s.status === 'DONE').length
}

export default function ProjectListPage() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  const [projects, setProjects] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    api
      .listProjects()
      .then((data) => {
        if (!cancelled) setProjects(data)
      })
      .catch((err) => {
        if (!cancelled) setError(err?.response?.data?.message || err.message || 'Failed to load projects')
      })
    return () => {
      cancelled = true
    }
  }, [])

  function handleSignOut() {
    signOut()
    navigate('/login')
  }

  return (
    <main className="page">
      <header className="page-header">
        <div>
          <h1>Your projects</h1>
          <p className="muted">Signed in as {user?.name} ({user?.email})</p>
        </div>
        <div className="header-actions">
          <Link to="/projects/new" className="btn btn-primary">
            New project
          </Link>
          <button type="button" className="btn btn-ghost" onClick={handleSignOut}>
            Sign out
          </button>
        </div>
      </header>

      {error && (
        <ErrorBanner message={error} onRetry={() => window.location.reload()} />
      )}

      {projects === null && !error && <p className="muted">Loading your projects…</p>}

      {projects !== null && projects.length === 0 && (
        <div className="empty-state">
          <h2>No projects yet</h2>
          <p>Create your first project by pasting or uploading a book's text.</p>
          <Link to="/projects/new" className="btn btn-primary">
            Create a project
          </Link>
        </div>
      )}

      {projects !== null && projects.length > 0 && (
        <ul className="project-list">
          {projects.map((p) => (
            <li key={p.id}>
              <Link to={`/projects/${p.id}`} className="project-row">
                <div className="project-info">
                  <h2 className="project-title">{p.title}</h2>
                  <p className="muted">{new Date(p.createdAt).toLocaleDateString()}</p>
                </div>
                <div className="project-meta">
                  <StepDots steps={p.steps} />
                  <span className={`status-pill ${statusClass(p.overallStatus)}`}>
                    {STATUS_LABELS[p.overallStatus] || p.overallStatus}
                  </span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}