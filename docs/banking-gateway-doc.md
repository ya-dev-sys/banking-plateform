# Banking API Gateway - Documentation

## 🎯 Vue d'ensemble

Plateforme microservices bancaire sécurisée avec authentification JWT, rate limiting distribué et observabilité complète.

**Objectif** : Fournir une infrastructure prête pour applications bancaires (web/mobile).

---

## ⚡ Démarrage Rapide

```bash
# Clone le projet
git clone https://github.com/votre-username/banking-api-gateway.git
cd banking-api-gateway

# Démarre tout avec Docker
docker compose up -d

# Attends 2 minutes que tout soit UP
docker compose ps

# Teste l'API
curl http://localhost:8080/actuator/health
```

**Accès :**

- API Gateway : http://localhost:8080
- Eureka Dashboard : http://localhost:8761
- Zipkin Tracing : http://localhost:9411
- Grafana : http://localhost:3000 (admin/admin)

---

## 🏗️ Architecture

### Diagramme Global

```
                    [CLIENT]
                       │
                       ↓
              ┌────────────────┐
              │  API GATEWAY   │ :8080
              │  - Routing     │
              │  - Auth JWT    │
              │  - Rate Limit  │
              └────────┬───────┘
                       │
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
  ┌──────────┐  ┌──────────┐  ┌──────────┐
  │   Auth   │  │ Account  │  │   User   │
  │ Service  │  │ Service  │  │ Service  │
  │  :8081   │  │  :8082   │  │  :8083   │
  └────┬─────┘  └────┬─────┘  └────┬─────┘
       │            │              │
       └────────────┴──────────────┘
                    │
        ┌───────────┼───────────┐
        ↓           ↓           ↓
   [PostgreSQL] [Redis]    [Kafka]
     :5432       :6379      :9092
```

### Pattern : Hexagonal (Ports & Adapters)

```
Service Structure:
├── domain/           # Logique métier pure
│   ├── model/       # Entités (Account, Transaction)
│   ├── port/in/     # Use cases (interfaces)
│   ├── port/out/    # Repositories (interfaces)
│   └── service/     # Implémentation logique
├── adapter/
│   ├── in/web/      # REST Controllers
│   └── out/         # JPA, Kafka, etc.
└── application/     # Configuration Spring
```

---

## 🚀 Features

### 1. Authentification JWT

- **Algo** : RS256 (clés asymétriques)
- **Expiration** : Access token 30min, Refresh token 7j
- **Révocation** : Blacklist Redis

```
POST /api/auth/register  # Créer compte
POST /api/auth/login     # Connexion → JWT
POST /api/auth/refresh   # Renouveler token
POST /api/auth/logout    # Blacklist token
```

### 2. Rate Limiting (Redis)

- **Authentifié** : 100 req/min
- **Anonyme** : 20 req/min
- **Login** : 5 tentatives/min (anti brute-force)

