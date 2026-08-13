import axios from 'axios'

const API_BASE = '/api'
const TOKEN_KEY = 'gradion_token'
const USER_KEY = 'gradion_user'

const client = axios.create({
  baseURL: API_BASE,
})

// Attach the JWT to every request.
client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// On 401 (expired/invalid token), clear session and go to login.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearSession()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export function saveSession({ token, user }) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

// ---------------------------------------------------------------------------
// Auth
// ---------------------------------------------------------------------------

export async function login(name, email) {
  const { data } = await client.post('/auth/login', { name, email })
  saveSession(data)
  return data
}

// ---------------------------------------------------------------------------
// Projects
// ---------------------------------------------------------------------------

export async function listProjects() {
  const { data } = await client.get('/projects')
  return data
}

export async function getProject(id) {
  const { data } = await client.get(`/projects/${id}`)
  return data
}

export async function createProject({ title, bookText, file }) {
  if (file) {
    const form = new FormData()
    form.append('title', title)
    form.append('file', file)
    const { data } = await client.post('/projects', form)
    return data
  }
  const { data } = await client.post('/projects', { title, bookText })
  return data
}

// ---------------------------------------------------------------------------
// Pipeline steps
// ---------------------------------------------------------------------------

export async function runStep(projectId, stepNumber, customStyle) {
  const { data } = await client.post(`/projects/${projectId}/steps/${stepNumber}/run`, {
    customStyle: customStyle || null,
  })
  return data
}

export async function retryStep(projectId, stepNumber) {
  const { data } = await client.post(`/projects/${projectId}/steps/${stepNumber}/retry`)
  return data
}

export async function resetStuckStep(projectId, stepNumber) {
  const { data } = await client.post(`/projects/${projectId}/steps/${stepNumber}/reset-stuck`)
  return data
}

// ---------------------------------------------------------------------------
// Files (generated images)
// ---------------------------------------------------------------------------

export function imageUrl(projectId, relativePath) {
  if (!relativePath) return null
  return `${API_BASE}/files/${projectId}/${relativePath}`
}