# Spécifications de l'API REST – Banking Portal

Ce document définit les interfaces de programmation (API) exposées par les microservices du portail bancaire. L'ensemble de ces routes est accessible de l'extérieur via l'API Gateway (Nginx) sur le port standard HTTPS (`443`).

## Considérations Générales

- **Format de données** : Le format des requêtes et réponses est exclusivement en `application/json`.
- **Authentification** : Toutes les requêtes (hormis `/api/auth/login`) nécessitent un en-tête d'autorisation contenant un jeton JWT valide :
  `Authorization: Bearer <token_jwt>`
- **Standard d'erreur** : En cas d'échec, les API renvoient les codes standards HTTP (400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 500 Internal Server Error).

---

## 1. Service d'Authentification (`/api/auth`)

Responsable de la gestion des identités, de l'émission des jetons JWT et de la clôture des sessions.

### 1.1. Connexion (Login)
- **Endpoint** : `POST /api/auth/login`
- **Description** : Authentifie un utilisateur et génère un jeton JWT.
- **Sécurité** : Publique

**Requête :**
```json
{
  "email": "alice@example.com",
  "password": "password123"
}
```

**Réponse (200 OK) :**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": 1,
  "email": "alice@example.com",
  "firstName": "Alice",
  "lastName": "Dupont",
  "expiresIn": 3600000
}
```

### 1.2. Déconnexion (Logout)
- **Endpoint** : `POST /api/auth/logout`
- **Description** : Clôture la session et journalise l'événement de déconnexion.
- **Sécurité** : Jeton JWT requis.

**Requête :**
```json
{
  "userId": 1
}
```

**Réponse (200 OK)** : Aucune donnée (Void).

### 1.3. Validation de Jeton
- **Endpoint** : `GET /api/auth/validate?token=<token>`
- **Description** : Vérifie la validité d'un jeton JWT (utilisé en interne par les autres services).

**Réponse (200 OK) :**
```json
true
```

---

## 2. Service des Comptes (`/api/accounts`)

Permet la consultation des comptes bancaires et des soldes. Ce service valide systématiquement la propriété du compte par rapport au jeton JWT de l'appelant.

### 2.1. Lister les Comptes Utilisateur
- **Endpoint** : `GET /api/accounts`
- **Description** : Récupère la liste des comptes appartenant à l'utilisateur authentifié (identifié par son JWT).

**Réponse (200 OK) :**
```json
[
  {
    "id": 1,
    "userId": 1,
    "accountNumber": "FR7630001000200030004000501",
    "accountType": "CHECKING",
    "balance": 2500.50,
    "currency": "EUR",
    "isActive": true,
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

### 2.2. Consulter un Compte Spécifique
- **Endpoint** : `GET /api/accounts/{id}`
- **Description** : Récupère les détails d'un compte précis. Renvoie `403 Forbidden` si l'utilisateur tente d'accéder au compte d'un tiers.

---

## 3. Service de Transactions (`/api/transactions`)

Gère l'historique et l'exécution des flux financiers entre les comptes. 

### 3.1. Effectuer un Virement (Transfer)
- **Endpoint** : `POST /api/transactions/transfer`
- **Description** : Initialise et exécute un transfert de fonds de manière atomique (ACID). Vérifie au préalable le solde disponible.
- **Sécurité** : Jeton JWT requis (l'appelant doit posséder le compte source).

**Requête :**
```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 150.00,
  "description": "Remboursement restaurant"
}
```

**Réponse (200 OK) :**
```json
{
  "id": 42,
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 150.00,
  "transactionType": "TRANSFER",
  "description": "Remboursement restaurant",
  "status": "COMPLETED",
  "createdAt": "2024-05-05T14:30:00",
  "completedAt": "2024-05-05T14:30:00"
}
```

### 3.2. Obtenir l'Historique des Transactions d'un Compte
- **Endpoint** : `GET /api/transactions?accountId={accountId}&page=0&size=20`
- **Description** : Retourne l'historique paginé des transactions impliquant le compte spécifié en tant qu'émetteur ou destinataire.

**Réponse (200 OK) :**
```json
{
  "content": [
    {
      "id": 42,
      "amount": 150.00,
      "status": "COMPLETED"
      // ... détails
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

### 3.3. Consulter une Transaction Spécifique
- **Endpoint** : `GET /api/transactions/{id}`
- **Description** : Renvoie les détails complets d'une transaction donnée.

---

## 4. Endpoints Internes & Supervision

### 4.1. Health Checks
Chaque microservice expose un endpoint permettant aux outils d'orchestration (ex. Docker, Kubernetes) de vérifier son état de santé :
- `GET /api/auth/health`
- `GET /api/accounts/health`
- `GET /api/transactions/health`

### 4.2. API Internes de Débit/Crédit
Le `Account Service` expose des points d'accès internes appelés de manière sécurisée par le `Transaction Service` :
- `PUT /api/accounts/{id}/debit?amount=150.00&userId=1`
- `PUT /api/accounts/{id}/credit?amount=150.00`

*Ces endpoints ne doivent pas être interrogés directement par un client externe. Ils opèrent exclusivement au sein du réseau privé Docker.*
