# 🏗️ Architecture Globale du Projet

## Vue d'ensemble Multi-Couches

```
┌────────────────────────────────────────────────────────────────────────┐
│                           END USERS / CLIENTS                           │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                     ┌─────────────▼──────────────┐
                     │   Let's Encrypt (Certbot) │
                     │   Auto-renewal Certificate │
                     └────────────┬───────────────┘
                                  │ HTTPS/TLS 1.2+
                    ┌─────────────▼──────────────┐
                    │   NGINX GATEWAY LAYER      │
                    │   ┌────────────────────┐   │
                    │   │ Security Headers   │   │
                    │   │ Rate Limiting      │   │
                    │   │ Compression (Gzip) │   │
                    │   │ Load Balancing     │   │
                    │   │ (ModSecurity - WAF)│   │
                    │   └────────────────────┘   │
                    │   Port: 80 → 443           │
                    │   Handles ~1000 req/s      │
                    └─────┬──────┬──────┬─────┬───┘
         ┌──────────────┼──────┼──────┼─────┼─────────────────┐
         │ /api/auth    │ /    │/api/a│/api/│  /api/notify    │
         │              │      │ccounts│trans│                │
         ▼              ▼      ▼      ▼     ▼                ▼
    ┌─────────────────────────────────────────────────────────────┐
    │           MICROSERVICES LAYER (5 Services)                  │
    ├─────────────────────────────────────────────────────────────┤
    │                                                              │
    │  ┌──────────────────┐  ┌──────────────────┐               │
    │  │  AUTH SERVICE    │  │ ACCOUNT SERVICE  │               │
    │  │  Port: 8081      │  │ Port: 8082       │               │
    │  │  JWT Handler     │  │ Account Mgmt     │               │
    │  │  Login/Register  │  │ Balance Queries  │               │
    │  │  Non-root user ✓ │  │ Non-root user ✓  │               │
    │  │  Healthcheck ✓   │  │ Healthcheck ✓    │               │
    │  └──────┬───────────┘  └────────┬─────────┘               │
    │         │                       │                          │
    │  ┌──────────────────┐  ┌──────────────────┐               │
    │  │TRANSACTION SERVICE│  │NOTIFICATION SVC  │               │
    │  │  Port: 8083      │  │ Port: 8084       │               │
    │  │  Transfers       │  │ Email/SMS Alerts │               │
    │  │  Payments        │  │ Async Messaging  │               │
    │  │  Non-root user ✓ │  │ Non-root user ✓  │               │
    │  │  Healthcheck ✓   │  │ Healthcheck ✓    │               │
    │  └──────┬───────────┘  └────────┬─────────┘               │
    │         │                       │                          │
    │  ┌──────────────────┐                                     │
    │  │   FRONTEND SPA   │                                     │
    │  │   React + Vite   │                                     │
    │  │   Port: 3000     │                                     │
    │  │   Non-root ✓     │                                     │
    │  └──────┬───────────┘                                     │
    │         │                                                 │
    │         └────────────────┬────────────────────────────────┘
    │                          │
    │        ┌─────────────────┼─────────────────┐
    │        │                 │                 │
    │        ▼                 ▼                 ▼
    │   RabbitMQ         Event Bus         Message Queue
    │   Port: 5672       Async Events      Notifications
    │   Durable Queues   Event Publishing  Email/SMS
    │                    ✓ Persistent      ✓ Retry Logic
    │
    └────────────────────────────────────────────────────────────┘
             │                          │                     │
    ┌────────▼───────┐        ┌────────▼──────┐      ┌───────▼──────┐
    │  PostgreSQL    │        │  PostgreSQL   │      │  PostgreSQL  │
    │  auth_db       │        │  account_db   │      │ transaction_ │
    │  Port: 5432    │        │  Port: 5432   │      │    db        │
    │  ├─ Users      │        │  ├─ Accounts  │      │  Port: 5432  │
    │  ├─ Roles      │        │  ├─ Balance   │      │  ├─ Trans.   │
    │  └─ Sessions   │        │  └─ History   │      │  └─ History  │
    │  Volume ✓      │        │  Volume ✓     │      │  Volume ✓    │
    │  Backup ✓      │        │  Backup ✓     │      │  Backup ✓    │
    └────────────────┘        └───────────────┘      └──────────────┘
             │                          │                     │
             └──────────────┬───────────┴─────────────────────┘
                            │
              ┌─────────────▼──────────────┐
              │    BACKUP & RECOVERY       │
              │  Location: /backups/banking│
              │  ├─ Daily Backup (Gzip)    │
              │  ├─ 30-day Retention       │
              │  ├─ Automated Rotation     │
              │  └─ Restore Scripts ✓      │
              └────────────────────────────┘
```

