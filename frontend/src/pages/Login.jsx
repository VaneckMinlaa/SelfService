import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/client'
import './Login.css'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/accounts', { replace: true })
    } catch (err) {
      setError(err.message || 'Identifiants incorrects')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>Portail Bancaire Self-Service</h1>
        <p className="subtitle">Connectez-vous pour consulter vos comptes</p>
        <form onSubmit={handleSubmit}>
          {error && <div className="login-error">{error}</div>}
          <label>
            Identifiant
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              required
            />
          </label>
          <label>
            Mot de passe
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
          <button type="submit" disabled={loading}>
            {loading ? 'Connexion…' : 'Se connecter'}
          </button>
        </form>
        <p className="demo-hint">Démo : alice / password ou bob / password</p>
      </div>
    </div>
  )
}
