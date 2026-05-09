import { useState, useEffect } from 'react'
import { getAccounts, transferMoney, getAccountByNumber, getTransactions } from '../api/client'

export default function Transfer() {
  const [accounts, setAccounts] = useState([])
  const [allTransactions, setAllTransactions] = useState([])
  const [isModalOpen, setIsModalOpen] = useState(false)
  
  // Form State
  const [fromAccount, setFromAccount] = useState('')
  const [toAccountNumber, setToAccountNumber] = useState('')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [loading, setLoading] = useState(false)
  const [msg, setMsg] = useState({ text: '', type: '' })

  const fetchData = async () => {
    try {
      const accs = await getAccounts()
      setAccounts(accs)
      
      // Fetch transactions for each account and merge
      const txPromises = accs.map(acc => getTransactions(acc.id))
      const txResults = await Promise.all(txPromises)
      const merged = txResults.flatMap(res => res?.content || res || [])
        .filter(tx => tx.transactionType === 'TRANSFER')
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      
      setAllTransactions(merged)
    } catch (err) {
      console.error(err)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setMsg({ text: '', type: '' })
    try {
      // 1. Resolve Account Number to ID
      const destAccount = await getAccountByNumber(toAccountNumber)
      
      // 2. Perform Transfer
      await transferMoney(fromAccount, destAccount.id, amount, description)
      
      setMsg({ text: 'Virement effectué avec succès !', type: 'success' })
      setAmount('')
      setToAccountNumber('')
      setDescription('')
      setTimeout(() => {
        setIsModalOpen(false)
        setMsg({ text: '', type: '' })
        fetchData()
      }, 1500)
    } catch (err) {
      setMsg({ text: err.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="transfer-view">
      <div className="view-header">
        <h2 className="font-outfit">Mes Virements</h2>
        <button 
          onClick={() => setIsModalOpen(true)}
          className="btn-primary"
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
        >
          <span>+</span> Nouveau Virement
        </button>
      </div>

      {/* Mini Stats Overview */}
      <div className="dashboard-grid" style={{ marginBottom: '2.5rem' }}>
        <div className="card">
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Virements émis</p>
          <p style={{ fontSize: '1.4rem', fontWeight: 700 }}>{allTransactions.length}</p>
        </div>
        <div className="card">
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Total Transféré</p>
          <p style={{ fontSize: '1.4rem', fontWeight: 700 }}>{formatMoney(allTransactions.reduce((acc, t) => acc + t.amount, 0))} XAF</p>
        </div>
      </div>

      {/* Table of Transfers */}
    <div className="card" style={{ padding: '0', overflow: 'hidden' }}>
        {/* Desktop View */}
        <div className="desktop-only-view">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Source</th>
                <th>Destination</th>
                <th>Montant</th>
                <th>Motif</th>
                <th>Statut</th>
              </tr>
            </thead>
            <tbody>
              {allTransactions.length === 0 ? (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                    Aucun virement enregistré.
                  </td>
                </tr>
              ) : (
                allTransactions.map(tx => {
                  const fromAcc = accounts.find(a => a.id === tx.fromAccountId);
                  const toAcc = accounts.find(a => a.id === tx.toAccountId);
                  
                  return (
                    <tr key={tx.id}>
                      <td>{formatDate(tx.createdAt)}</td>
                      <td style={{ fontWeight: 500 }}>
                        {fromAcc ? fromAcc.accountNumber : `Compte #${tx.fromAccountId}`}
                      </td>
                      <td style={{ fontWeight: 500 }}>
                        {toAcc ? toAcc.accountNumber : `Compte #${tx.toAccountId}`}
                      </td>
                      <td style={{ fontWeight: 700 }}>{formatMoney(tx.amount)} XAF</td>
                      <td style={{ color: 'var(--text-muted)' }}>{tx.description}</td>
                      <td><span className="badge badge-success">{tx.status}</span></td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Mobile View */}
        <div className="mobile-only-view">
          {allTransactions.length === 0 ? (
            <p style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>Aucun virement.</p>
          ) : (
            <div className="mobile-list">
              {allTransactions.map(tx => {
                const toAcc = accounts.find(a => a.id === tx.toAccountId);
                return (
                  <div key={tx.id} className="mobile-tx-item">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                      <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{formatDate(tx.createdAt)}</p>
                      <span style={{ fontSize: '0.65rem', padding: '2px 8px' }} className="badge badge-success">{tx.status}</span>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      <div style={{ flex: 1 }}>
                        <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '2px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Vers</p>
                        <p style={{ 
                          fontWeight: 600, 
                          fontSize: '0.9rem', 
                          wordBreak: 'break-all', 
                          lineHeight: '1.4',
                          color: 'var(--text-main)' 
                        }}>
                          {toAcc ? toAcc.accountNumber : `Compte #${tx.toAccountId}`}
                        </p>
                        {tx.description && (
                          <p style={{ 
                            fontSize: '0.8rem', 
                            color: 'var(--text-muted)', 
                            marginTop: '6px',
                            fontStyle: 'italic',
                            borderLeft: '2px solid var(--border)',
                            paddingLeft: '8px'
                          }}>
                            {tx.description}
                          </p>
                        )}
                      </div>
                      <div style={{ 
                        marginTop: '4px',
                        paddingTop: '8px',
                        borderTop: '1px dashed var(--border)',
                        display: 'flex',
                        justifyContent: 'flex-end'
                      }}>
                        <p style={{ fontWeight: 700, fontSize: '1.2rem', color: 'var(--primary)' }}>
                          {formatMoney(tx.amount)} <span style={{ fontSize: '0.8rem' }}>XAF</span>
                        </p>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div className="modal-overlay">
          <div className="card modal-content" style={{ maxWidth: '500px', width: '90%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
              <h3 className="font-outfit">Nouveau Virement</h3>
              <button onClick={() => setIsModalOpen(false)} style={{ color: 'var(--text-muted)', fontSize: '1.5rem' }}>&times;</button>
            </div>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.2rem' }}>
              <div className="form-group">
                <label>Source</label>
                <select value={fromAccount} onChange={(e) => setFromAccount(e.target.value)} required className="form-control">
                  <option value="">Choisir un compte</option>
                  {accounts.map(acc => (
                    <option key={acc.id} value={acc.id}>{acc.accountType} ({formatMoney(acc.balance)} {acc.currency})</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Numéro de compte destinataire (IBAN)</label>
                <input 
                  type="text" 
                  value={toAccountNumber} 
                  onChange={(e) => setToAccountNumber(e.target.value)}
                  placeholder="Ex: FR76..."
                  required
                  className="form-control"
                />
              </div>

              <div className="form-group">
                <label>Montant (XAF)</label>
                <input 
                  type="number" 
                  value={amount} 
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="0.00"
                  required
                  className="form-control"
                />
              </div>

              <div className="form-group">
                <label>Description</label>
                <textarea 
                  value={description} 
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Motif du virement"
                  className="form-control"
                />
              </div>

              {msg.text && (
                <div className={`badge ${msg.type === 'success' ? 'badge-success' : 'badge-warning'}`} style={{ padding: '0.8rem', borderRadius: '8px' }}>
                  {msg.text}
                </div>
              )}

              <button type="submit" className="btn-primary" disabled={loading} style={{ marginTop: '1rem' }}>
                {loading ? 'Traitement...' : 'Confirmer le virement'}
              </button>
            </form>
          </div>
        </div>
      )}

      <style dangerouslySetInnerHTML={{ __html: `
        .view-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; gap: 1rem; }
        .modal-overlay {
          position: fixed; top: 0; left: 0; width: 100%; height: 100%;
          background: rgba(0,0,0,0.8); backdrop-filter: blur(4px);
          display: flex; align-items: center; justify-content: center; z-index: 1000;
        }
        .modal-content { max-width: 500px; width: 90%; }
        .form-group { display: flex; flex-direction: column; gap: 6px; }
        .form-control {
          padding: 10px; border-radius: 8px; background: var(--bg-sidebar);
          border: 1px solid var(--border); color: var(--text-main); outline: none; font-family: inherit;
          appearance: none; -webkit-appearance: none;
        }
        select.form-control {
          background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='white'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
          background-repeat: no-repeat; background-position: right 10px center; background-size: 15px;
          padding-right: 35px;
        }
        .form-control option { background: #0f172a; color: white; padding: 10px; }
        .form-control:focus { border-color: var(--primary); }
        .data-table { width: 100%; border-collapse: collapse; text-align: left; }
        .data-table th { padding: 1rem 1.5rem; background: rgba(255,255,255,0.02); color: var(--text-muted); font-size: 0.75rem; text-transform: uppercase; }
        .data-table td { padding: 1rem 1.5rem; border-bottom: 1px solid var(--border); font-size: 0.85rem; }

        .mobile-only-view { display: none; }
        .desktop-only-view { display: block; }

        @media (max-width: 768px) {
          .view-header { flex-direction: column; align-items: stretch; gap: 1rem; margin-bottom: 1.5rem; }
          .view-header button { width: 100%; justify-content: center; padding: 1rem; }
          .modal-content { width: 95%; padding: 1.5rem; }
          .desktop-only-view { display: none; }
          .mobile-only-view { display: block; }
          .mobile-list { display: flex; flex-direction: column; background: var(--bg-card); }
          .mobile-tx-item { padding: 1.25rem 1rem; border-bottom: 1px solid var(--border); transition: background 0.2s; }
          .mobile-tx-item:active { background: rgba(255,255,255,0.05); }
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
  return new Date(isoString).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
}