---

## Monitoring & Observability Layer

```
┌────────────────────────────────────────────────────────────────┐
│              MONITORING & OBSERVABILITY LAYER                  │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────────┐              ┌──────────────────┐      │
│  │   PROMETHEUS     │              │     GRAFANA      │      │
│  │   Metrics Store  │◄────────────►│   Dashboards     │      │
│  │   Port: 9090     │              │   Port: 3000     │      │
│  │                  │              │                  │      │
│  │  ✓ 30s Scrape    │              │  ✓ Alerts        │      │
│  │  ✓ 30-day Store  │              │  ✓ Graphs        │      │
│  │  ✓ 8 Jobs        │              │  ✓ Auth Support  │      │
│  └────────┬─────────┘              └──────────────────┘      │
│           │                                                   │
│           │ Scrapes every 30s from:                          │
│           │                                                   │
│        ┌──┴──┬──────┬──────┬──────┬──────┬──────┬─────┐      │
│        ▼     ▼      ▼      ▼      ▼      ▼      ▼     ▼      │
│      Auth  Account Trans  Notif  Rabbit PG-Auth PG-Acc PG-Trans
│      /act  /act    /act   /act   :15672  :9187  :9187 :9187  │
│      /prom /prom   /prom  /prom  Exporter Export Export Export│
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │         CENTRALIZED LOGGING (JSON Driver)              │ │
│  │                                                         │ │
│  │  Each Service logs in JSON format:                    │ │
│  │  - Max 10MB per file                                  │ │
│  │  - 3 files rotation                                   │ │
│  │  - Accessible via: docker-compose logs <service>     │ │
│  │  - Stored in: /var/lib/docker/containers/...         │ │
│  │                                                         │ │
│  │  Log Fields:                                           │ │
│  │  ├─ timestamp (ISO 8601)                              │ │
│  │  ├─ level (INFO, WARN, ERROR)                         │ │
│  │  ├─ message                                           │ │
│  │  ├─ service                                           │ │
│  │  └─ request_id (for tracing)                          │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │    HEALTH CHECKS (Built-in Docker Health)              │ │
│  │                                                         │ │
│  │  Interval: 30s (all services)                          │ │
│  │  Timeout: 10s per check                               │ │
│  │  Retries: 3 before marking unhealthy                  │ │
│  │                                                         │ │
│  │  Monitored Endpoints:                                  │ │
│  │  ├─ /actuator/health (Spring Boot Services)           │ │
│  │  ├─ /health (Nginx, RabbitMQ)                         │ │
│  │  ├─ pg_isready (PostgreSQL)                           │ │
│  │  └─ curl (Nginx/Gateway)                              │ │
│  │                                                         │ │
│  │  Auto-recovery: Docker restarts failed containers     │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## CI/CD Pipeline Flow

```
LOCAL DEVELOPER
    │
    ├─ git push origin develop
    │
    ▼
GITLAB REPOSITORY
    │
    ├─ Webhook triggered
    │
    ▼
