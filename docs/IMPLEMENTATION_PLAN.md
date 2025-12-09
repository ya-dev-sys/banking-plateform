# Plan d'Implémentation - Banking API Gateway

## 🎯 Objectif

Construire une plateforme microservices bancaire complète en **3 semaines**.

---

## 📅 Timeline Globale

| Jours     | Service              | Objectif                  |
| --------- | -------------------- | ------------------------- |
| **1**     | Setup Infrastructure | Docker Compose prêt       |
| **2**     | Config + Eureka      | Services Spring Cloud     |
| **3-4**   | Auth Service         | JWT + Register/Login      |
| **5-6**   | API Gateway          | Routing + Sécurité        |
| **7-9**   | Account Service      | Logique métier principale |
| **10-11** | User Service         | Service complémentaire    |
| **12-14** | Tests                | Coverage 80%+             |
| **15**    | Monitoring           | Prometheus + Grafana      |
| **16-17** | Docs + CI/CD         | Finitions                 |

**Durée totale : 17 jours (3 semaines à temps plein)**

---

## 🚀 PHASE 1 : Setup Initial (Jour 1)

### Objectif

Infrastructure Docker opérationnelle (PostgreSQL, Redis, Kafka, Zipkin)

### Étapes

#### 1.1 Créer la structure racine

```
banking-platform/
├── .gitignore
├── README.md
├── docker-compose.yml
└── .env.example
```

#### 1.2 Initialiser Git

- [ ] Créer repository GitHub `banking-api-gateway`
- [ ] Cloner localement
- [ ] Créer `.gitignore` (Java, Maven, IDE)
- [ ] Premier commit avec structure vide

#### 1.3 Créer docker-compose.yml

Ajouter les services suivants :

- [ ] **PostgreSQL** (port 5432)

  - Database : `banking`
  - User : `banking_user`
  - Password : variable `.env`

- [ ] **Redis** (port 6379)

  - Image : `redis:7.2-alpine`
  - Pas de password en dev

- [ ] **Kafka + Zookeeper**

  - Kafka : port 9092
  - Zookeeper : port 2181
  - Image : `confluentinc/cp-kafka`

- [ ] **Zipkin** (port 9411)
  - Image : `openzipkin/zipkin:latest`

#### 1.4 Créer .env.example

Variables d'environnement template :

