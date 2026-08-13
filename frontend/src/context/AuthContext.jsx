import { createContext, useContext, useState, useMemo } from 'react'
import * as api from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => api.getStoredUser())

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      async signIn(name, email) {
        const data = await api.login(name, email)
        setUser(data.user)
        return data
      },
      signOut() {
        api.clearSession()
        setUser(null)
      },
    }),
    [user]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}