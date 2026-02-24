import { useState, useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { getMe } from './api/client'
import Login from './pages/Login'
import Accounts from './pages/Accounts'
import AccountDetail from './pages/AccountDetail'

function ProtectedRoute({ children }) {
  const [checking, setChecking] = useState(true)
  const [authenticated, setAuthenticated] = useState(false)

  useEffect(() => {
    getMe()
      .then((user) => setAuthenticated(!!user))
      .catch(() => setAuthenticated(false))
      .finally(() => setChecking(false))
  }, [])

  if (checking) return <div className="page-loading">Vérification de la session…</div>
  if (!authenticated) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/accounts"
          element={
            <ProtectedRoute>
              <Accounts />
            </ProtectedRoute>
          }
        />
        <Route
          path="/accounts/:id"
          element={
            <ProtectedRoute>
              <AccountDetail />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/accounts" replace />} />
        <Route path="*" element={<Navigate to="/accounts" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
