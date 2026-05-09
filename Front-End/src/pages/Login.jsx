import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/client'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(email, password)
      navigate('/accounts', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ 
      height: '100vh', 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center',
      background: 'var(--bg-gradient)',
      padding: '1rem'
    }}>
      <div className="card" style={{ width: '100%', maxWidth: '400px', padding: '2.5rem', background: 'var(--bg-sidebar)' }}>
        <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
          <h1 className="font-outfit" style={{ fontSize: '2rem', marginBottom: '0.5rem', color: 'var(--text-main)' }}>VanBank</h1>
          <p style={{ color: 'var(--text-muted)' }}>Accédez à votre espace sécurisé</p>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)' }}>Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="votre@email.com"
              style={{
                padding: '0.875rem',
                borderRadius: 'var(--radius-md)',
                background: 'var(--bg-main)',
                border: '1px solid var(--border)',
                color: 'var(--text-main)',
                outline: 'none'
              }}
            />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-muted)' }}>Mot de passe</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              style={{
                padding: '0.875rem',
                borderRadius: 'var(--radius-md)',
                background: 'var(--bg-main)',
                border: '1px solid var(--border)',
                color: 'var(--text-main)',
                outline: 'none'
              }}
            />
          </div>

          {error && (
            <div style={{ 
              padding: '0.75rem', 
              background: 'rgba(239, 68, 68, 0.1)', 
              color: '#ef4444', 
              borderRadius: 'var(--radius-sm)', 
              fontSize: '0.85rem',
              border: '1px solid rgba(239, 68, 68, 0.2)'
            }}>
              {error}
            </div>
          )}

          <button 
            type="submit" 
            className="btn-primary" 
            disabled={loading}
            style={{ marginTop: '1rem', opacity: loading ? 0.7 : 1 }}
          >
            {loading ? 'Connexion en cours...' : 'Se connecter'}
          </button>
        </form>

        <div style={{ marginTop: '2rem', textAlign: 'center' }}>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Pas encore client ? <span style={{ color: 'var(--primary)', fontWeight: 600, cursor: 'pointer' }}>Ouvrir un compte</span>
          </p>
        </div>
      </div>
    </div>
  )
}
