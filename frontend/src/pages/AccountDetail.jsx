import { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { getAccount, getTransactions, logout } from '../api/client'
import './AccountDetail.css'

export default function AccountDetail() {
  const { id } = useParams()
  const [account, setAccount] = useState(null)
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    Promise.all([getAccount(id), getTransactions(id)])
      .then(([acc, tx]) => {
        setAccount(acc)
        setTransactions(tx || [])
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  if (loading) return <div className="page-loading">Chargement…</div>
  if (error || !account) return <div className="page-error">{error || 'Compte introuvable'}</div>

  return (
    <div className="account-detail-page">
      <header className="app-header">
        <h1>Détail du compte</h1>
        <div className="header-actions">
          <Link to="/accounts" className="back-link">← Mes comptes</Link>
          <button type="button" className="logout-btn" onClick={handleLogout}>
            Déconnexion
          </button>
        </div>
      </header>
      <main className="account-detail-main">
        <div className="account-summary">
          <h2>{account.label}</h2>
          <p className="iban">{account.iban}</p>
          <p className="balance">
            Solde : <strong>{formatMoney(account.balance)} {account.currency}</strong>
          </p>
        </div>
        <section className="transactions-section">
          <h3>Historique des transactions</h3>
          {transactions.length === 0 ? (
            <p className="empty">Aucune transaction.</p>
          ) : (
            <table className="transactions-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Description</th>
                  <th>Montant</th>
                  <th>Solde après</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => (
                  <tr key={tx.id}>
                    <td>{formatDate(tx.occurredAt)}</td>
                    <td>{tx.description}</td>
                    <td className={tx.amount >= 0 ? 'amount-pos' : 'amount-neg'}>
                      {tx.amount >= 0 ? '+' : ''}{formatMoney(tx.amount)}
                    </td>
                    <td>{formatMoney(tx.balanceAfter)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
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

function formatDate(isoString) {
  return new Date(isoString).toLocaleString('fr-FR', {
    dateStyle: 'short',
    timeStyle: 'short',
  })
}
