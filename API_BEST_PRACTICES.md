# Bonnes Pratiques API & Standards Modernes (Spring Boot 3)

Ce document complète les conventions de code en se focalisant spécifiquement sur le **Design d'API** et l'utilisation des fonctionnalités modernes de Spring Boot 3 / Java 17+.
Il proscrit les pratiques obsolètes (Legacy).

## 1. 🌐 Design RESTful (Maturité Niveau 2/3)

### Ressources & Verbs

- **Noms** : Utilisez des noms au pluriel pour les ressources (`/products`, pas `/product`).
- **Verbes HTTP** : Respectez la sémantique stricte.
  - `GET` : Lecture (Idempotent, Cacheable).
  - `POST` : Création (Non-idempotent). Retourne `201 Created` + Header `Location`.
  - `PUT` : Remplacement complet (Idempotent).
  - `PATCH` : Modification partielle (Idempotent).
  - `DELETE` : Suppression (Idempotent).

### Codes de Statut (Précision)

Ne retournez pas juste `200` ou `500`. Soyez précis.

- `201 Created` : Après un POST réussi.
- `204 No Content` : Après un DELETE réussi ou un PUT sans retour.
- `400 Bad Request` : Erreur de validation (ex: champ manquant).
- `401 Unauthorized` : Token manquant ou invalide.
- `403 Forbidden` : Token valide mais droits insuffisants (ex: Client veut accéder à Admin).
- `404 Not Found` : Ressource inexistante.
- `409 Conflict` : Doublon (ex: Email déjà pris).
- `422 Unprocessable Entity` : Erreur métier (ex: Stock insuffisant).
- `429 Too Many Requests` : Rate limit dépassé.

## 2. 🚀 Fonctionnalités Modernes (Spring Boot 3+)

### Utilisation de `ProblemDetail` (RFC 7807)

**STOP** aux objets d'erreur maison (`ErrorResponse`).
Utilisez le standard `ProblemDetail` intégré à Spring 6.

```java
// ✅ Modern Way
@ExceptionHandler(ProductNotFoundException.class)
public ProblemDetail handleNotFound(ProductNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Product Not Found");
    problem.setProperty("productId", ex.getId()); // Extensions standardisées
    return problem;
}
```

### Records Java (DTOs)

**STOP** aux classes DTO avec `@Data` de Lombok (sauf cas complexes).
Utilisez les `record` Java (natifs, immuables, performants).

```java
// ✅ Modern Way
public record CreateOrderRequest(
    @NotNull Long productId,
    @Min(1) int quantity
) {}
```

### RestClient (vs RestTemplate)

**DEPRECATED** : `RestTemplate` est en mode maintenance.
**MODERN** : Utilisez `RestClient` (API fluide/fonctionnelle) ou `WebClient`.

```java
// ✅ Modern Way
var response = restClient.get()
    .uri("https://api.stripe.com/v1/charges")
    .retrieve()
    .body(StripeResponse.class);
```

### Observability (Micrometer Tracing)

**STOP** à Sleuth (Déprécié dans Boot 3).
Utilisez **Micrometer Tracing** pour le traçage distribué (Logs corrélés avec TraceID/SpanID).

## 3. 📄 Pagination & Filtrage

Ne réinventez pas la roue. Utilisez les abstractions Spring Data.

- **Request** : Acceptez `Pageable` dans les contrôleurs.
  - `GET /products?page=0&size=20&sort=price,desc`
- **Response** : Retournez `Page<T>` ou une version simplifiée `PagedResponse<T>`.

```java
@GetMapping
public Page<ProductDTO> getAll(Pageable pageable) {
    return service.findAll(pageable);
}
```

## 4. 🔒 Sécurité (Stateless)

- **CSRF** : Désactivez-le pour une API REST pure (car pas de cookies de session).
- **Session** : `SessionCreationPolicy.STATELESS`.
- **CORS** : Configurez-le globalement, pas sur chaque contrôleur.

## 5. ⚡ Performance & Caching

- **ETags** : Utilisez `ShallowEtagHeaderFilter` pour économiser la bande passante (le client reçoit `304 Not Modified` si la donnée n'a pas changé).
- **Cache** : Utilisez `@Cacheable` sur les méthodes de lecture lourdes (ex: Catalogue, Catégories), avec un provider comme Redis ou Caffeine.

## 6. 🛡️ Résilience & Fiabilité (Enterprise Grade)

### Idempotency (Clés d'Idempotence)

Pour les opérations critiques comme le paiement (`POST /orders`), implémentez un mécanisme d'**Idempotency Key**.

- Le client envoie un header `Idempotency-Key: UUID`.
- Si le serveur reçoit deux fois la même clé (ex: retry réseau), il ne rejoue pas la commande mais renvoie la réponse mise en cache de la première tentative.

### Rate Limiting

Protégez l'API contre les abus avec **Bucket4j** ou **Redis Rate Limiter**.

- Retournez `429 Too Many Requests` avec les headers `X-RateLimit-Limit` et `X-RateLimit-Remaining`.

### Circuit Breaker

Utilisez **Resilience4j** pour les appels aux services externes (Stripe, SendGrid).

- Si le service externe est down, le circuit s'ouvre pour éviter d'attendre le timeout et saturer les threads.

## 7. 🌍 Internationalization & Monitoring

### i18n (Internationalisation)

L'API doit supporter le header `Accept-Language` pour retourner les messages d'erreur dans la langue du client.

- Utilisez `MessageSource` de Spring.

### Health Checks (Actuator)

Exposez les endpoints **Spring Boot Actuator** (sécurisés) pour le monitoring :

- `/actuator/health` : État du service (DB, Disk, Ping).
- `/actuator/metrics` : Métriques Prometheus (Requêtes/sec, Latence, JVM).
- `/actuator/info` : Version Git et Build info.

## 8. 🏗️ Versioning

- **URI Versioning** : Simple et explicite.
  - `/api/v1/products`
- Ne cassez jamais l'API existante. Si changement majeur, créez `/api/v2/...`.

---

_Ce document garantit que notre Backend est "Future-Proof" et aligné avec les standards 2024/2025._
