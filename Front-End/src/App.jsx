import { useState, useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { getMe } from './api/client'
import Login from './pages/Login'
import Accounts from './pages/Accounts'
import AccountDetail from './pages/AccountDetail'
import Transactions from './pages/Transactions'
import Transfer from './pages/Transfer'
import Layout from './components/Layout'

function ProtectedRoute({ children, theme, toggleTheme }) {
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
  return <Layout theme={theme} toggleTheme={toggleTheme}>{children}</Layout>
}

export default function App() {
  const [theme, setTheme] = useState(localStorage.getItem('theme') || 'dark');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/accounts"
          element={
            <ProtectedRoute theme={theme} toggleTheme={toggleTheme}>
              <Accounts />
            </ProtectedRoute>
          }
        />
        <Route
          path="/accounts/:id"
          element={
            <ProtectedRoute theme={theme} toggleTheme={toggleTheme}>
              <AccountDetail />
            </ProtectedRoute>
          }
        />
        <Route
          path="/transactions"
          element={
            <ProtectedRoute theme={theme} toggleTheme={toggleTheme}>
              <Transactions />
            </ProtectedRoute>
          }
        />
        <Route
          path="/transfer"
          element={
            <ProtectedRoute theme={theme} toggleTheme={toggleTheme}>
              <Transfer />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/accounts" replace />} />
        <Route path="*" element={<Navigate to="/accounts" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