**Headers réponse :**

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 42
X-RateLimit-Reset: 1702987800
```

### 3. Circuit Breaker (Resilience4j)

- **Seuil** : 50% erreurs sur 10 appels
- **État ouvert** : 30 secondes
- **Fallback** : Réponse dégradée

### 4. Distributed Tracing (Zipkin)

- Trace ID propagé via headers
- Visualisation complète des appels
- Debug latence end-to-end

### 5. Sécurité Headers

```
Strict-Transport-Security: max-age=31536000
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Content-Security-Policy: default-src 'self'
```

---

## 🛠️ Services

### 1. API Gateway (Port 8080)

**Rôle** : Point d'entrée unique, routing, sécurité

**Responsabilités :**

- Validation JWT
- Rate limiting distribué
- Circuit breaker
- Logging + tracing

### 2. Auth Service (Port 8081)

**Rôle** : Authentification et autorisation

**Endpoints :**

- `POST /auth/register` - Inscription
- `POST /auth/login` - Connexion
- `POST /auth/refresh` - Renouvellement token
- `POST /auth/logout` - Déconnexion

**Base de données :** Table `users` (email, password_hash, roles)

### 3. Account Service (Port 8082)

**Rôle** : Gestion des comptes bancaires

**Endpoints :**

- `GET /accounts` - Liste comptes utilisateur
- `POST /accounts` - Créer compte (CHECKING/SAVINGS)
- `GET /accounts/{id}` - Détails compte
- `GET /accounts/{id}/balance` - Solde actuel
- `POST /accounts/{id}/transfer` - Virement
- `GET /accounts/{id}/transactions` - Historique

**Base de données :** Tables `accounts`, `transactions`

**Exemple Use Case :**

```
User crée un compte CHECKING
→ POST /accounts {"type": "CHECKING", "currency": "EUR"}
→ Service génère accountNumber
→ Crée compte avec balance = 0
→ Publie event "AccountCreated" dans Kafka
```

### 4. User Service (Port 8083)

**Rôle** : Gestion profil utilisateur

**Endpoints :**

- `GET /users/me` - Profil utilisateur
- `PATCH /users/me` - Modifier profil
- `GET /users/{id}` - Info utilisateur (admin only)

**Base de données :** Table `user_profiles` (firstName, lastName, phone, address)

---

## 📦 Infrastructure

### Services Obligatoires

| Service           | Port | Rôle                          |
| ----------------- | ---- | ----------------------------- |
| **Config Server** | 8888 | Configuration centralisée     |
| **Eureka Server** | 8761 | Service discovery             |
| **PostgreSQL**    | 5432 | Base de données relationnelle |
| **Redis**         | 6379 | Cache + Rate limiting         |
| **Kafka**         | 9092 | Event streaming               |
| **Zookeeper**     | 2181 | Coordination Kafka            |
| **Zipkin**        | 9411 | Distributed tracing           |
| **Prometheus**    | 9090 | Métriques                     |
| **Grafana**       | 3000 | Dashboards                    |

### docker-compose.yml

```yaml
version: "3.8"

services:
  # --- INFRASTRUCTURE ---
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: banking
      POSTGRES_USER: banking_user
      POSTGRES_PASSWORD: banking_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7.2-alpine
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    ports:
      - "9092:9092"

  zipkin:
    image: openzipkin/zipkin:latest
    ports:
      - "9411:9411"

  # --- SPRING CLOUD ---
  config-server:
    build: ./config-server
    ports:
      - "8888:8888"

  eureka-server:
    build: ./eureka-server
    ports:
      - "8761:8761"
    depends_on:
      - config-server

  # --- MICROSERVICES ---
  auth-service:
    build: ./auth-service
    ports:
      - "8081:8081"
    depends_on:
      - eureka-server
      - postgres
      - redis
    environment:
      SPRING_PROFILES_ACTIVE: docker

  account-service:
    build: ./account-service
    ports:
      - "8082:8082"
    depends_on:
      - eureka-server
      - postgres
      - kafka

  user-service:
    build: ./user-service
    ports:
      - "8083:8083"
    depends_on:
      - eureka-server
      - postgres

  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    depends_on:
      - eureka-server
      - redis
      - auth-service
      - account-service
      - user-service

volumes:
  postgres-data:
```

---

## 📚 Stack Technique

### Backend

- **Java** 21
- **Spring Boot** 4.0.x
- **Spring Cloud Gateway** 4.1.0
- **Spring Cloud Netflix Eureka**
- **Spring Security** 7.0.x

### Sécurité

- **JJWT** 0.12.5 (JWT)
- **BCrypt** (hash passwords)

### Résilience

- **Resilience4j** 2.1.0 (Circuit Breaker)
- **Bucket4j** 8.7.0 (Rate Limiting)

### Data

- **PostgreSQL** 16
- **Redis** 7.2
- **Spring Data JPA**
- **Flyway** (migrations)

### Messaging

- **Apache Kafka** 3.6.0

### Observabilité

- **Spring Cloud Sleuth** (Tracing)
- **Zipkin**
- **Micrometer** + **Prometheus**
- **Grafana**

### Testing

- **JUnit 5**
- **Testcontainers**
- **RestAssured**

---

## 🔧 Configuration

### Variables d'Environnement (.env)

```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_DB=banking
POSTGRES_USER=banking_user
POSTGRES_PASSWORD=changeme

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT
JWT_SECRET=your-256-bit-secret-key-generate-with-openssl
JWT_EXPIRATION_MS=1800000  # 30 min

