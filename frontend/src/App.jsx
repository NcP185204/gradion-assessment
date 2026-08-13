import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import ProjectListPage from './pages/ProjectListPage'
import NewProjectPage from './pages/NewProjectPage'
import ProjectDetailPage from './pages/ProjectDetailPage'
import './App.css'

function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  return children
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/projects"
            element={
              <RequireAuth>
                <ProjectListPage />
              </RequireAuth>
            }
          />
          <Route
            path="/projects/new"
            element={
              <RequireAuth>
                <NewProjectPage />
              </RequireAuth>
            }
          />
          <Route
            path="/projects/:id"
            element={
              <RequireAuth>
                <ProjectDetailPage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<Navigate to="/projects" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
