export default function ErrorBanner({ message, onRetry, children }) {
  return (
    <div className="error-banner" role="alert">
      <p className="error-message">{message}</p>
      {children}
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  )
}