┌──────────────────────────────────────────────────────┐
│          GITLAB CI/CD PIPELINE (6 Stages)            │
├──────────────────────────────────────────────────────┤
│                                                      │
│  STAGE 1: LINT                                       │
│  ├─ Validate docker-compose.prod.yml                │
│  └─ ✓ PASS → Continue to next stage                 │
│                                                      │
│  STAGE 2: BUILD-PUSH (Parallel - 5 jobs)            │
│  ├─ build:auth-service                              │
│  │  ├─ docker build (FROM eclipse-temurin:17-jre)   │
│  │  ├─ Tag: :$IMAGE_TAG (commit SHA)                │
│  │  ├─ Tag: :latest                                 │
│  │  └─ docker push → registry                       │
│  ├─ build:account-service                           │
│  │  └─ (same flow)                                  │
│  ├─ build:transaction-service                       │
│  │  └─ (same flow)                                  │
│  ├─ build:notification-service                      │
│  │  └─ (same flow)                                  │
│  └─ build:frontend                                  │
│     ├─ npm run build                                │
│     ├─ docker build (FROM nginx:alpine)             │
│     └─ docker push → registry                       │
│                                                      │
│  All builds complete (2-5 minutes)                   │
│                                                      │
│  STAGE 3: DEPLOY-STAGING (Auto on develop)          │
│  ├─ SSH → staging-vps                               │
│  ├─ Backup BD (pg_dump)                             │
│  ├─ Update IMAGE_TAG in .env                        │
│  ├─ docker-compose pull                             │
│  ├─ docker-compose up -d                            │
│  ├─ Wait 30s                                        │
│  └─ Run health checks                               │
│     ├─ GET /api/auth/health → 200 ✓                │
│     ├─ GET /api/accounts/health → 200 ✓            │
│     └─ GET /api/transactions/health → 200 ✓        │
│                                                      │
│  If health checks OK:                                │
│  ├─ 🟢 Staging deployment successful                │
│  ├─ Team can test new features                      │
│  └─ Logs in /var/log/banking/deploy.log            │
│                                                      │
│  ───────────────────────────────────────────────────│
│                                                      │
│  STAGE 4: DEPLOY-PRODUCTION (Auto on main)          │
│  ├─ SSH → production-vps                            │
│  ├─ CRITICAL: Backup BD to /backups/banking/       │
│  ├─ Update IMAGE_TAG in .env                        │
│  ├─ docker-compose pull                             │
│  ├─ docker-compose up -d (rolling update)           │
│  ├─ Wait 40s                                        │
│  ├─ Run health checks (5 attempts, 5s interval)     │
│  │  ├─ GET /api/auth/health → 200 ✓                │
│  │  ├─ GET /api/accounts/health → 200 ✓            │
│  │  ├─ GET /api/transactions/health → 200 ✓        │
│  │  ├─ GET /health (Nginx) → 200 ✓                 │
│  │  └─ If any fail, retry up to 5 times            │
│  │                                                   │
│  │  If all fail:                                     │
│  │  └─ 🔴 Automatic rollback triggered              │
│  │     ├─ docker-compose down                       │
│  │     ├─ restore-db.sh (full restore)              │
│  │     ├─ docker-compose up -d                      │
│  │     └─ Alert team (Slack notification)           │
│  │                                                   │
│  If health checks OK:                                │
│  ├─ 🟢 Production deployment successful             │
│  ├─ All services running live                       │
│  ├─ Logs in /var/log/banking/deploy.log            │
│  └─ Prometheus scraping metrics                     │
│                                                      │
│  ───────────────────────────────────────────────────│
│                                                      │
│  STAGE 5: ROLLBACK (Manual trigger)                 │
│  ├─ Available if production deployment fails        │
│  ├─ Find latest backup                              │
│  ├─ docker-compose down                             │
│  ├─ restore-db.sh all <backup_dir>                 │
│  ├─ docker-compose up -d                            │
│  └─ Verify with docker-compose ps                  │
│                                                      │
│  ───────────────────────────────────────────────────│
│                                                      │
│  STAGE 6: NOTIFY (Slack webhooks)                   │
│  ├─ On success:                                      │
│  │  └─ 🟢 Slack: "Deployment successful to PROD"    │
│  │     ├─ Commit SHA                                │
│  │     ├─ Branch                                    │
│  │     ├─ Author                                    │
│  │     └─ Pipeline URL                              │
│  │                                                   │
│  └─ On failure:                                      │
│     └─ 🔴 Slack: "Deployment failed"                │
│        ├─ Error details                             │
│        ├─ Rollback instructions                     │
│        └─ Pipeline URL for logs                     │
│                                                      │
└──────────────────────────────────────────────────────┘
                    │
                    ▼
PRODUCTION ENVIRONMENT
    │
    ├─ Services running live
    ├─ Load balancer (Nginx) active
    ├─ Prometheus collecting metrics
    ├─ Grafana displaying dashboards
    └─ Ready to serve users
