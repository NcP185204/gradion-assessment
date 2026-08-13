import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../context/AuthContext'
import ProjectListPage from '../pages/ProjectListPage'
import * as api from '../api/client'

vi.mock('../api/client', () => ({
  listProjects: vi.fn(),
  getStoredUser: vi.fn(() => ({ name: 'Test', email: 'test@example.com' })),
  clearSession: vi.fn(),
  saveSession: vi.fn(),
}))

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <ProjectListPage />
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('ProjectListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows empty state when there are no projects', async () => {
    api.listProjects.mockResolvedValue([])
    renderPage()
    expect(await screen.findByText(/No projects yet/i)).toBeInTheDocument()
  })

  it('shows projects with status pills when present', async () => {
    api.listProjects.mockResolvedValue([
      {
        id: 1,
        title: 'The Wind in the Willows',
        overallStatus: 'IN_PROGRESS',
        createdAt: '2026-01-01T00:00:00',
        steps: [
          { stepNumber: 1, status: 'DONE' },
          { stepNumber: 2, status: 'RUNNING' },
          { stepNumber: 3, status: 'PENDING' },
          { stepNumber: 4, status: 'PENDING' },
          { stepNumber: 5, status: 'PENDING' },
        ],
      },
    ])
    renderPage()
    expect(await screen.findByText('The Wind in the Willows')).toBeInTheDocument()
    expect(screen.getByText('In progress')).toBeInTheDocument()
  })

  it('shows an error banner when loading fails', async () => {
    api.listProjects.mockRejectedValue({ response: { data: { message: 'boom' } } })
    renderPage()
    expect(await screen.findByText('boom')).toBeInTheDocument()
  })
})
