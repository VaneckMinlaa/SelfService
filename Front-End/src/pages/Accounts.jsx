import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getAccounts, getTransactions, getMe, creditAccount, debitAccount } from '../api/client'

export default function Accounts() {
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    Promise.all([getAccounts(), getMe()])
      .then(([accs, u]) => {
        setAccounts(accs)
        setUser(u)
        if (accs.length > 0) {
          return getTransactions(accs[0].id)
        }
        return []
      })
      .then(tx => setTransactions(tx?.content || tx || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  const totalBalance = accounts.reduce((acc, curr) => acc + curr.balance, 0)

  if (loading) return <div className="page-loading">Chargement de votre espace…</div>

  return (
    <div className="dashboard-view">
      <div className="dashboard-grid">
        {/* Total Balance Card */}
        <div className="card balance-card">
          <p style={{ opacity: 0.8, fontSize: '0.9rem', marginBottom: '0.5rem' }}>Solde Total</p>
          <h2 style={{ fontSize: '2.4rem', fontWeight: 700 }}>
            {formatMoney(totalBalance)} <span style={{ fontSize: '1rem', fontWeight: 400 }}>XAF</span>
          </h2>
          <div style={{ marginTop: '1.5rem', display: 'flex', gap: '10px' }}>
            <button className="btn-primary" onClick={() => navigate('/transfer')} style={{ background: 'white', color: 'var(--primary)', boxShadow: 'none', borderRadius: '8px' }}>
              Virement
            </button>
          </div>
        </div>

        {/* Quick Stats Card */}
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>Analyse mensuelle</h3>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
            <div>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Revenus</p>
              <p style={{ color: '#10b981', fontWeight: 600 }}>+ {formatMoney(transactions.filter(t => t.amount > 0).reduce((acc, t) => acc + t.amount, 0))} XAF</p>
            </div>
            <div style={{ textAlign: 'right' }}>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Dépenses</p>
              <p style={{ color: '#ef4444', fontWeight: 600 }}>- {formatMoney(transactions.filter(t => t.amount < 0).reduce((acc, t) => acc + Math.abs(t.amount), 0))} XAF</p>
            </div>
          </div>
          <div style={{ height: '8px', background: 'rgba(255,255,255,0.05)', borderRadius: '10px', overflow: 'hidden' }}>
            <div style={{ width: `${transactions.filter(t => t.amount > 0).reduce((acc, t) => acc + t.amount, 0) > 0 ? Math.min(Math.round((transactions.filter(t => t.amount < 0).reduce((acc, t) => acc + Math.abs(t.amount), 0) / transactions.filter(t => t.amount > 0).reduce((acc, t) => acc + t.amount, 0)) * 100), 100) : 0}%`, height: '100%', background: 'var(--primary)' }}></div>
          </div>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '8px' }}>
            {transactions.filter(t => t.amount > 0).reduce((acc, t) => acc + t.amount, 0) > 0 ? Math.min(Math.round((transactions.filter(t => t.amount < 0).reduce((acc, t) => acc + Math.abs(t.amount), 0) / transactions.filter(t => t.amount > 0).reduce((acc, t) => acc + t.amount, 0)) * 100), 100) : 0}% de vos revenus utilisés
          </p>
        </div>
      </div>

      <div style={{ marginTop: '2.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h2 className="font-outfit">Mes Comptes</h2>
          <button style={{ color: 'var(--primary)', fontWeight: 600, fontSize: '0.9rem' }}>Voir tout</button>
        </div>

        <div className="dashboard-grid">
          {accounts.map((acc) => (
            <Link to={`/accounts/${acc.id}`} key={acc.id} className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
                <div className="logo-icon" style={{ background: 'rgba(37,99,235,0.1)', color: 'var(--primary)', boxShadow: 'none' }}>
                  {acc.accountType === 'SAVINGS' ? 'S' : 'C'}
                </div>
                <span className={`badge ${acc.isActive ? 'badge-success' : 'badge-warning'}`}>
                  {acc.isActive ? 'Actif' : 'Inactif'}
                </span>
              </div>
              <h3 style={{ marginBottom: '4px' }}>{acc.accountType === 'SAVINGS' ? 'Compte Épargne' : 'Compte Courant'}</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginBottom: '1.5rem' }}>
                {acc.accountNumber.replace(/(.{4})/g, '$1 ')}
              </p>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                <p style={{ fontSize: '1.4rem', fontWeight: 700 }}>
                  {formatMoney(acc.balance)} <span style={{ fontSize: '0.8rem', fontWeight: 400 }}>{acc.currency}</span>
                </p>
                <div style={{ width: '30px', height: '20px', background: 'rgba(255,255,255,0.1)', borderRadius: '4px' }}></div>
              </div>
            </Link>
          ))}
        </div>
      </div>

      <div style={{ marginTop: '2.5rem' }}>
        <h2 className="font-outfit" style={{ marginBottom: '1.5rem' }}>Transactions Récentes</h2>
        <div className="card">
          <div className="transaction-list">
            {transactions.length === 0 ? (
              <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '1rem' }}>Aucune transaction récente.</p>
            ) : (
              transactions.slice(0, 5).map(tx => (
                <TransactionItem 
                  key={tx.id}
                  title={tx.description || 'Virement'} 
                  subtitle={tx.transactionType} 
                  amount={(tx.amount >= 0 ? '+ ' : '') + formatMoney(tx.amount)} 
                  date={new Date(tx.createdAt).toLocaleDateString()} 
                  type={tx.amount >= 0 ? 'in' : 'out'} 
                />
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

function TransactionItem({ title, subtitle, amount, date, type }) {
  return (
    <div className="transaction-item">
      <div style={{ display: 'flex', gap: '15px', alignItems: 'center' }}>
        <div className={`transaction-icon ${type === 'in' ? 'icon-in' : 'icon-out'}`}>
          {type === 'in' ? '↓' : '↑'}
        </div>
        <div>
          <p style={{ fontWeight: 600 }}>{title}</p>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{subtitle} • {date}</p>
        </div>
      </div>
      <p style={{ fontWeight: 700, color: type === 'in' ? '#10b981' : 'var(--text-main)' }}>
        {amount} XAF
      </p>
    </div>
  )
}

function formatMoney(value) {
  return new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}
