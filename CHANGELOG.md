# Changelog

Tous les changements notables de ce projet sont documentés dans ce fichier.

Le format est basé sur Keep a Changelog,
et ce projet respecte la Versioning Sémantique.

---

## [2.4.0] - Gouvernance OpenSpec et compatibilité import/export

### Added

- Gouvernance en deux phases : transition code-first puis OpenSpec canonique.
- Checklist PR imposant la mise à jour de README et CHANGELOG lors des changements de comportement.
- Workflow CI `Documentation Guard` pour bloquer les PR de comportement sans mise à jour docs.
- Workflow CI `Import Export Compatibility Gate` avec vérification des fixtures et exécution des tests de compatibilité.
- Fixtures de compatibilité import/export versionnées (N et N-1) pour les tests.
- Tests unitaires et d'intégration dédiés à la compatibilité import/export N/N-1.
- Documentation technique dédiée : gouvernance de vérité produit et décisions techniques.

### Changed

- Sécurité export : validation de clé API déplacée vers un filtre dédié dans la chaîne Spring Security (`ApiKeyAuthFilter`).
- Règle d'appartenance collection normalisée : une carte est possédée si `quantity > 0` ou `foilQuantity > 0`.
- Invariant foil normalisé : `foil == (foilQuantity > 0)` sur les flux lecture/écriture.
- Réparation automatique au démarrage des incohérences historiques de collection (null/flag foil incohérent/entrées non possédées).
- Statistiques : comptage des cartes possédées aligné sur la règle distincte possédée (normal ou foil).
- Scanner OCR : borne haute `TOTAL` configurable via `scanner_total_max` avec fallback documenté.
- Import Companion : reporting structuré des résultats (`details`: mode, importées, ignorées, causes).

### Fixed

- Nettoyage du changelog : suppression des sections dupliquées pour ne garder qu'une seule entrée par version.

---

## [2.3.2] - Correction scanner cartes enchantees

### Fixed

- Scanner OCR : les cartes dont le numero depasse le total imprime (cartes enchantees, ex. `205/204`) n'etaient pas acceptees. Cette contrainte est supprimee.
- Borne maximale du champ `total` relevee de 400 a 500.

---

## [2.3.1] - Correction sauvegarde/restauration Foil

### Fixed

- `foilQuantity` est maintenant inclus dans la sauvegarde complete.
- `foilQuantity` est relu et restaure dans `UserCollection`.
- Ajout de tests de regression dans `BackupRestoreIntegrationTest`.

---

## [2.3.0] - Cles API et export programmable

### Added

- Gestion des cles API en administration (creation, liste, suppression, expiration, dernier usage).
- Endpoint `GET /api/export?apiKey=<cle>` pour export sans JWT.
- Tests unitaires `ApiKeyServiceTest` et integration `ApiKeyExportIntegrationTest`.

---

## [2.2.0] - Derniers scans et filtre Foil

### Added

- Onglet `Recents` avec limites 10/20/25/50.
- Filtre `Foil` sur la collection.

### Changed

- Endpoint `GET /api/collection/recent?limit=...`.
- Chargement JPA optimise via `JOIN FETCH`.

---

## [2.1.0] - Suivi dual Regular/Foil

### Added

- Distinction des quantites `quantity` (normal) et `foilQuantity` (foil).
- Import Companion separant `Regular` et `Foiled`.

### Changed

- API collection et DTO etendus pour `foilQuantity`.
- UI adaptee avec actions normal/foil separees.

---

## [2.0.0] - Cartes foil et dates d'ajout

### Added

- Support du statut foil par entree de collection.
- Dates `firstAddedAt` et `lastAddedAt`.

### Changed

- Sauvegarde/restauration complete incluant les metadonnees foil et dates.

---

## [1.3.1] - Migration Spring Boot 4

### Changed

- Migration vers Spring Boot 4.
- Ajustements Java 25 (Mockito/byte-buddy, imports de tests).

