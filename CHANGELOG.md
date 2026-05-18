# Changelog

Tous les changements notables de ce projet sont documentés dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/),
et ce projet respecte la [Versioning Sémantique](https://semver.org/lang/fr/).


---

## [2.3.2] — Correction scanner cartes enchantées

### Fixed

- **Scanner OCR** : les cartes dont le numéro dépasse le total imprimé (cartes enchantées, ex. `205/204`) étaient rejetées silencieusement par la validation `cardNum > total`. Cette contrainte est supprimée.
- Borne maximale du champ `total` relevée de 400 à 500 pour anticiper les grands sets futurs.

---

## [2.3.1] — Correction sauvegarde/restauration Foil

### Fixed

- **Sauvegarde complète** : le champ `foilQuantity` était absent du JSON exporté — les cartes foil apparaissaient comme normales après restauration. Il est désormais inclus dans chaque entrée de collection (`"foilQuantity": N`).
- **Restauration complète** : `foilQuantity` est maintenant relu et réappliqué sur l'entité `UserCollection` lors de la restauration.
- **Tests** : deux nouveaux cas dans `BackupRestoreIntegrationTest` couvrent explicitement la persistance de `foilQuantity` à l'export et à la restauration.

---

## [2.3.0] — Clés API & Export programmable

### Added

- **Clés API** : génération de clés API depuis la page Administration pour accéder à un endpoint d'export sans JWT.
  - Chaque clé dispose d'un **nom** descriptif, d'une **durée de validité** choisie (7 j / 30 j / 90 j / 180 j / 1 an / 10 ans) et d'une **date d'expiration**.
  - La clé en clair est affichée **une seule fois** à la création, avec un bouton **Copier** ; seul son hash SHA-256 est persisté.
  - **Dernière utilisation** : `lastUsedAt` est mis à jour à chaque appel réussi.
- **Endpoint `GET /api/export?apiKey=<clé>`** : retourne le même payload JSON que la sauvegarde complète (éditions + cartes + collection + paramètres), accessible sans authentification JWT — par clé API uniquement.
- **Section "Clés API" dans l'Administration** : panneau dépliable (accordéon `<details>`) avec :
  - Formulaire de création (nom + durée).
  - Tableau des clés existantes : nom, préfixe (8 premiers caractères), date d'expiration, dernière utilisation.
  - **Ligne rouge** pour les clés expirées.
  - Bouton **Supprimer** par clé.
- **Modèle `ApiKey`** : entité JPA (`id`, `name`, `keyHash`, `keyPrefix`, `expiresAt`, `lastUsedAt`, `createdAt`).
- **`ApiKeyRepository`** : `findByKeyHash(String)`.
- **`ApiKeyService`** : `generateKey`, `validateAndTouch`, `listKeys`, `deleteKey`, `sha256` (package-visible pour les tests).
- **`ApiKeyAuthFilter`** : `OncePerRequestFilter` branché avant `JwtAuthenticationFilter` — intercepte `/api/export`, valide la clé et positionne l'authentification dans le `SecurityContext`.
- **`ApiKeyController`** : `GET /api/admin/apikeys`, `POST /api/admin/apikeys`, `DELETE /api/admin/apikeys/{id}` (JWT requis).
- **`ExportController`** : `GET /api/export` (authentifié via `ApiKeyAuthFilter`).
- **Tests unitaires `ApiKeyServiceTest`** : génération, validation (valide/expirée/inconnue/null), `lastUsedAt`, listing, suppression, SHA-256 déterministe.
- **Tests d'intégration `ApiKeyExportIntegrationTest`** : 200 avec clé valide, 403 sans clé / clé incorrecte / clé expirée, mise à jour de `lastUsedAt`.

### Changed

- `SecurityConfig` : injection de `ApiKeyAuthFilter` et ajout avant `UsernamePasswordAuthenticationFilter`.



## [2.2.0] — Onglet Derniers scans & filtre Foil

### Added

- **Onglet "Derniers scans"** : nouvel onglet dédié dans la barre de navigation (icône ↻) affichant les N dernières cartes ajoutées à la collection, triées par date d'ajout décroissante.
  - Même affichage que la collection : image, badge ✦ Foil, compteur de quantité.
  - Date et heure de scan affichées sous chaque carte.
  - Sélecteur de limite : **10 / 20 / 25 / 50** cartes (chips cliquables, valeur mémorisée pendant la session). Valeur par défaut : 20.
  - Clic sur une carte → ouvre la modale de détail habituelle (modification de quantités incluse).
- **Filtre "✦ Foil"** dans la barre de filtres de la collection : n'affiche que les cartes possédant au moins un exemplaire foilé (`foilQuantity > 0`). Combinable avec la recherche par nom et le filtre par édition.

### Changed

- `UserCollectionRepository` : ajout de la méthode `findRecentWithCard(Pageable)` avec `JOIN FETCH` sur `card` et `edition` — remplace l'ancienne `findTop15ByOrderByLastAddedAtDesc()` qui provoquait une `LazyInitializationException`.
- `CollectionService` : méthode `getRecentCards(int limit)` avec validation de la valeur autorisée parmi `{10, 20, 25, 50}`.
- `CollectionController` : endpoint `GET /api/collection/recent?limit=20` avec `@RequestParam(defaultValue = "20")`.
- `app.js` : route `#/recent` → `renderRecentScansPage()` ; état `recentLimit` persisté en session ; cache `recentCardsState` mis à jour après chaque scan ajouté.

### Fixed

- `LazyInitializationException` sur `GET /api/collection/recent` : la requête JPQL utilise désormais `JOIN FETCH uc.card c LEFT JOIN FETCH c.edition` pour charger toutes les associations en une seule requête SQL.

---

## [2.2.0] — Amélioration du système Foil

### Added

- **Suivi dual des quantités Foil** : La collection distingue désormais `quantity` (exemplaires réguliers) et `foilQuantity` (exemplaires foil).
- **Import Companion amélioré** : Le champ `Type` du fichier Companion est maintenant utilisé pour séparer les cartes Regular et Foiled dans les quantités.
- **API enhancée** : Les endpoints `/api/collection` acceptent désormais un paramètre `foilQuantity` distinct de `quantity`.
- **Interface utilisateur rénovée** : Deux boutons distincts pour ajouter des cartes :
  - "◇ Ajouter exemplaire normal" pour ajouter une carte régulière
  - "✦ Ajouter exemplaire foil" pour ajouter une carte foilée
- **Affichage amélioré** : Les cartes affichent le total combiné (regular + foil) avec un badge doré ✦ si des exemplaires foil sont présents.

### Changed

- **Modèle `UserCollection`** : Ajout du champ `Integer foilQuantity = 0` pour stocker indépendamment les quantités foil.
- **DTO `CardDTO`** : Ajout du champ `foilQuantity` en réponse API.
- **Service `LorcaJsonService`** : Refactorisation de `doCompanionImport()` pour séparer les quantités selon le champ `Type` (case-insensitive).
- **Service `CollectionService`** : Mise à jour des signatures de méthode :
  - `addCard(Long cardId, int quantity, int foilQuantity, boolean foil)`
  - `updateQuantity(Long cardId, int quantity, int foilQuantity, Boolean foil)`
- **Contrôleur `CollectionController`** : Endpoints `POST` et `PUT` acceptent `foilQuantity` dans le body.
- **Frontend `app.js`** : 
  - La modale d'ajout remplace le checkbox par deux boutons indépendants
  - Nouvelle signature `addCard(cardId, quantity = 1, foilQuantity = 0)`
  - Fonctions séparées `updateQtyRegular()` et `updateQtyFoiled()` pour gérer les contrôles indépendants

### Fixed

- **Interface utilisateur cohérente** : Les résultats de recherche utilisent désormais le même système de deux boutons (Normal/Foil) que la modale, remplaçant l'ancien système checkbox + bouton unique.
- **Compatibilité Java 25** : Mise à jour de Lombok v1.18.44 et configuration des `<annotationProcessorPaths>` dans Maven pour résoudre le problème `TypeTag :: UNKNOWN`.
- **Tests unitaires** : Mise à jour de tous les appels de méthode `CollectionServiceTest` avec les nouvelles signatures incluant `foilQuantity`.

### Technical Details

| Aspect | Détail |
|--------|--------|
| Séparation import | Deux maps distincts dans `doCompanionImport()` : `regularQtiesByExternalId` et `foilQtiesByExternalId` |
| Détection Type | Utilise `"Foiled".equalsIgnoreCase(type)` pour brancher la logique |
| Suppression | Une entrée `UserCollection` n'est supprimée que si les deux `quantity` et `foilQuantity` sont ≤ 0 |
| Compatibilité | Utilise l'opérateur Elvis (`?:`) en JavaScript pour éviter les valeurs null |

**Build Status** : ✅ `mvn clean compile` passe sans erreur, ✅ tous les tests unitaires réussissent


---

## [1.3.1] — 2026-05-18 — Migration Spring Boot 4

### Changed

- **Spring Boot** : migration de 3.x vers **4.0.6**.
  - `spring-boot-starter-webflux` remplacé par `spring-boot-starter-webclient` (nouveau starter dédié en Spring Boot 4 pour l'auto-configuration du bean `WebClient.Builder`).
  - Import `TestEntityManager` mis à jour : `org.springframework.boot.data.jpa.test.autoconfigure` → `org.springframework.boot.jpa.test.autoconfigure` (réorganisation des modules Spring Boot 4).
  - Maven Surefire configuré avec `-javaagent: byte-buddy-agent` pour permettre à Mockito de fonctionner sous Java 25 (l'attachement dynamique d'agent est restreint depuis Java 21+).

### Fixed

- `MockitoInitializationException` : `byte-buddy-agent` chargé explicitement via `<argLine>` dans `maven-surefire-plugin` — résout l'erreur `net.bytebuddy.agent.Installer` introuvable sur Java 25.
- `ApplicationContext` failure dans les tests d'intégration : absence du bean `WebClient$Builder` corrigée par l'ajout du starter `spring-boot-starter-webclient`.
- Erreur de compilation `cannot find symbol TestEntityManager` dans `UserCollectionAuditTest` : import corrigé vers le nouveau package `org.springframework.boot.jpa.test.autoconfigure`.

---

## [2026-04-01] — Cartes foil, dates d'ajout

### Added

#### Nouvelles fonctionnalités

| Fonctionnalité | Description |
|---|---|
| **Version foil** | Chaque carte de la collection peut être marquée comme foil. |
| **Badge Foil** | Les cartes foil affichent un badge doré ✦ dans la grille de la collection et une bordure mise en valeur. |
| **Toggle foil dans le détail** | En ouvrant une carte possédée, un bouton **◇ Normal / ✦ Foil** permet de basculer la version d'un seul tap. |
| **Choix foil à l'ajout** | Lors de l'ajout d'une carte (modale manuelle, scanner), une case à cocher "Foil" est proposée avant confirmation. |
| **Date de premier ajout** | La date à laquelle une carte a été ajoutée pour la première fois en collection est mémorisée (`firstAddedAt`). |
| **Date de dernière modification** | La date de la dernière mise à jour de la quantité ou du statut foil est mémorisée (`lastAddedAt`). |
| **Affichage des dates** | Les deux dates sont visibles dans la vue de détail d'une carte possédée. |

### Changed

#### Modifications techniques

- `UserCollection` : champs `foil` (boolean, défaut `false`), `firstAddedAt`, `lastAddedAt` (ex-`addedAt`). `@PrePersist` utilise des **null-checks** pour ne pas écraser les dates pré-initialisées (scénario de restauration). `@PreUpdate` maintient `lastAddedAt` automatiquement.
- `CardDTO` : champs `foil`, `firstAddedAt`, `lastAddedAt` exposés dans tous les endpoints collection.
- `POST /api/collection` : accepte `foil` dans le body.
- `PUT /api/collection/{cardId}` : accepte `foil` en option dans le body (`null` = conserve la valeur existante).
- **Sauvegarde complète** : `foil`, `firstAddedAt`, `lastAddedAt` inclus dans le JSON exporté.
- **Restauration complète** : `foil`, `firstAddedAt` et `lastAddedAt` lus depuis le backup et injectés avant persist, préservant ainsi les dates d'origine. Si les champs sont absents (backup ancien format), `@PrePersist` les remplit avec `now()`.
- **Import Companion** : le format Companion additionne Regular + Foil en une seule quantité sans distinguer la version ; le champ `foil` reste à `false` pour les nouvelles entrées et conservé pour les entrées existantes en mode fusion.

### Fixed

| Problème | Correction |
|---|---|
| `@PrePersist` écrasait `firstAddedAt`/`lastAddedAt` sur une restauration | `@PrePersist` utilise désormais `if (field == null)` avant d'attribuer `now()` |
| La restauration ne lisait pas `firstAddedAt`/`lastAddedAt` depuis le backup | `fullRestore()` parse et injecte ces deux champs via `LocalDateTime.parse()` si présents |

### Tests

| Classe | Type | Couverture |
|---|---|---|
| `model/UserCollectionAuditTest` | `@DataJpaTest` (H2) | `@PrePersist` initialise les dates ; null-check préserve les dates preset ; `@PreUpdate` met à jour `lastAddedAt` sans toucher `firstAddedAt` ; valeurs foil persistées et modifiables |
| `service/CollectionServiceTest` | Unitaire Mockito | `addCard` et `updateQuantity` — foil transmis, incrémentation de quantité, foil null = valeur inchangée, quantité ≤ 0 = suppression |
| `controller/BackupRestoreIntegrationTest` | `@SpringBootTest` (H2) + MockMvc | Backup exporte `foil`/`firstAddedAt`/`lastAddedAt` ; restore les relit ; backup ancien format (sans dates) → fallback `now()` ; endpoints `POST /api/collection` et `PUT /api/collection/{id}` — foil stocké et `firstAddedAt` préservé au toggle |

---

## [Scanner] — Historique du système de scanner

### Changelog scanner

#### Version actuelle (OCR caméra continu)

- Scan **100 % caméra** (mode image supprimé).
- OCR du code imprimé en bas-gauche : `N/TOTAL • FR • SET`.
- Boucle de scan continue avec arrêt automatique dès qu'une carte est reconnue.
- Bip + vibration à la détection.
- Vue résultat focalisée (caméra masquée) avec quantité en collection.
- Action **Ajouter un exemplaire** puis retour automatique caméra + relance du scan continu.
- Action **Recommencer** pour relancer immédiatement une nouvelle détection.

#### Historique récent

- Migration Tesseract.js v4 → v5 pour corriger les erreurs WASM (`SetImageFile, e is null`).
- Passage d'un scan ponctuel manuel à un flux continu optimisé mobile.