```

---

## Data Flow Architecture

```
┌─ CLIENT REQUEST ─────────────────────────────────────────┐
│                                                           │
│  GET https://banking.example.com/api/accounts            │
│                                                           │
└──────────────────────────┬────────────────────────────────┘
                           │
                     TLS 1.2+ Encrypted
                           │
                    ┌──────▼──────────┐
                    │  NGINX GATEWAY  │
                    │ (Reverse Proxy)  │
                    │ ✓ HTTPS/TLS      │
                    │ ✓ Rate Limit     │
                    │ ✓ Security Hdrs  │
                    │ ✓ ModSecurity    │
                    │ ✓ Compression    │
                    └────────┬─────────┘
                             │
                  ┌──────────┼──────────┐
                  │                     │
                  ▼                     ▼
            ┌──────────────┐     ┌────────────────┐
            │ Load Balancer│     │ Route Based on  │
            │ (Round-robin)│     │ /api/accounts   │
            └──────┬───────┘     └────────┬───────┘
                   │                      │
        ┌──────────┴──────────┐           │
        │                     │           │
        ▼                     ▼           │
    ┌──────────────┐  ┌──────────────┐   │
    │Account Srv 1 │  │Account Srv 2 │◄──┘
    │  Port 8082   │  │  Port 8082   │
    └──────┬───────┘  └──────┬───────┘
           │                 │
           └────────┬────────┘
                    │
              ┌─────▼──────────┐
              │  RabbitMQ      │
              │  (if async)    │
              └─────┬──────────┘
                    │
            ┌───────▼──────────┐
            │  PostgreSQL      │
            │  account_db      │
            │  ├─ Query        │
            │  ├─ Process      │
            │  └─ Return data  │
            └───────┬──────────┘
                    │
            ┌───────▼──────────┐
            │  Result JSON     │
            │  {               │
            │   "accounts": [] │
            │  }              │
            └───────┬──────────┘
                    │
            ┌───────▼──────────┐
            │   NGINX Logs     │
            │   - Request time │
            │   - Status code  │
            │   - IP address   │
            │   - Metrics sent │
            └───────┬──────────┘
                    │
        ┌───────────┼──────────────┐
        │           │              │
        ▼           ▼              ▼
    ┌────────┐ ┌────────┐ ┌──────────────┐
    │ Client │ │Prometheus  Grafana    │
    │ (TLS)  │ │ Metrics    Dashboards │
    └────────┘ └────────┘ └──────────────┘
```

---

## Deployment Topology

### Single VPS (Current Setup)
```
┌─────────────────────────────────────┐
│      SINGLE VPS (4 CPU, 8 GB RAM)   │
│                                     │
│  Docker Host (Ubuntu 22.04 LTS)    │
│  ├─ Docker Engine 24+              │
│  ├─ Docker Compose v2+             │
│  │                                 │
│  ├─ Container: Nginx Gateway       │
│  ├─ Container: Auth Service        │
│  ├─ Container: Account Service     │
│  ├─ Container: Transaction Service │
│  ├─ Container: Notification Svc    │
│  ├─ Container: Frontend            │
│  ├─ Container: RabbitMQ            │
│  ├─ Container: PostgreSQL (auth)   │
│  ├─ Container: PostgreSQL (account)│
│  ├─ Container: PostgreSQL (trans)  │
│  ├─ Container: Prometheus          │
│  ├─ Container: Grafana             │
│  ├─ Container: Certbot             │
│  │                                 │
│  └─ Volumes:                       │
│     ├─ /postgres-*-data            │
│     ├─ /rabbitmq-data              │
│     ├─ /prometheus-data            │
│     ├─ /grafana-data               │
│     ├─ /nginx-logs                 │
│     ├─ /backups/banking            │
│     └─ /var/log/banking            │
│                                     │
└─────────────────────────────────────┘
```

### Multi-VPS with Load Balancing (Recommended for Scale)
```
┌────────────────────────────────────────────────┐
│             LOAD BALANCER (CloudFlare/AWS)     │
└─────────────┬──────────────────────┬───────────┘
              │                      │
    ┌─────────▼─────────┐  ┌────────▼──────────┐
    │   STAGING VPS     │  │  PRODUCTION VPS 1 │
    │  (Development)    │  │  (Primary)        │
    │  4 CPU, 8 GB      │  │  8 CPU, 16 GB     │
    │                   │  │                   │
    │  All Services     │  │  All Services     │
    │  Full Stack       │  │  Full Stack       │
    └───────────────────┘  │                   │
                           └──────┬────────────┘
                                  │
                           ┌──────▼──────────┐
                           │ PRODUCTION VPS 2│
                           │ (Replica/HA)    │
                           │ 8 CPU, 16 GB    │
                           │                 │
                           │ All Services    │
                           │ Full Stack      │
                           └──────────────────┘
                                  │
                           ┌──────▼──────────┐
                           │  DB Replication │
                           │  (Master-Slave) │
                           │  Shared Storage │
                           └──────────────────┘