```
POSTGRES_PASSWORD=changeme
JWT_SECRET=generate-me
REDIS_HOST=localhost
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

#### 1.5 Tester l'infrastructure

- [ ] `docker compose up -d`
- [ ] Vérifier tous les conteneurs UP : `docker compose ps`
- [ ] Se connecter PostgreSQL : `psql -h localhost -U banking_user -d banking`
- [ ] Tester Redis : `docker exec -it <container> redis-cli PING`
- [ ] Accéder Zipkin : http://localhost:9411

### ✅ Validation Phase 1

- [ ] Tous les conteneurs démarrent sans erreur
- [ ] Connexion PostgreSQL OK
- [ ] Redis répond PONG
- [ ] Zipkin UI accessible

---

## ⚙️ PHASE 2 : Config Server (Jour 2 - Matin)

### Objectif

Centraliser la configuration de tous les services

### Étapes

#### 2.1 Créer le projet

- [ ] Dossier `config-server/`
- [ ] Spring Initializr avec :
  - Spring Boot 3.5.7
  - Java 21
  - Maven
  - Dependencies : `Config Server`

#### 2.2 Configuration

- [ ] Activer `@EnableConfigServer` dans classe principale
- [ ] Configurer `application.yml` :

  - Port : 8888
  - Git backend OU native (filesystem)

- [ ] Créer dossier `config-repo/` avec fichiers :
  - `application.yml` (config commune)
  - `auth-service.yml`
  - `account-service.yml`
  - `user-service.yml`
  - `gateway.yml`

#### 2.3 Tester

- [ ] Démarrer le service : `./mvnw spring-boot:run`
- [ ] Accéder : http://localhost:8888/actuator/health
- [ ] Vérifier config disponible : http://localhost:8888/auth-service/default

### ✅ Validation Phase 2

- [ ] Service démarre en < 30 secondes
- [ ] Health check retourne `{"status":"UP"}`
- [ ] Configurations accessibles via HTTP

---

## 🔍 PHASE 3 : Eureka Server (Jour 2 - Après-midi)

### Objectif

Service discovery pour communication inter-services

### Étapes

#### 3.1 Créer le projet

- [ ] Dossier `eureka-server/`
- [ ] Spring Initializr avec :
  - Dependencies : `Eureka Server`, `Config Client`

#### 3.2 Configuration

- [ ] Activer `@EnableEurekaServer`
- [ ] Configurer `bootstrap.yml` :

  - Pointer vers Config Server (port 8888)

- [ ] Dans Config Server, créer `eureka-server.yml` :
  - Port : 8761
  - `registerWithEureka: false`
  - `fetchRegistry: false`

#### 3.3 Tester

- [ ] Démarrer le service
- [ ] Accéder dashboard : http://localhost:8761
- [ ] Vérifier "No instances available" (normal, pas de service enregistré)

### ✅ Validation Phase 3

- [ ] Dashboard Eureka accessible
- [ ] Config Server apparaît dans dashboard
- [ ] Aucune erreur dans les logs

---

## 🔐 PHASE 4 : Auth Service (Jours 3-4)

### Objectif

**PREMIER MICROSERVICE** - Authentification JWT

### Étapes

#### 4.1 Créer structure hexagonale

```
auth-service/
├── src/main/java/com/banking/auth/
│   ├── domain/
│   │   ├── model/
│   │   │   └── User.java
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   ├── RegisterUserUseCase.java
│   │   │   │   └── LoginUserUseCase.java
│   │   │   └── out/
│   │   │       └── UserRepository.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   └── JwtService.java
│   │   └── exception/
│   │       ├── UserAlreadyExistsException.java
│   │       └── InvalidCredentialsException.java
│   ├── adapter/
│   │   ├── in/
│   │   │   └── web/
│   │   │       ├── AuthController.java
│   │   │       └── dto/
│   │   │           ├── RegisterRequest.java
│   │   │           ├── LoginRequest.java
│   │   │           └── AuthResponse.java
│   │   └── out/
│   │       └── persistence/
│   │           ├── UserEntity.java
│   │           ├── UserJpaRepository.java
│   │           └── UserRepositoryImpl.java
│   └── application/
│       ├── config/
│       │   └── SecurityConfig.java
│       └── AuthServiceApplication.java
└── src/main/resources/
    ├── application.yml
    └── db/migration/
        └── V1__create_users_table.sql
