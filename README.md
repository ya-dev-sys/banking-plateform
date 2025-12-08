# Banking API Gateway Microservices Platform

Plateforme microservices bancaire sécurisée avec authentification JWT, rate limiting distribué et observabilité complète.

## 🚀 Démarrage Rapide

```bash
# Clone le projet (si ce n'est pas déjà fait)
git clone <votre-repo>
cd banking-plateform

# Copier les variables d'environnement
cp .env.example .env

# Démarre tout avec Docker
docker compose up -d
```

## 🏗️ Architecture

Voir [banking-gateway-doc.md](./banking-gateway-doc.md) pour la documentation complète.

## 📦 Services

| Service         | Port | Description         |
| --------------- | ---- | ------------------- |
| API Gateway     | 8080 | Point d'entrée      |
| Auth Service    | 8081 | Authentification    |
| Account Service | 8082 | Comptes & Virements |
| User Service    | 8083 | Profils             |
| Eureka          | 8761 | Discovery           |