```

---

## Security Layers

```
┌─────────────────────────────────────────────────────────────┐
│                     INTERNET                                │
└────────────────────┬────────────────────────────────────────┘
                     │
          ┌──────────▼──────────┐
          │  DDoS Protection    │  (CDN Level)
          │  (CloudFlare/AWS)   │
          └──────────┬──────────┘
                     │
          ┌──────────▼──────────────────┐
          │  TLS 1.2+ Encryption        │  ← HTTPS Mandatory
          │  (Let's Encrypt + Certbot)  │
          │  OCSP Stapling              │
          └──────────┬──────────────────┘
                     │
          ┌──────────▼──────────────────┐
          │  NGINX Security             │
          │  - Security Headers         │
          │  - Rate Limiting (5 req/s)  │
          │  - Request Validation       │
          └──────────┬──────────────────┘
                     │
          ┌──────────▼──────────────────┐
          │  ModSecurity (WAF)          │  ← Web App Firewall
          │  - SQL Injection Detection  │
          │  - XSS Prevention           │
          │  - Path Traversal Block     │
          │  - Command Injection Block  │
          └──────────┬──────────────────┘
                     │
          ┌──────────▼──────────────────┐
          │  Application Level          │
          │  - JWT Authentication       │
          │  - Authorization (RBAC)     │
          │  - Input Validation         │
          │  - SQL Parameterization     │
          └──────────┬──────────────────┘
                     │
          ┌──────────▼──────────────────┐
          │  Database Security          │
          │  - Encrypted Passwords      │
          │  - Isolated Networks        │
          │  - No Public Access         │
          │  - Connection Pooling       │
          └─────────────────────────────┘
```

---

## Data Persistence & Backup

```
┌────────────────────────────────────────────┐
│         PRODUCTION DATABASES               │
├────────────────────────────────────────────┤
│                                            │
│  PostgreSQL auth_db      PostgreSQL        │
│  ├─ Users Table          account_db        │
│  ├─ Roles Table          ├─ Accounts       │
│  ├─ Sessions Table       ├─ Balances       │
│  ├─ Permissions Table    └─ Transactions   │
│  │                                         │
│  PostgreSQL                                │
│  transaction_db                            │
│  ├─ Transactions                          │
│  ├─ Audit Logs                            │
│  └─ Financial Records                     │
│                                            │
└─────────────┬──────────────────────────────┘
              │
     ┌────────▼─────────┐
     │   EVERY NIGHT    │
     │   (2 AM - Cron)  │
     └────────┬─────────┘
              │
     ┌────────▼──────────────────┐
     │  backup-prod.sh Script    │
     │  ├─ pg_dump (all DBs)     │
     │  ├─ gzip compression      │
     │  ├─ timestamp directory   │
     │  └─ config backup         │
     └────────┬──────────────────┘
              │
     ┌────────▼──────────────────┐
     │ /backups/banking/         │
     │ ├─ 2026-05-06_02-00-00/   │
     │ │  ├─ auth_db.sql.gz      │
     │ │  ├─ account_db.sql.gz   │
     │ │  ├─ transaction_db.sql  │
     │ │  ├─ .env.backup         │
     │ │  ├─ nginx.conf.backup   │
     │ │  └─ docker-compose.yml  │
     │ │                          │
     │ ├─ 2026-05-05_02-00-00/   │
     │ ├─ 2026-05-04_02-00-00/   │
     │ └─ ... (30 day retention)  │
     └────────┬──────────────────┘
              │
     ┌────────▼──────────────────┐
     │  Optional Cloud Sync      │
     │  ├─ AWS S3                │
     │  ├─ Azure Blob            │
     │  └─ rsync to backup VPS   │
     └───────────────────────────┘

RESTORE PROCESS (On Disaster):
     ┌──────────────────────────┐
     │ ./restore-db.sh all      │
     │ /backups/banking/latest  │
     └────────┬─────────────────┘
              │
     ┌────────▼──────────────────┐
     │ • Decompress .sql.gz      │
     │ • Drop current tables     │
     │ • Restore from SQL dump   │
     │ • Verify integrity        │
     │ • Resume services         │
     └───────────────────────────┘
```

---

## Summary

This architecture provides:

✅ **High Availability**
- Multi-service deployment
- Load balancing (Nginx)
- Health checks & auto-recovery
- Backup & restore capability

✅ **Security**
- HTTPS/TLS encryption
- WAF (ModSecurity)
- Non-root containers
- Environment variable secrets
- Network isolation

✅ **Observability**
- Prometheus monitoring (30s intervals)
- Grafana dashboards
- Centralized JSON logging
- Health check endpoints

✅ **Scalability**
- Containerized services
- Load balancer ready
- Database separation
- Stateless app tier

✅ **Disaster Recovery**
- Daily automated backups
- 30-day retention
- Rollback capability
- Restore scripts

✅ **DevOps**
- CI/CD pipeline (GitLab)
- Automated builds & deploys
- Staging environment
- Production safeguards

---

## Next Steps

1. Deploy to production VPS
2. Configure monitoring & alerts
3. Establish backup procedures
4. Test disaster recovery
5. Implement WAF rules (ModSecurity)
6. Plan for horizontal scaling
