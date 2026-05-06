# UML Diagrams Index - Banking Portal Project

## Diagram Index

### 1. System Architecture Diagram
**Type:** Component Diagram
**Purpose:** High-level overview of all system components and their interactions
**Key Components:**
- End User (Client)
- API Gateway (NGINX with security features)
- 5 Microservices (Auth, Account, Transaction, Notification, Frontend)
- 3 PostgreSQL Databases (separated per service)
- RabbitMQ Message Queue
- Monitoring Stack (Prometheus + Grafana)
- Backup & Recovery System
- CI/CD Pipeline (GitLab)

**Relationships:** Shows complete flow from user through gateway to services and databases

---

### 2. Authentication Flow Sequence Diagram
**Type:** Sequence Diagram
**Purpose:** Detailed sequence of user login and JWT token validation
**Key Flows:**
1. User login request → Auth Service
2. Password validation (BCrypt)
3. JWT generation (HMAC-SHA512)
4. Subsequent API calls with JWT token
5. JWT validation in API Gateway
6. Account Service authorization

**Interactions:** 5 participant flows showing token lifecycle

---

### 3. Money Transfer Transaction Flow Sequence Diagram
**Type:** Sequence Diagram
**Purpose:** Complete sequence of a money transfer operation
**Key Flows:**
1. Transfer request from client
2. JWT validation
3. Source account verification
4. Account debit
5. Account credit
6. Transaction record creation
7. Event publishing to RabbitMQ
8. Email notification generation

**Interactions:** Shows full ACID transaction flow with event publishing

---

### 4. CI/CD Pipeline Flow Sequence Diagram
**Type:** Sequence Diagram
**Purpose:** Automated deployment pipeline from code push to production
**Key Flows:**
1. Developer push to GitLab
2. Pipeline triggers (Lint stage)
3. Parallel build of 5 services
4. Push to Docker Registry
5. Deploy to Staging VPS (auto on develop)
6. Deploy to Production VPS (manual on main)
7. Health checks with auto-rollback
8. Slack notifications

**Interactions:** Shows staging validation before production deployment

---

### 5. Database Schema Diagram
**Type:** Entity-Relationship Diagram
**Purpose:** Data model showing tables and relationships
**Tables:**
- Users (auth_db): User credentials, roles, profiles
- AuditLogs (auth_db): Security event logging
- Accounts (account_db): Bank accounts with balances
- Transactions (transaction_db): Transfer history

**Relationships:** Shows one-to-many relationships and primary/foreign keys

---

### 6. Microservices Deployment Topology Diagram
**Type:** Deployment Diagram
**Purpose:** Docker container layout and volume persistence
**Components:**
- 14 containers (services + databases + monitoring)
- Docker networks (banking-network bridge)
- 7 persistent volumes (data, config, logs)
- Non-root user containers

**Features:** Shows health checks, ports, and volume mounts for each service

---

### 7. Monitoring and Observability Architecture Diagram
**Type:** Component Diagram
**Purpose:** Metrics collection, visualization, and alerting system
**Components:**
- Prometheus (metrics collection from 6 targets)
- Grafana (dashboard and alerting)
- Health checks (Docker + custom endpoints)
- JSON logging (structured logs)

**Relationships:** Shows scrape intervals (30s), data retention (30 days), alert flow

---

### 8. Security Layers Diagram
**Type:** Architecture Diagram
**Purpose:** Multi-layered security controls from perimeter to data
**Layers:**
1. Perimeter (DDoS protection, Firewall)
2. Transport (TLS 1.2+, OCSP stapling)
3. API Gateway (NGINX rate limiting, ModSecurity WAF)
4. Application (JWT, RBAC, input validation, SQL protection)
5. Data (encryption, network isolation, audit logs)
6. Infrastructure (non-root containers, secrets management)
7. Monitoring (logging, alerts, incident response)

**Flow:** Shows defense-in-depth approach

---

### 9. Backup and Disaster Recovery Process Diagram
**Type:** Process Diagram
**Purpose:** Daily backup automation and recovery procedures
**Daily Process:**
1. Cron trigger (2 AM)
2. pg_dump of all 3 databases
3. Configuration backup
4. gzip compression
5. Archive to timestamped directory
6. 30-day retention cleanup
7. Optional cloud sync (S3/rsync)

**Recovery Process:**
1. Manual trigger of restore script
2. Decompress files
3. Drop current structures
4. Import from SQL dump
5. Verify data integrity

