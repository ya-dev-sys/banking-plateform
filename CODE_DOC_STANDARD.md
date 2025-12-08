# Standards de Documentation du Code (Interne)

Ce document définit les normes de documentation du code source pour le projet. L'objectif est de garantir que le code soit compréhensible, maintenable et que la documentation technique puisse être générée automatiquement.

---

## 1. 🎯 Principes Généraux

1.  **Le "Pourquoi" avant le "Comment"** : Le code explique déjà _comment_ il fonctionne. Les commentaires doivent expliquer _pourquoi_ (contexte, choix techniques, contraintes).
2.  **Interface Publique** : Documentez systématiquement toutes les classes, méthodes et fonctions publiques.
3.  **À jour** : Une documentation obsolète est pire que pas de documentation. Mettez à jour les commentaires en même temps que le code.
4.  **Format Standard** : Utilisez le format de bloc de commentaires multilignes (`/** ... */`) compatible avec les générateurs de documentation (JSDoc, JavaDoc, etc.).

---

## 2. 📝 Format des Blocs de Commentaires

Chaque bloc de documentation doit suivre cette structure :

1.  **Description Courte** : Une phrase résumant l'action.
2.  **Description Détaillée** (Optionnel) : Contexte, avertissements, détails d'implémentation importants.
3.  **Tags** : Liste des paramètres, retours, exceptions, etc.

### Exemple (JavaScript/TypeScript - JSDoc)

```javascript
/**
 * Calcule le montant total d'une commande TTC.
 *
 * Cette fonction est la source de vérité pour le calcul final.
 * Elle applique les règles de TVA en vigueur et les réductions potentielles.
 *
 * @param {Array<Object>} items - Liste des articles (prix HT et quantité).
 * @param {string} [couponCode] - (Optionnel) Code promotionnel à appliquer.
 * @param {number} taxRate - Taux de TVA (ex: 0.20 pour 20%).
 * @returns {number} Le montant total TTC arrondi à 2 décimales.
 * @throws {Error} Si le taux de TVA est négatif.
 *
 * @example
 * const total = calculateTotal([{price: 10, qty: 2}], 'PROMO10', 0.20);
 */
function calculateTotal(items, couponCode, taxRate) { ... }
```

### Exemple (Java - JavaDoc)

```java
/**
 * Authentifie un utilisateur via son email et mot de passe.
 *
 * <p>Cette méthode vérifie le hash du mot de passe avec BCrypt.
 * En cas de succès, elle retourne un token JWT valide.</p>
 *
 * @param email L'email de l'utilisateur.
 * @param rawPassword Le mot de passe en clair.
 * @return Un objet AuthResponse contenant le token JWT.
 * @throws BadCredentialsException Si les identifiants sont invalides.
 * @see SecurityConfig#passwordEncoder
 */
public AuthResponse login(String email, String rawPassword) { ... }
```

---

## 3. 🏷️ Tags Standards (Référence)

Utilisez ces tags pour structurer vos commentaires.

| Tag                      | Description                                                       | Contexte           |
| :----------------------- | :---------------------------------------------------------------- | :----------------- |
| `@param`                 | Décrit un paramètre d'entrée (Nom + Type + Description).          | Fonction / Méthode |
| `@return` / `@returns`   | Décrit la valeur retournée.                                       | Fonction / Méthode |
| `@throws` / `@exception` | Liste les erreurs que la fonction peut lever explicitement.       | Fonction / Méthode |
| `@deprecated`            | Marque un élément comme obsolète. Indiquez par quoi le remplacer. | Tout               |
| `@see`                   | Référence vers une autre partie du code ou une doc externe.       | Tout               |
| `@author`                | Auteur original du module (utile pour les gros fichiers).         | Classe / Fichier   |
| `@since`                 | Version du logiciel où l'élément a été introduit.                 | Classe / Méthode   |
| `@example`               | Fournit un exemple d'utilisation concret (très recommandé).       | Fonction / Méthode |

---

## 4. 💡 Bonnes Pratiques par Langage

### JavaScript / TypeScript (JSDoc)

- Utilisez `{Type}` pour spécifier les types si vous n'utilisez pas TypeScript.
- Avec TypeScript, les types sont souvent redondants dans `@param`, concentrez-vous sur la description.
- Utilisez `@typedef` pour définir des structures d'objets complexes réutilisées.

### Java (JavaDoc)

- Utilisez les balises HTML (`<p>`, `<ul>`, `<code>`) pour formater la description longue.
- Liez les classes avec `{@link Classe}` pour une navigation facile dans l'IDE.

---

## 5. 🚫 Ce qu'il ne faut PAS faire

- **Commentaires évidents** :

  ```javascript
  // ❌ Mauvais
  /**
   * Définit le nom.
   * @param name Le nom.
   */
  setName(name) { ... }
  ```

  _Si le nom de la fonction est explicite et qu'il n'y a pas de logique complexe, le commentaire est du bruit._

- **Commenter le code commenté** : Ne laissez pas de code mort commenté dans les fichiers. Supprimez-le (Git est là pour l'historique).

---
