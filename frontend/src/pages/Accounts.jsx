import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getAccounts, logout } from '../api/client'
import './Accounts.css'

export default function Accounts() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    getAccounts()
      .then(setAccounts)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  if (loading) return <div className="page-loading">Chargement des comptes…</div>
  if (error) return <div className="page-error">{error}</div>

  return (
    <div className="accounts-page">
      <header className="app-header">
        <h1>Mes comptes</h1>
        <button type="button" className="logout-btn" onClick={handleLogout}>
          Déconnexion
        </button>
      </header>
      <main className="accounts-list">
        {accounts.length === 0 ? (
          <p className="empty">Aucun compte.</p>
        ) : (
          <ul>
            {accounts.map((acc) => (
              <li key={acc.id}>
                <Link to={`/accounts/${acc.id}`} className="account-card">
                  <span className="account-label">{acc.label}</span>
                  <span className="account-iban">{acc.iban}</span>
                  <span className="account-balance">
                    {formatMoney(acc.balance)} {acc.currency}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </main>
    </div>
  )
}

function formatMoney(value) {
  return new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}