# Rate Limiting
RATE_LIMIT_DEFAULT=100
RATE_LIMIT_WINDOW_SEC=60
```

### Générer JWT Secret

```bash
openssl rand -base64 32
```

---

## 📡 API Examples

### 1. Inscription + Login

```bash
# Inscription
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
# → Retourne { "accessToken": "eyJ...", "refreshToken": "..." }
```

### 2. Créer un Compte

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "type": "CHECKING",
    "currency": "EUR",
    "initialDeposit": 100.00
  }'
# → Retourne compte créé avec accountNumber
```

### 3. Consulter Solde

```bash
curl -X GET http://localhost:8080/api/accounts/abc-123/balance \
  -H "Authorization: Bearer eyJ..."
# → { "balance": 100.00, "currency": "EUR" }
```

### 4. Virement

```bash
curl -X POST http://localhost:8080/api/accounts/abc-123/transfer \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "targetAccountId": "xyz-789",
    "amount": 50.00,
    "description": "Remboursement"
  }'
```

---

## 🧪 Tests

### Lancer les Tests

```bash
# Tests unitaires
mvn test

# Tests intégration (avec Testcontainers)
mvn verify

# Coverage report
mvn clean verify jacoco:report
open target/site/jacoco/index.html
```

### Tests de Charge

```bash
# JMeter (1000 req/s pendant 5 min)
jmeter -n -t load-test.jmx -l results.jtl
```

**Objectifs :**

- P95 latency < 100ms
- P99 latency < 250ms
- Throughput ≥ 1000 req/s

---

## 📊 Monitoring

### Métriques Prometheus

```
http://localhost:9090
```

**Queries utiles :**

```promql
# Latence P95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Taux erreur
rate(http_server_requests_seconds_count{status="5xx"}[5m])

# Throughput
rate(http_server_requests_seconds_count[1m])
```

### Dashboards Grafana

```
http://localhost:3000 (admin/admin)
```

**Dashboards pré-configurés :**

- API Gateway Health
- Service Performance
- Business Metrics

---

## 🚨 Troubleshooting

### Service ne démarre pas

```bash
# Vérifier les logs
docker compose logs -f gateway

# Vérifier Eureka
open http://localhost:8761
# → Tous les services doivent être UP
```

### Rate Limit Redis

```bash
# Vérifier connexion Redis
docker exec -it banking-redis redis-cli
> PING
PONG

# Voir les clés rate limit
> KEYS rate_limit:*
```

### JWT Invalid

```bash
# Vérifier token expiré
# Utiliser https://jwt.io pour décoder

# Vérifier secret cohérent entre services
grep JWT_SECRET .env
```

---

## 📁 Structure du Projet

```
banking-platform/
├── api-gateway/
├── auth-service/
├── account-service/
├── user-service/
├── config-server/
├── eureka-server/
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 🎓 Pour Aller Plus Loin

### Améliorations Possibles

1. **Sécurité**

   - 2FA (TOTP)
   - Certificate pinning
   - API Key management

2. **Performance**

   - Cache distribué (Redis Cluster)
   - Database read replicas
   - CDN pour static assets

3. **Features**

   - WebSocket notifications
   - GraphQL API
   - Multi-device sessions

4. **DevOps**
   - Kubernetes deployment
   - Auto-scaling
   - Blue/Green deployment

---

## 📄 Licence

MIT License

---

**Version** : 1.0.0
**Date** : Décembre 2025
