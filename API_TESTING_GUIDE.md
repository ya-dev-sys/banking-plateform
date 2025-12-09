# Guide de Test via API Gateway

Ce document explique comment utiliser l'**API Gateway** (Port 8080) pour accéder aux endpoints des microservices (ex: Auth Service).

## 1. Prérequis

Assurez-vous que l'infrastructure tourne :

- **Docker** : Postgres, Redis, Kafka, Zipkin (`docker compose up -d`)
- **Discovery** : Eureka Server (Port 8761)
- **Config** : Config Server (Port 8888)
- **Gateway** : API Gateway (Port 8080)
- **Service** : Auth Service (Port aléatoire ou défini, enregistré sur Eureka)

## 2. Structure des URLs

Toutes les requêtes passent par le Gateway sur le port **8080**.
Le Gateway redirige le trafic en fonction du préfixe de l'URL.

| Service          | Route Gateway                        | URL Cible (Interne)              |
| :--------------- | :----------------------------------- | :------------------------------- |
| **Auth Service** | `http://localhost:8080/api/auth/...` | `lb://AUTH-SERVICE/api/auth/...` |

---

## 3. Scénarios de Test (Exemples avec Auth Service)

### A. Inscription (Register)

Créez un nouvel utilisateur.

- **URL** : `POST http://localhost:8080/api/auth/register`
- **Body (JSON)** :
  ```json
  {
    "username": "yanis",
    "email": "yanis@example.com",
    "password": "password123",
    "confirmPassword": "password123"
  }
  ```
- **Réponse attendue** : `201 Created`

### B. Connexion (Login)

Récupérez un token JWT.

- **URL** : `POST http://localhost:8080/api/auth/login`
- **Body (JSON)** :
  ```json
  {
    "email": "yanis@example.com",
    "password": "password123"
  }
  ```
- **Réponse attendue** : `200 OK`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "expiresIn": 3600
  }
  ```

### C. Accès Protégé (Test de Route)

Tentez d'accéder à une ressource protégée (nécessite le Token).

- **URL** : `GET http://localhost:8080/api/auth/me` (ou tout autre endpoint sécurisé)
- **Headers** :
  - `Authorization`: `Bearer <VOTRE_TOKEN_JWT>`
- **Réponse attendue** :
  - `200 OK` : Si le token est valide.
  - `401 Unauthorized` : Si le token est manquant ou invalide (filtré par le Gateway ou le Service).

---

## 4. Tester avec cURL

Vous pouvez tester directement depuis votre terminal :

**Login :**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"yanis@example.com","password":"password123"}'
```

**Accès sécurisé (remplacez le token) :**

```bash
TOKEN="eyJhbGciOi..."
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

## 5. Dépannage

- **503 Service Unavailable** : Le service (Auth Service) n'est pas encore enregistré sur Eureka ou est éteint.
- **401 Unauthorized** : Votre token est expiré ou invalide.
- **429 Too Many Requests** : Vous avez dépassé la limite du Rate Limiter (configuré dans le Gateway).