```

#### 4.2 Spring Initializr

Dependencies :

- [ ] Spring Web
- [ ] Spring Data JPA
- [ ] Spring Security
- [ ] PostgreSQL Driver
- [ ] Eureka Discovery Client
- [ ] Config Client
- [ ] Validation
- [ ] Lombok

#### 4.3 Ajouter dépendances JWT

Dans `pom.xml` :

- [ ] JJWT API
- [ ] JJWT Impl
- [ ] JJWT Jackson

#### 4.4 Implémenter Domain Layer

- [ ] Créer `User.java` (POJO pure, pas d'annotations JPA)
- [ ] Créer interfaces use cases (RegisterUserUseCase, LoginUserUseCase)
- [ ] Créer interface repository (UserRepository)
- [ ] Implémenter `AuthService` (logique métier)
- [ ] Implémenter `JwtService` (génération/validation tokens)

#### 4.5 Implémenter Adapters

**IN (Web) :**

- [ ] Créer DTOs (RegisterRequest, LoginRequest, AuthResponse)
- [ ] Créer `AuthController` avec endpoints :
  - POST /auth/register
  - POST /auth/login
  - POST /auth/refresh
  - POST /auth/logout

**OUT (Persistence) :**

- [ ] Créer `UserEntity` (avec @Entity)
- [ ] Créer `UserJpaRepository` (extends JpaRepository)
- [ ] Créer `UserRepositoryImpl` (implémente UserRepository du domain)
- [ ] Mapper Entity ↔ Domain Model

#### 4.6 Configuration Sécurité

- [ ] `SecurityConfig` : désactiver CSRF pour API REST
- [ ] Configurer BCrypt password encoder
- [ ] Autoriser `/auth/**` sans authentification

#### 4.7 Migration Database (Flyway)

Créer `V1__create_users_table.sql` :

- [ ] Table `users` avec : id, email (unique), password_hash, roles, created_at

#### 4.8 Configuration

Dans Config Server, `auth-service.yml` :

- [ ] Port : 8081
- [ ] Database URL, user, password
- [ ] JWT secret
- [ ] JWT expiration

#### 4.9 Tester

- [ ] Démarrer le service
- [ ] Vérifier enregistrement dans Eureka
- [ ] POST /auth/register avec Postman
- [ ] POST /auth/login → vérifier JWT retourné
- [ ] Décoder JWT sur jwt.io

### ✅ Validation Phase 4

- [ ] Register crée un user en BDD
- [ ] Login retourne access + refresh tokens
- [ ] Password hashé avec BCrypt
- [ ] Service visible dans Eureka dashboard
- [ ] Logs sans erreur

---

## 🌐 PHASE 5 : API Gateway (Jours 5-6)

### Objectif

Point d'entrée unique avec sécurité centralisée

### Étapes

#### 5.1 Créer le projet

- [ ] Dossier `api-gateway/`
- [ ] Spring Initializr avec :
  - Spring Cloud Gateway
  - Eureka Discovery Client
  - Config Client
  - Redis Reactive
  - Actuator

#### 5.2 Structure

```
api-gateway/
├── src/main/java/com/banking/gateway/
│   ├── filter/
│   │   ├── AuthenticationFilter.java
│   │   ├── LoggingFilter.java
│   │   ├── RateLimitFilter.java
│   │   └── CircuitBreakerFilter.java
│   ├── config/
│   │   ├── GatewayConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── RedisConfig.java
│   │   └── CorsConfig.java
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   └── JwtAuthenticationManager.java
│   └── GatewayApplication.java
└── src/main/resources/
    └── application.yml
```

#### 5.3 Configuration Routes

Dans `application.yml` :

- [ ] Route vers auth-service : `/api/auth/**` → `lb://AUTH-SERVICE`
- [ ] Prédicat Path + StripPrefix
- [ ] Filters : CircuitBreaker, Retry

#### 5.4 Implémenter Filtres

**AuthenticationFilter :**

- [ ] Extraire JWT du header `Authorization: Bearer <token>`
- [ ] Valider signature + expiration
- [ ] Check blacklist Redis (tokens révoqués)
- [ ] Injecter userId dans headers pour services backend

**LoggingFilter :**

- [ ] Log request (method, path, traceId)
- [ ] Log response (status, latency)

**RateLimitFilter :**

- [ ] Implémenter sliding window avec Redis
- [ ] Limite : 100 req/min par user
- [ ] Retourner 429 si dépassé
- [ ] Headers : X-RateLimit-Remaining, X-RateLimit-Reset

**CircuitBreakerFilter :**

- [ ] Configurer Resilience4j
- [ ] Fallback response si service down

#### 5.5 Configuration Sécurité

- [ ] CORS : autoriser origins spécifiques
- [ ] Headers sécurité (HSTS, X-Frame-Options, etc.)
- [ ] Désactiver CSRF

#### 5.6 Tester

- [ ] Démarrer Gateway
- [ ] Appeler auth via Gateway : `POST http://localhost:8080/api/auth/login`
- [ ] Vérifier routing vers auth-service
- [ ] Tester JWT validation (token invalide → 401)
- [ ] Tester rate limiting (101 requêtes rapides → 429)

### ✅ Validation Phase 5

- [ ] Routing fonctionne (Gateway → Auth Service)
- [ ] JWT validé correctement
- [ ] Rate limiting actif (Redis)
- [ ] Logs contiennent traceId
- [ ] Zipkin montre traces complètes

---

## 💰 PHASE 6 : Account Service (Jours 7-9)

### Objectif

Logique métier principale - Gestion comptes + virements

### Étapes

#### 6.1 Créer structure hexagonale

```
account-service/
├── src/main/java/com/banking/account/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Account.java
│   │   │   ├── Transaction.java
│   │   │   ├── Balance.java
│   │   │   └── Money.java (Value Object)
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   ├── CreateAccountUseCase.java
│   │   │   │   ├── GetBalanceUseCase.java
│   │   │   │   ├── TransferMoneyUseCase.java
│   │   │   │   └── GetTransactionsUseCase.java
│   │   │   └── out/
│   │   │       ├── AccountRepository.java
│   │   │       ├── TransactionRepository.java
│   │   │       └── EventPublisher.java
│   │   ├── service/
│   │   │   ├── AccountService.java
│   │   │   └── TransferService.java
│   │   └── exception/
│   │       ├── InsufficientFundsException.java
│   │       ├── AccountNotFoundException.java
│   │       └── InvalidTransferException.java
│   ├── adapter/
│   │   ├── in/
│   │   │   └── web/
│   │   │       ├── AccountController.java
│   │   │       └── dto/
│   │   └── out/
│   │       ├── persistence/
│   │       │   ├── AccountEntity.java
│   │       │   ├── TransactionEntity.java
│   │       │   ├── AccountJpaRepository.java
│   │       │   └── AccountRepositoryImpl.java
│   │       └── messaging/
│   │           └── KafkaEventPublisher.java
│   └── application/
│       ├── config/
│       │   ├── KafkaConfig.java
│       │   └── DatabaseConfig.java
│       └── AccountServiceApplication.java
└── src/main/resources/
    └── db/migration/
        ├── V1__create_accounts_table.sql
        └── V2__create_transactions_table.sql
```

#### 6.2 Spring Initializr

Dependencies :

- [ ] Spring Web
- [ ] Spring Data JPA
- [ ] PostgreSQL Driver
- [ ] Spring Kafka
- [ ] Eureka Discovery Client
- [ ] Config Client
- [ ] Validation
- [ ] Lombok

#### 6.3 Implémenter Domain Layer

**Models :**

- [ ] `Account` : id, userId, accountNumber, type (CHECKING/SAVINGS), balance, currency, status
- [ ] `Transaction` : id, sourceAccountId, targetAccountId, amount, type, status, timestamp
- [ ] `Money` : ValueObject (amount + currency)

**Services :**

- [ ] `AccountService` : createAccount, getAccount, listAccountsByUser
- [ ] `TransferService` : transferMoney, validateSufficientFunds

#### 6.4 Implémenter Adapters

**IN (Web) :**

- [ ] POST /accounts - Créer compte
- [ ] GET /accounts - Liste comptes user
- [ ] GET /accounts/{id} - Détails compte
- [ ] GET /accounts/{id}/balance - Solde
- [ ] POST /accounts/{id}/transfer - Virement
- [ ] GET /accounts/{id}/transactions - Historique

**OUT (Persistence) :**

- [ ] Entities JPA avec relations
- [ ] Repositories JPA
- [ ] Implémentations des ports domain

**OUT (Messaging) :**

- [ ] KafkaEventPublisher
- [ ] Publier événements :
  - `AccountCreated`
  - `TransferCompleted`
  - `TransferFailed`

#### 6.5 Migration Database

**V1\_\_create_accounts_table.sql :**

- [ ] Table `accounts` : id, user_id, account_number, type, balance, currency, status, created_at

**V2\_\_create_transactions_table.sql :**

- [ ] Table `transactions` : id, source_account_id, target_account_id, amount, currency, type, status, description, created_at

#### 6.6 Configuration Gateway

Dans Config Server, ajouter route dans `gateway.yml` :

- [ ] `/api/accounts/**` → `lb://ACCOUNT-SERVICE`
- [ ] Filters : Auth, RateLimit, CircuitBreaker

#### 6.7 Tester

- [ ] POST /api/accounts (via Gateway avec JWT)
- [ ] Vérifier compte créé en BDD
- [ ] GET /api/accounts/{id}/balance
- [ ] POST /api/accounts/{id}/transfer
- [ ] Vérifier événement Kafka publié
- [ ] Vérifier transaction en BDD

### ✅ Validation Phase 6

- [ ] CRUD comptes fonctionne
- [ ] Virement débite/crédite correctement
- [ ] Événements Kafka publiés
- [ ] Validation métier (solde insuffisant → erreur)
- [ ] Service visible dans Eureka

---

## 👤 PHASE 7 : User Service (Jours 10-11)

### Objectif

Gestion profil utilisateur (service plus simple)

### Étapes

#### 7.1 Créer structure hexagonale

```
user-service/
├── src/main/java/com/banking/user/
│   ├── domain/
│   │   ├── model/
│   │   │   └── UserProfile.java
│   │   ├── port/
│   │   └── service/
│   ├── adapter/
│   │   ├── in/web/
│   │   └── out/persistence/
│   └── application/
└── src/main/resources/
    └── db/migration/
        └── V1__create_user_profiles_table.sql
```

#### 7.2 Implémenter

**Endpoints :**

- [ ] GET /users/me - Profil utilisateur connecté
- [ ] PATCH /users/me - Modifier profil
- [ ] GET /users/{id} - Info user (admin only)

**Domain :**

- [ ] `UserProfile` : userId, firstName, lastName, phoneNumber, address, createdAt

#### 7.3 Configuration Gateway

- [ ] Route `/api/users/**` → `lb://USER-SERVICE`

#### 7.4 Tester

- [ ] GET /api/users/me (via Gateway avec JWT)
- [ ] PATCH /api/users/me (update phone)
- [ ] Vérifier extraction userId depuis JWT

### ✅ Validation Phase 7

- [ ] Profil récupérable
- [ ] Update fonctionne
- [ ] UserId extrait du JWT correctement

---

## 🧪 PHASE 8 : Tests (Jours 12-14)

### Objectif

Coverage 80%+ avec tests automatisés

### Étapes

#### 8.1 Tests Unitaires (Domain Services)

Pour chaque service :

- [ ] Tester `AuthService` (register, login, JWT generation)
- [ ] Tester `AccountService` (create, getBalance)
- [ ] Tester `TransferService` (transfer, validation)

**Objectif : 90%+ coverage domain layer**

#### 8.2 Tests d'Intégration (Testcontainers)

- [ ] Ajouter dépendance Testcontainers
- [ ] Créer base test class avec containers :
  - PostgreSQL
  - Redis
  - Kafka

**Tests à écrire :**

- [ ] Auth : Register → Login → JWT valide
- [ ] Account : Create → Get → Transfer
- [ ] Gateway : Rate limiting fonctionne
- [ ] Gateway : JWT validation fonctionne

#### 8.3 Tests End-to-End (User Journey)

Scénario complet :

- [ ] 1. User s'inscrit (register)
- [ ] 2. User se connecte (login)
- [ ] 3. User crée un compte (account)
- [ ] 4. User consulte solde (balance)
- [ ] 5. User fait un virement (transfer)
- [ ] 6. User vérifie transaction (history)

#### 8.4 Tests de Charge (JMeter)

- [ ] Créer plan JMeter : 1000 req/s pendant 5 min
- [ ] Endpoints à tester :
  - GET /api/accounts (read-heavy)
  - POST /api/accounts/{id}/transfer (write)

**Métriques à vérifier :**

- [ ] P95 latency < 100ms
- [ ] P99 latency < 250ms
- [ ] Error rate < 0.1%

#### 8.5 Rapport Coverage

- [ ] Configurer Jacoco plugin
- [ ] Générer rapport : `mvn clean verify jacoco:report`
- [ ] Vérifier coverage global ≥ 80%

### ✅ Validation Phase 8

- [ ] Tous les tests passent (green)
- [ ] Coverage ≥ 80%
- [ ] Tests d'intégration avec Testcontainers OK
- [ ] User journey E2E fonctionne
- [ ] Load test : objectifs atteints

---

## 📊 PHASE 9 : Monitoring (Jour 15)

### Objectif

Observabilité complète (Prometheus + Grafana)

### Étapes

#### 9.1 Ajouter Prometheus

Dans tous les services :

- [ ] Dépendance Micrometer Prometheus
- [ ] Activer endpoint `/actuator/prometheus`

Dans `docker-compose.yml` :

- [ ] Ajouter Prometheus (port 9090)
- [ ] Configurer scraping des services

#### 9.2 Configurer Grafana

- [ ] Ajouter Grafana dans Docker Compose (port 3000)
- [ ] Ajouter datasource Prometheus
- [ ] Importer dashboards :
  - Spring Boot 2.1 System Monitor
  - JVM Micrometer
  - Custom business metrics

#### 9.3 Métriques Custom

Dans Account Service :

- [ ] Counter : `transfers_total`
- [ ] Gauge : `accounts_balance_sum`
- [ ] Histogram : `transfer_amount_distribution`

#### 9.4 Alertes Prometheus

Créer `alerts.yml` :

- [ ] Alert si P95 latency > 1s
- [ ] Alert si error rate > 5%
- [ ] Alert si service down

#### 9.5 Vérifier Zipkin

- [ ] Tracer un appel complet (Gateway → Account → Kafka)
- [ ] Vérifier spans visibles
- [ ] Identifier bottleneck de latence

### ✅ Validation Phase 9

- [ ] Métriques visibles dans Prometheus
- [ ] Dashboards Grafana fonctionnels
- [ ] Traces Zipkin complètes
- [ ] Alertes configurées (test en coupant un service)

---

## 📚 PHASE 10 : Documentation & CI/CD (Jours 16-17)

### Objectif

Projet production-ready

### Étapes

#### 10.1 Documentation

- [ ] README.md complet avec :

  - Vue d'ensemble
  - Architecture diagram
  - Quick start (docker compose up)
  - API examples
  - Configuration

- [ ] CONTRIBUTING.md :

  - Standards de code
  - Process PR
  - Guidelines commit messages

- [ ] Swagger/OpenAPI :
  - Ajouter Springdoc OpenAPI dans chaque service
  - Documentation endpoints automatique

#### 10.2 Postman Collection

- [ ] Créer collection avec tous les endpoints
- [ ] Variables pour :
  - `baseUrl`
  - `token` (JWT)
  - `accountId`
- [ ] Tests automatisés (assertions)
- [ ] Exporter et commiter dans repo

#### 10.3 CI/CD (GitHub Actions)

Créer `.github/workflows/ci.yml` :

- [ ] Trigger : push + PR
- [ ] Jobs :
  - Build tous les services
  - Run tests (unit + integration)
  - SonarQube analysis
  - Build Docker images
  - Push vers registry

#### 10.4 SonarQube

- [ ] Configurer SonarCloud (gratuit pour open-source)
- [ ] Ajouter badge dans README
- [ ] Objectif : Grade A

#### 10.5 Deployment (Bonus)

Si temps disponible :

- [ ] Déployer sur Heroku/Railway
- [ ] OU créer Kubernetes manifests
- [ ] Script de déploiement automatique

### ✅ Validation Phase 10

- [ ] README clair et complet
- [ ] Postman collection testée
- [ ] CI GitHub Actions green
- [ ] SonarQube grade A ou B
- [ ] Projet déployé (optionnel)

---

## ✅ Checklist Finale - Projet Terminé

### Code

- [ ] Tous les services démarrent avec `docker compose up`
- [ ] Aucune erreur dans les logs
- [ ] Tests passent (green)
- [ ] Coverage ≥ 80%
- [ ] SonarQube grade A/B

### Fonctionnalités

- [ ] User peut s'inscrire
- [ ] User peut se connecter (JWT)
- [ ] User peut créer un compte
- [ ] User peut consulter solde
- [ ] User peut faire un virement
- [ ] User peut voir historique transactions

### Sécurité

- [ ] Passwords hashés (BCrypt)
- [ ] JWT avec signature RSA
- [ ] Rate limiting actif
- [ ] Headers sécurité présents
- [ ] Pas de secrets en dur

### Observabilité

- [ ] Métriques Prometheus
- [ ] Dashboards Grafana
- [ ] Traces Zipkin
- [ ] Logs structurés

### Documentation

- [ ] README avec quick start
- [ ] Architecture diagram
- [ ] Postman collection
- [ ] Swagger UI accessible

### CI/CD

- [ ] GitHub Actions configuré
- [ ] Tests automatiques
- [ ] Build Docker images

---

## 🎯 Ordre de Priorité (rappel)

**NE JAMAIS sauter d'étape !**

1. Infrastructure (jour 1)
2. Config Server (jour 2)
3. Eureka (jour 2)
4. Auth Service (jours 3-4)
5. Gateway (jours 5-6)
6. Account Service (jours 7-9)
7. User Service (jours 10-11)
8. Tests (jours 12-14)
9. Monitoring (jour 15)
10. Docs + CI/CD (jours 16-17)

**Règle d'or : Un service terminé = testé + documenté + commit avant de passer au suivant**

---

## 📞 En Cas de Blocage

### Erreurs Courantes

**Service ne démarre pas :**

- Vérifier port disponible
- Vérifier Config Server accessible
- Vérifier Eureka accessible

**JWT invalid :**

- Vérifier secret cohérent entre Auth et Gateway
- Vérifier expiration du token
- Décoder sur jwt.io

**Tests échouent :**

- Vérifier Testcontainers Docker disponible
- Vérifier ports libres
- Nettoyer : `mvn clean`

**Kafka events pas reçus :**

- Vérifier Kafka UP
- Vérifier topic créé
- Vérifier serialization/deserialization

---

**Bon courage ! 💪**

Tu as toutes les étapes. Commence par le Jour 1 et avance méthodiquement.
