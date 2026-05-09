import Sidebar from './Sidebar';
import { useState, useEffect } from 'react';
import { getMe } from '../api/client';

export default function Layout({ children, theme, toggleTheme }) {
  const [user, setUser] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    getMe().then(setUser).catch(console.error);
  }, []);

  return (
    <div className="app-container">
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      
      {/* Mobile Overlay */}
      {sidebarOpen && (
        <div 
          onClick={() => setSidebarOpen(false)}
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            zIndex: 99,
            backdropFilter: 'blur(4px)'
          }}
        />
      )}

      <main className="main-content">
        <header className="header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <button 
              className="mobile-toggle"
              onClick={() => setSidebarOpen(true)}
              style={{ display: 'none', fontSize: '1.5rem', color: 'var(--text-main)' }}
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
            </button>
            <div>
              <h1 style={{ fontSize: '1.8rem' }}>Bienvenue, <span className="text-gradient">{user?.firstName || 'Utilisateur'}</span></h1>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Ravi de vous revoir dans votre espace VanBank.</p>
            </div>
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <button 
              onClick={toggleTheme} 
              style={{ 
                background: 'var(--bg-card)', 
                border: '1px solid var(--border)', 
                borderRadius: '12px', 
                width: '42px', 
                height: '42px', 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center',
                color: 'var(--text-main)',
                boxShadow: 'var(--shadow)',
                cursor: 'pointer'
              }}
              title={theme === 'light' ? 'Mode Sombre' : 'Mode Clair'}
            >
              {theme === 'light' ? (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg>
              ) : (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg>
              )}
            </button>
            <div className="user-profile">
              <div className="avatar" style={{ background: 'var(--primary)', color: 'white' }}>{user?.firstName?.[0] || 'U'}</div>
              <div style={{ textAlign: 'right' }} className="user-info-hide">
                <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{user?.firstName} {user?.lastName}</div>
              </div>
            </div>
          </div>
        </header>
        
        {children}
      </main>

      <style>{`
        @media (max-width: 768px) {
          .mobile-toggle { display: block !important; }
          .user-info-hide { display: none; }
        }
      `}</style>
    </div>
  );
}