**Storage:** Shows directory structure with 30-day retention

---

### 10. Class Diagram (Diagramme de Classe)
**Type:** Diagramme de Classe UML
**Purpose:** Structure orientée objet de tous les composants applicatifs
**Key Classes:**
- **Entities:** User, Account, Transaction, AuditLog, TransactionEvent
- **Services:** AuthService, AccountService, TransactionService, NotificationService
- **Repositories:** UserRepository, AccountRepository, TransactionRepository
- **Security:** JwtTokenProvider, PasswordEncoder
- **Enums:** AccountType, AccountStatus, TransactionStatus

**Relationships:** Montre héritage, composition, associations 1-to-many entre classes

---

### Total: 11 Diagrammes
- **Health checks:** 30s intervals with 10s timeout on all services
- **Rate limiting:** 5 req/s on auth, 100 req/s on API endpoints
- **HTTPS enforcement:** TLS 1.2+ mandatory via NGINX
- **Database isolation:** 3 separate PostgreSQL instances
- **Message queue:** RabbitMQ for async event processing
- **Monitoring:** Prometheus + Grafana with 30-day retention
- **Logging:** JSON driver with 10MB max file size, 3 file rotation
- **Backup retention:** 30 days with automated cleanup
- **CI/CD automation:** GitLab pipeline with staging validation
- **Rollback capability:** Automatic on health check failure
- **SSL certificates:** Let's Encrypt with Certbot auto-renewal

---

## How to Use These Diagrams

### View in PlantUML Online Editor
1. Visit: https://www.plantuml.com/plantuml/uml/
2. Paste the diagram code from UML_DIAGRAMS.puml
3. Render to see the diagram

### Generate PNG/SVG from Command Line
```bash
# Install PlantUML
# Windows: choco install plantuml
# macOS: brew install plantuml
# Linux: apt-get install plantuml

# Generate PNG
plantuml -Tpng UML_DIAGRAMS.puml

# Generate SVG
plantuml -Tsvg UML_DIAGRAMS.puml
```

### Use in Documentation
- Embed in README.md using markdown image syntax
- Include in architecture documentation
- Reference in technical specifications
- Use in team presentations

---

## Diagram Specifications

| Diagram | Type | Participants | Key Interactions |
|---------|------|--------------|------------------|
| System Architecture | Component | 15 components | 25+ relationships |
| Auth Flow | Sequence | 5 participants | 12 messages |
| Transfer Flow | Sequence | 7 participants | 18 messages |
| CI/CD Pipeline | Sequence | 6 participants | 16 messages |
| Database Schema | ER Diagram | 4 entities | 6 relationships |
| Deployment | Deployment | 14 containers | Network + volumes |
| Monitoring | Component | 9 components | 12 connections |
| Security | Architecture | 7 layers | Hierarchical flow |
| Backup | Process | 5 stages | 2-phase (backup+recovery) |
| Complete Interactions | Sequence | 13 participants | 32 messages |
| Class Diagram | Class | 35+ classes | Associations + inheritance |

---

## Technical Details

### PlantUML Features Used
- Subgraphs for logical grouping
- Components, actors, databases for modeling
- Different diagram types (component, sequence, ER, deployment)
- Notes and descriptions for clarity
- Color coding through themes (plain theme for clarity)
- Relationship arrows with labels

### No External Dependencies
- All diagrams are self-contained in single file
- Pure PlantUML syntax (no custom extensions)
- Compatible with all PlantUML rendering engines
- No external image references
- Portable and version-control friendly

---

## Project Coverage

These diagrams cover the complete Banking Portal architecture:

1. **Infrastructure:** Docker Compose, networking, volumes, persistence
2. **Services:** 5 microservices with clear responsibilities
3. **Security:** Multi-layer security from perimeter to data
4. **Data:** 3 isolated PostgreSQL instances with schema
5. **Communication:** Synchronous (REST) and async (RabbitMQ)
6. **Deployment:** CI/CD pipeline with staging and production
7. **Observability:** Monitoring, logging, health checks
8. **Reliability:** Backup, restore, rollback capabilities

---

## References

- PlantUML Documentation: https://plantuml.com/
- Architecture Best Practices: Microservices Patterns
- Security: OWASP Top 10, Banking Security Standards
- DevOps: CI/CD Best Practices, Infrastructure as Code

---

Generated: May 6, 2026
Format: PlantUML (.puml)
Total Diagrams: 11
Total Components: 80+
Lines of UML Code: 1200+
