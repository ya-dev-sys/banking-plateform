# Conventions de Code & Bonnes Pratiques Backend (Spring Boot)

Ce document définit les standards de qualité pour le développement Backend Java/Spring Boot. L'objectif est de garantir un code uniforme, robuste et maintenable.

## 1. 📏 Principes Généraux

- **Stateless** : L'API doit être sans état. Aucune session utilisateur en mémoire serveur (utilisez Redis si besoin de cache partagé).
- **Fail Fast** : Validez les entrées le plus tôt possible (dans le Controller via DTO).
- **Immutability** : Privilégiez les objets immuables (Records, champs `final`) pour éviter les effets de bord.
- **Dependency Injection** : Toujours utiliser l'injection par constructeur (via `@RequiredArgsConstructor`). Jamais d'injection par champ (`@Autowired` sur field interdit).
- **Logging** : Utilisez SLF4J avec Logback. En PROD, privilégiez le **Structured Logging** (JSON) pour faciliter l'indexation (ELK/Datadog).
- **Soft Delete** : Ne supprimez jamais physiquement les données métier (sauf RGPD). Utilisez un champ `deleted_at` ou `is_active` pour conserver l'historique.

## 2. 📝 Naming Conventions (Nommage)

| Élément        | Convention        | Exemple                                     |
| :------------- | :---------------- | :------------------------------------------ |
| **Packages**   | lowercase         | `com.xxxxxxxxx.feature.auth`                |
| **Classes**    | PascalCase        | `ProductService`, `UserController`          |
| **Interfaces** | PascalCase        | `ProductRepository`, `PaymentGateway`       |
| **Méthodes**   | camelCase         | `findActiveProducts()`, `calculateTotal()`  |
| **Variables**  | camelCase         | `userList`, `isValid`                       |
| **Constantes** | UPPER_SNAKE_CASE  | `MAX_RETRY_ATTEMPTS`, `DEFAULT_PAGE_SIZE`   |
| **DTOs**       | Suffixe explicite | `CreateUserRequest`, `UserResponse`         |
| **Tests**      | Suffixe Test      | `ProductServiceTest`, `AuthIntegrationTest` |

## 3. ☕ Java & Spring Best Practices

### Injection de Dépendances

Utilisez **Lombok** pour générer le constructeur. C'est plus propre et facilite les tests unitaires.

```java
// ❌ Bad
@Service
public class UserService {
    @Autowired
    private UserRepository repo; // Field injection (Difficile à mocker)
}

// ✅ Good
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repo; // Constructor injection (Clean)
}
```

### Gestion des Exceptions

Ne jamais avaler une exception avec un `catch` vide. Utilisez un **Global Exception Handler**.

```java
// ✅ Good (@ControllerAdvice)
@ExceptionHandler(EntityNotFoundException.class)
public ProblemDetail handleNotFound(EntityNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
}
```

### Logging

Utilisez l'annotation `@Slf4j`. Logguez les erreurs avec la stacktrace, et les infos métier pertinentes.

```java
log.error("Payment failed for order {}", orderId, exception);
log.info("User {} logged in successfully", userId);
```

## 4. 🏗️ Structure & DTOs

- **Jamais d'Entités dans le Controller** : N'exposez jamais vos entités JPA (`@Entity`) directement dans l'API. Cela crée un couplage fort et des problèmes de sécurité (ex: password hash exposé).
- **Utilisez des Records** : Pour les DTOs, utilisez les `record` Java (Java 14+) qui sont immuables et concis.

```java
// ✅ Good
public record UserResponse(Long id, String email, String fullName) {}
```

## 5. 🚦 Git Workflow & Commits

Même convention que le Front (Conventional Commits).

- `feat: ...` : Nouvelle fonctionnalité API.
- `fix: ...` : Correction de bug.
- `chore: ...` : Mise à jour de dépendances (pom.xml), config Docker.
- `test: ...` : Ajout de tests JUnit.

## 6. 🧪 Testing Strategy

- **Unit Tests** : Testez la logique métier (Service) en isolant les dépendances avec **Mockito**.
  - _Règle_ : Rapides (< 100ms), ne chargent pas le contexte Spring.
- **Integration Tests** : Testez les Controllers et Repositories avec **@SpringBootTest** et **Testcontainers**.
  - _Règle_ : Doivent utiliser une vraie BDD (Docker), pas H2 (pour éviter les différences SQL).
- **Assertions** : Utilisez **AssertJ** pour des assertions lisibles.

```java
assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID);
```

## 7. 🧹 Code Quality

- **Checkstyle / Spotless** : Le code doit respecter le formatage Google Java Style.
- **SonarQube** : Pas de "Code Smells" critiques.
- **Commentaires** : Le code doit s'expliquer de lui-même. Commentez le "Pourquoi", pas le "Comment". Javadoc obligatoire sur les interfaces publiques des Services.
- **Code Smells** : Pas de "Code Smells" critiques.

---

_Ce document sert de référence pour toute Code Review._
