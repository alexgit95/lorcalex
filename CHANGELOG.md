# Changelog

Tous les changements notables de ce projet sont documentés dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/),
et ce projet respecte la [Versioning Sémantique](https://semver.org/lang/fr/).

---

## [Unreleased] — Amélioration du système Foil

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
