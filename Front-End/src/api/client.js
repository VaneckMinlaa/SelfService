const baseUrl = import.meta.env.VITE_API_BASE_URL || '';

function request(path, options = {}) {
  const url = `${baseUrl}${path}`;
  return fetch(url, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });
}

export async function login(email, password) {
  const res = await request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.error || 'Échec de la connexion');
  }
  return res.json();
}

export async function logout() {
  await request('/api/auth/logout', { method: 'POST' });
}

export async function getMe() {
  const res = await request('/api/auth/me');
  if (!res.ok) return null;
  return res.json();
}

export async function getAccounts() {
  const res = await request('/api/accounts');
  if (!res.ok) throw new Error('Impossible de charger les comptes');
  return res.json();
}

export async function getAccount(id) {
  const res = await request(`/api/accounts/${id}`);
  if (!res.ok) return null;
  return res.json();
}

export async function getTransactions(accountId) {
  const res = await request(`/api/accounts/${accountId}/transactions`);
  if (!res.ok) throw new Error('Impossible de charger les transactions');
  return res.json();
}
