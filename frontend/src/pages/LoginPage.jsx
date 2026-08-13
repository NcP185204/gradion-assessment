import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)

    const trimmedName = name.trim()
    const trimmedEmail = email.trim()
    if (!trimmedName) {
      setError('Please enter your name.')
      return
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      setError('Please enter a valid email address.')
      return
    }

    setSubmitting(true)
    try {
      await signIn(trimmedName, trimmedEmail)
      navigate('/projects')
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Sign in failed. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-card">
        <h1 className="auth-title">Book Illustrator</h1>
        <p className="auth-subtitle">
          Turn a book into character portraits and chapter illustrations with Gemini.
        </p>
        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="name">Name</label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ada Lovelace"
              autoComplete="name"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="ada@example.com"
              autoComplete="email"
              required
            />
          </div>
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Continue'}
          </button>
        </form>
      </div>
    </main>
  )
}