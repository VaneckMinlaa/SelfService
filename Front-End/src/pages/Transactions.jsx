import { useState, useEffect } from 'react'
import { getAccounts, getTransactions } from '../api/client'

export default function Transactions() {
  const [transactions, setTransactions] = useState([])
  const [accounts, setAccounts] = useState([])
  const [selectedAccount, setSelectedAccount] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getAccounts()
      .then(accs => {
        setAccounts(accs)
        if (accs.length > 0) {
          const firstId = accs[0].id
          setSelectedAccount(firstId)
          return getTransactions(firstId)
        }
        return { content: [] }
      })
      .then(res => setTransactions(res?.content || res || []))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  const handleAccountChange = async (id) => {
    setSelectedAccount(id)
    setLoading(true)
    try {
      const res = await getTransactions(id)
      setTransactions(res?.content || res || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const income = transactions.reduce((acc, tx) => tx.amount > 0 ? acc + tx.amount : acc, 0)
  const expenses = transactions.reduce((acc, tx) => tx.amount < 0 ? acc + Math.abs(tx.amount) : acc, 0)

  if (loading && transactions.length === 0) return <div className="page-loading">Chargement...</div>

  return (
    <div className="transactions-view">
      <div className="view-header">
        <h2 className="font-outfit">Historique des Opérations</h2>
        <select 
          value={selectedAccount} 
          onChange={(e) => handleAccountChange(e.target.value)}
          className="account-select"
        >
          {accounts.map(acc => (
            <option key={acc.id} value={acc.id}>{acc.accountType} - {acc.accountNumber}</option>
          ))}
        </select>
      </div>

      <div className="dashboard-grid" style={{ marginBottom: '2.5rem' }}>
        <div className="card" style={{ borderLeft: '4px solid #10b981' }}>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Total Crédits</p>
          <p style={{ fontSize: '1.4rem', fontWeight: 700, color: '#10b981' }}>+ {formatMoney(income)} XAF</p>
        </div>
        <div className="card" style={{ borderLeft: '4px solid #ef4444' }}>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Total Débits</p>
          <p style={{ fontSize: '1.4rem', fontWeight: 700, color: '#ef4444' }}>- {formatMoney(expenses)} XAF</p>
        </div>
        <div className="card" style={{ borderLeft: '4px solid var(--primary)' }}>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Nombre d'opérations</p>
          <p style={{ fontSize: '1.4rem', fontWeight: 700 }}>{transactions.length}</p>
        </div>
      </div>

      <div className="card" style={{ padding: '0', overflow: 'hidden' }}>
        {/* Desktop Table View */}
        <div className="desktop-only-view">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Type</th>
                <th>Montant</th>
                <th>Statut</th>
              </tr>
            </thead>
            <tbody>
              {transactions.length === 0 ? (
                <tr>
                  <td colSpan="5" style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                    Aucune transaction trouvée.
                  </td>
                </tr>
              ) : (
                transactions.map(tx => (
                  <tr key={tx.id}>
                    <td>{formatDate(tx.createdAt)}</td>
                    <td style={{ fontWeight: 500 }}>{tx.description || 'Virement'}</td>
                    <td><span className="badge" style={{ background: 'rgba(255,255,255,0.05)' }}>{tx.transactionType}</span></td>
                    <td style={{ fontWeight: 700, color: tx.amount > 0 ? '#10b981' : 'var(--text-main)' }}>
                      {tx.amount > 0 ? '+' : ''}{formatMoney(tx.amount)} XAF
                    </td>
                    <td>
                      <span className={`badge ${tx.status === 'COMPLETED' ? 'badge-success' : 'badge-warning'}`}>
                        {tx.status}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Mobile List View */}
        <div className="mobile-only-view">
          {transactions.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>Aucune transaction.</p>
          ) : (
            <div className="mobile-list">
              {transactions.map(tx => (
                <div key={tx.id} className="mobile-tx-item">
                  <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                    <div className={`transaction-icon ${tx.amount > 0 ? 'icon-in' : 'icon-out'}`}>
                      {tx.amount > 0 ? '↓' : '↑'}
                    </div>
                    <div style={{ flex: 1 }}>
                      <p style={{ fontWeight: 600, fontSize: '0.95rem' }}>{tx.description || 'Transaction'}</p>
                      <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{formatDate(tx.createdAt)}</p>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <p style={{ fontWeight: 700, color: tx.amount > 0 ? '#10b981' : 'var(--text-main)' }}>
                        {tx.amount > 0 ? '+' : ''}{formatMoney(tx.amount)}
                      </p>
                      <span style={{ fontSize: '0.65rem' }} className={`badge ${tx.status === 'COMPLETED' ? 'badge-success' : 'badge-warning'}`}>
                        {tx.status}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        .view-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; gap: 1rem; }
        .account-select { padding: 8px 12px; border-radius: 8px; background: var(--bg-sidebar); color: var(--text-main); border: 1px solid var(--border); outline: none; }
        .data-table { width: 100%; border-collapse: collapse; text-align: left; }
        .data-table th { padding: 1rem 1.5rem; background: rgba(255,255,255,0.02); color: var(--text-muted); font-size: 0.8rem; text-transform: uppercase; letter-spacing: 1px; }
        .data-table td { padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--border); font-size: 0.9rem; }
        .data-table tr:last-child td { border-bottom: none; }
        .data-table tr:hover td { background: rgba(255,255,255,0.01); }
        
        .mobile-only-view { display: none; }
        .desktop-only-view { display: block; }

        @media (max-width: 768px) {
          .view-header { flex-direction: column; align-items: flex-start; }
          .account-select { width: 100%; }
          .desktop-only-view { display: none; }
          .mobile-only-view { display: block; }
          .mobile-list { display: flex; flex-direction: column; }
          .mobile-tx-item { padding: 1rem; border-bottom: 1px solid var(--border); }
          .mobile-tx-item:last-child { border-bottom: none; }
        }
      `}} />
    </div>
  )
}

function formatMoney(value) {
  return new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2 }).format(value)
}

function formatDate(isoString) {
  if (!isoString) return '-'
  return new Date(isoString).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}
