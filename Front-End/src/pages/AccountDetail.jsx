import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getAccount, getTransactions } from '../api/client'

export default function AccountDetail() {
  const { id } = useParams()
  const [account, setAccount] = useState(null)
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([getAccount(id), getTransactions(id)])
      .then(([acc, tx]) => {
        setAccount(acc)
        setTransactions(tx?.content || tx || []) // Handle Page object or List
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="page-loading">Analyse des mouvements…</div>
  if (error || !account) return <div className="page-error">{error || 'Compte introuvable'}</div>

  return (
    <div className="account-detail-view">
      <Link to="/accounts" style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', display: 'inline-block' }}>
        ← Retour au tableau de bord
      </Link>

      <div className="dashboard-grid">
        <div className="card balance-card">
          <p style={{ opacity: 0.8, fontSize: '0.9rem', marginBottom: '0.5rem' }}>Solde actuel du compte</p>
          <h2 style={{ fontSize: '2.4rem', fontWeight: 700 }}>
            {formatMoney(account.balance)} <span style={{ fontSize: '1rem', fontWeight: 400 }}>{account.currency}</span>
          </h2>
          <p style={{ marginTop: '1rem', fontSize: '0.8rem', opacity: 0.8 }}>
            IBAN: {account.accountNumber.replace(/(.{4})/g, '$1 ')}
          </p>
        </div>

        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>Actions rapides</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <ActionButton label="Virement" icon="⇆" color="var(--primary)" />
            <ActionButton label="Relevé" icon="📄" color="var(--secondary)" />
            <ActionButton label="Plafonds" icon="⚙️" color="var(--secondary)" />
            <ActionButton label="Bloquer" icon="🔒" color="var(--danger)" />
          </div>
        </div>
      </div>

      <div style={{ marginTop: '2.5rem' }}>
        <h2 className="font-outfit" style={{ marginBottom: '1.5rem' }}>Historique complet</h2>
        <div className="card">
          {transactions.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
              <p>Aucune transaction enregistrée pour ce compte.</p>
            </div>
          ) : (
            <div className="transaction-list">
              {transactions.map((tx) => (
                <div className="transaction-item" key={tx.id}>
                  <div style={{ display: 'flex', gap: '15px', alignItems: 'center' }}>
                    <div className={`transaction-icon ${tx.amount >= 0 ? 'icon-in' : 'icon-out'}`}>
                      {tx.amount >= 0 ? '↓' : '↑'}
                    </div>
                    <div>
                      <p style={{ fontWeight: 600 }}>{tx.description || 'Transaction'}</p>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        {formatDate(tx.createdAt)} • {tx.status}
                      </p>
                    </div>
                  </div>
                  <p style={{ fontWeight: 700, color: tx.amount >= 0 ? '#10b981' : 'white' }}>
                    {tx.amount >= 0 ? '+' : ''} {formatMoney(tx.amount)} {account.currency}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function ActionButton({ label, icon, color }) {
  return (
    <button style={{ 
      display: 'flex', 
      flexDirection: 'column', 
      alignItems: 'center', 
      padding: '12px', 
      background: 'rgba(255,255,255,0.03)', 
      borderRadius: '12px',
      border: '1px solid var(--border)'
    }}>
      <span style={{ fontSize: '1.2rem', marginBottom: '4px' }}>{icon}</span>
      <span style={{ fontSize: '0.75rem', fontWeight: 500 }}>{label}</span>
    </button>
  )
}

function formatMoney(value) {
  return new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

function formatDate(isoString) {
  if (!isoString) return 'Date inconnue'
  return new Date(isoString).toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  })
}
