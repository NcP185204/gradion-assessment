import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import Stepper from '../components/Stepper'
import CharacterCard from '../components/CharacterCard'
import ChapterCard from '../components/ChapterCard'
import ErrorBanner from '../components/ErrorBanner'
import { useProjectPolling } from '../hooks/useProjectPolling'
import * as api from '../api/client'

const STEP_NAMES = {
  1: 'Style',
  2: 'Characters',
  3: 'Portraits',
  4: 'Chapters',
  5: 'Illustrations',
}

function findCurrentStep(project) {
  const steps = project?.steps || []
  for (const s of steps) {
    if (s.status === 'RUNNING' || s.status === 'FAILED') return s
  }
  // Advance to the first non-DONE step.
  for (const s of steps) {
    if (s.status !== 'DONE') return s
  }
  return null
}

export default function ProjectDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const projectId = Number(id)
  const { project, loading, error, refresh } = useProjectPolling(projectId)

  const [customStyle, setCustomStyle] = useState('')
  const [actionError, setActionError] = useState(null)
  const [acting, setActing] = useState(false)

  const steps = project?.steps || []
  const currentStep = findCurrentStep(project)
  const runningStep = steps.find((s) => s.status === 'RUNNING')
  const runningProgress = runningStep?.progressJson ? JSON.parse(runningStep.progressJson) : null

  async function doRunStep(stepNumber, style) {
    setActionError(null)
    setActing(true)
    try {
      await api.runStep(projectId, stepNumber, style)
      await refresh()
    } catch (err) {
      setActionError(err?.response?.data?.message || err.message || 'Failed to run step.')
    } finally {
      setActing(false)
    }
  }

  async function doRetry(stepNumber) {
    setActionError(null)
    setActing(true)
    try {
      await api.retryStep(projectId, stepNumber)
      await refresh()
    } catch (err) {
      setActionError(err?.response?.data?.message || err.message || 'Failed to retry step.')
    } finally {
      setActing(false)
    }
  }

  async function doResetStuck(stepNumber) {
    setActionError(null)
    setActing(true)
    try {
      await api.resetStuckStep(projectId, stepNumber)
      await refresh()
    } catch (err) {
      setActionError(err?.response?.data?.message || err.message || 'Failed to reset stuck step.')
    } finally {
      setActing(false)
    }
  }

  if (loading) {
    return (
      <main className="page narrow">
        <p className="muted">Loading project…</p>
      </main>
    )
  }

  if (error) {
    return (
      <main className="page narrow">
        <ErrorBanner message={error} onRetry={refresh} />
        <Link to="/projects" className="btn btn-ghost">
          Back to projects
        </Link>
      </main>
    )
  }

  return (
    <main className="page">
      <header className="page-header">
        <div>
          <Link to="/projects" className="back-link">
            ← Projects
          </Link>
          <h1>{project.title}</h1>
          <p className="muted">Created {new Date(project.createdAt).toLocaleDateString()}</p>
        </div>
      </header>

      <Stepper steps={steps} />

      {actionError && <ErrorBanner message={actionError} />}

      {/* Running / in-progress state names the specific step */}
      {runningStep && (
        <div className="progress-banner" role="status">
          <span className="spinner" aria-hidden="true" />
          <span>
            Running <strong>{STEP_NAMES[runningStep.stepNumber]}</strong>
            {runningProgress && ` — ${runningProgress.done}/${runningProgress.total} complete`}
          </span>
        </div>
      )}

      {/* Current step action + error/retry + stuck recovery */}
      {currentStep && !runningStep && (
        <section className="action-panel">
          {currentStep.status === 'FAILED' ? (
            <>
              <p className="step-error">
                Step {currentStep.stepNumber} ({STEP_NAMES[currentStep.stepNumber]}) failed.
              </p>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => doRetry(currentStep.stepNumber)}
                disabled={acting}
              >
                Retry this step
              </button>
            </>
          ) : currentStep.stepNumber === 1 ? (
            <div className="style-form">
              <label htmlFor="customStyle">Art style (optional — leave blank to auto-generate)</label>
              <input
                id="customStyle"
                type="text"
                value={customStyle}
                onChange={(e) => setCustomStyle(e.target.value)}
                placeholder="e.g. Watercolor children's book illustration"
              />
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => doRunStep(1, customStyle)}
                disabled={acting}
              >
                {customStyle.trim() ? 'Generate style' : 'Generate style from book'}
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => doRunStep(currentStep.stepNumber)}
              disabled={acting}
            >
              Run {STEP_NAMES[currentStep.stepNumber]}
            </button>
          )}

          {currentStep.status === 'RUNNING' && (
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => doResetStuck(currentStep.stepNumber)}
            >
              Reset stuck step
            </button>
          )}
        </section>
      )}

      {/* Style result */}
      {project.style && (
        <section className="section">
          <h2>Art style</h2>
          <p className="style-text">{project.style}</p>
        </section>
      )}

      {/* Character cards */}
      {project.characters?.length > 0 && (
        <section className="section">
          <h2>Characters</h2>
          <div className="card-grid">
            {project.characters.map((c) => (
              <CharacterCard key={c.id} character={c} projectId={projectId} />
            ))}
          </div>
        </section>
      )}

      {/* Chapter cards */}
      {project.chapters?.length > 0 && (
        <section className="section">
          <h2>Chapters</h2>
          <div className="card-grid">
            {project.chapters.map((c) => (
              <ChapterCard key={c.id} chapter={c} projectId={projectId} />
            ))}
          </div>
        </section>
      )}

      {/* Book text — readable in full at any point */}
      <section className="section">
        <details>
          <summary>Read the full book text</summary>
          <pre className="book-text">{project.bookText}</pre>
        </details>
      </section>
    </main>
  )
}