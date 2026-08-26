# Changelog

Tous les changements notables de ce projet sont documentés dans ce fichier.

Le format est basé sur Keep a Changelog,
et ce projet respecte la Versioning Sémantique.

---

## [2.7.0] - Sync pricing paginee par sets et limites API strictes

### Added

- Snapshot historique de la valeur totale de la collection calculé après chaque synchronisation pricing réussie.
- Graphique d'évolution de la valeur globale et tableau de tendance par édition avec changements sur 7 jours et 30 jours.
- Endpoints `GET /api/pricing/trend` et `GET /api/pricing/edition-deltas` pour consultater les snapshots historiques.

### Changed

- La vue Prix expose désormais la trajectoire historique globale et les deltas par édition en plus de la valorisation actuelle.

---

## [2.7.0] - Sync pricing paginee par sets et limites API strictes

### Added

- Synchronisation pricing provider en pagination par sets (`/episodes`) puis pagination cartes par set (`/episodes/{id}/cards?page=n&per_page=100`).
- Curseur persistant de reprise (`phase`, `episodePage`, `episodeId`, `episodeCardsPage`) pour continuer un run partiel sur les runs suivants.
- Nouvelles cles de parametrage admin pour hard cap journalier, marge de securite, limite minute, endpoints provider pagines et telemetrie d'arret.

### Changed

- Gouvernance des appels provider en double garde stricte: jamais plus de 100 appels/jour (hard cap) et jamais plus de 30 appels/minute.
- Budget operationnel derive du hard cap via `effectiveDailyBudget = dailyHardLimit - dailySafetyMargin`.
- Chaque requete sortante provider est comptee (y compris reponses non-2xx et erreurs), afin de respecter le plafond quotidien strict.
- Priorisation de mise a jour pricing revisee: cartes sans prix d'abord, puis cartes dont le prix date de plus de 7 jours, puis le reste.
- Payload de statut pricing enrichi avec hard cap, marge, budget effectif, limite minute, curseur courant et derniere raison d'arret.

---

## [2.6.0] - Onglet Prix et valorisation EUR par édition suivie

### Added

- Nouvel endpoint JWT `GET /api/pricing/insights` pour alimenter la vue Prix.
- Nouvel onglet `Prix` avec:
	- 20 dernières cartes du catalogue valorisées (`lastPriceAt` décroissant)
	- valorisation collection par édition suivie
	- total global de valorisation en EUR
- Compteurs d'exclusion de valorisation: cartes sans prix et cartes non-EUR.

### Changed

- Règle de valorisation collection formalisée: `(quantity + foilQuantity) x marketPrice`.
- Périmètre des éditions valorisées aligné sur le filtre `stats_enabled_sets` du module Statistiques.
- Affichage monétaire normalisé en EUR pour la vue Prix.

---

## [2.5.0] - Synchronisation pricing avec quota journalier par tentative

### Added

- Moteur de synchronisation pricing avec exécution planifiée quotidienne et déclenchement manuel admin.
- Endpoint de statut pricing (`/api/admin/pricing/status`) avec budget, consommation et files de traitement.
- Client provider RapidAPI avec mapping déterministe basé sur les identifiants carte (nom, numéro, set, externalId).
- Compteurs persistés de quota journalier (`pricing_usage_date`, `pricing_used_attempts`) pour garantir la sécurité après redémarrage.
- Tests unitaires et d'intégration sur quota, priorisation, rollover et persistance de consommation.

### Changed

- Stratégie de refresh pricing : cartes sans valorisation d'abord, puis cartes les plus anciennes (`lastPriceAt` ascendant).
- Politique de comptage: chaque tentative d'appel provider consomme une unité de quota, y compris en erreur (ex: HTTP 429).
- Contrat import/export et sauvegarde étendu avec les champs pricing (`marketPrice`, `priceCurrency`, `priceSource`, `lastPriceAt`, `lastPriceStatus`) pour préserver la stratégie de refresh après restauration.
- Planification pricing rendue dynamique via `pricing_schedule_cron` avec reconfiguration à chaud après mise à jour admin.
- Ajout d'un rattrapage startup quotidien unique basé sur `pricing_last_scheduled_run_date` pour couvrir les runs manqués quand l'application était arrêtée.

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

