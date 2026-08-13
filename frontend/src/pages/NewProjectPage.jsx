import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import * as api from '../api/client'
import ErrorBanner from '../components/ErrorBanner'

export default function NewProjectPage() {
  const navigate = useNavigate()
  const fileRef = useRef(null)
  const [title, setTitle] = useState('')
  const [bookText, setBookText] = useState('')
  const [file, setFile] = useState(null)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)

    if (!title.trim()) {
      setError('Please enter a project title.')
      return
    }
    if (!file && !bookText.trim()) {
      setError('Please paste the book text or upload a .txt file.')
      return
    }

    setSubmitting(true)
    try {
      const created = await api.createProject({
        title: title.trim(),
        bookText: file ? undefined : bookText,
        file: file || undefined,
      })
      navigate(`/projects/${created.id}`)
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to create project.')
    } finally {
      setSubmitting(false)
    }
  }

  function handleFileChange(e) {
    const selected = e.target.files?.[0]
    setFile(selected || null)
    if (selected) setBookText('')
  }

  return (
    <main className="page narrow">
      <header className="page-header">
        <h1>New project</h1>
      </header>

      <form onSubmit={handleSubmit} className="form-card">
        <div className="field">
          <label htmlFor="title">Project title</label>
          <input
            id="title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="The Wind in the Willows"
            required
          />
        </div>

        <div className="field">
          <label htmlFor="file">Upload a .txt file</label>
          <input
            id="file"
            ref={fileRef}
            type="file"
            accept=".txt,text/plain"
            onChange={handleFileChange}
          />
          {file && <p className="muted">Selected: {file.name}</p>}
        </div>

        <div className="field">
          <label htmlFor="bookText">…or paste the book text</label>
          <textarea
            id="bookText"
            value={bookText}
            onChange={(e) => {
              setBookText(e.target.value)
              if (file) setFile(null)
            }}
            rows={12}
            placeholder="Paste the full book text here…"
            disabled={Boolean(file)}
          />
        </div>

        {error && <ErrorBanner message={error} />}

        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={() => navigate('/projects')}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create project'}
          </button>
        </div>
      </form>
    </main>
  )
}
