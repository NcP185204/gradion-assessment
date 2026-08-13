const STEP_LABELS = ['Style', 'Characters', 'Portraits', 'Chapters', 'Illustrations']

function statusFor(step) {
  if (!step) return 'pending'
  return step.status.toLowerCase()
}

export default function Stepper({ steps }) {
  return (
    <ol className="stepper" aria-label="Pipeline progress">
      {STEP_LABELS.map((label, i) => {
        const step = steps?.[i]
        const status = statusFor(step)
        return (
          <li key={label} className={`step ${status}`}>
            <span className="step-dot" aria-hidden="true" />
            <span className="step-label">{label}</span>
            {status === 'running' && (
              <span className="step-status" role="status">
                Running…
              </span>
            )}
            {status === 'failed' && (
              <span className="step-status failed" role="status">
                Failed
              </span>
            )}
          </li>
        )
      })}
    </ol>
  )
}