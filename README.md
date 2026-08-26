# Lorcalex — Gestionnaire de collection Lorcana (FR)

Application web mobile-first pour gérer votre collection de cartes Disney Lorcana **en français**.
Le frontend HTML/JS/CSS vanilla est **inclus dans le JAR Spring Boot** — un seul binaire à déployer, **aucun npm ni Node.js requis**.

---

## Table des matières

- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Développement local](#développement-local)
- [Déploiement Docker / Portainer](#déploiement-docker--portainer)
- [Import du catalogue](#import-du-catalogue-lorcajson)
- [Collection](#collection--tri-et-affichage)
- [Scanner OCR](#scanner-de-cartes-ocr-continu)
- [Synchronisation des prix](#synchronisation-des-prix-des-cartes)
- [Sauvegarde & Restauration](#sauvegarde--restauration-complètes)
- [Clés API & Export programmable](#clés-api--export-programmable)
- [Import Lorcana Companion](#import-depuis-lorcana-companion)
- [API REST](#api-rest)
- [Sécurité](#sécurité)

---

## Fonctionnalités

| Écran | Description |
|-------|-------------|
| **Collection** | Cartes triées par set et numéro croissants, filtrage possédées / manquantes / **✦ Foil**, recherche, ajout/retrait. |
| **Visualisation carte** | Clic sur une carte → grande image plein-écran, compteurs Regular et Foil, modification directe des quantités. |
| **Statistiques** | Graphiques Chart.js (donut, barres empilées) : progression globale, par set, par rareté. |
| **Scanner** | OCR caméra en continu : lecture du code bas-gauche (`N/TOTAL • FR • SET`), arrêt automatique à la détection, vue de confirmation, reprise rapide. |
| **Derniers scans** | Onglet dédié : N dernières cartes ajoutées avec date/heure. Sélecteur 10 / 20 / 25 / 50. |
| **Administration** | Sync LorcaJson (URL/fichier), sauvegarde/restauration complètes, import Lorcana Companion, gestion des clés API. |
| **Se souvenir de moi** | Option à la connexion pour 12 mois d'authentification sans reconnexion. |

---

## Architecture

```
lorcalex/
├── pom.xml
└── src/main/
    ├── java/com/alexgit95/
    │   ├── model/           # Entités JPA (Card, Edition, UserCollection, ApiKey…)
    │   ├── repository/      # Interfaces Spring Data
    │   ├── service/         # CardService, CollectionService, LorcaJsonService, ApiKeyService…
    │   ├── controller/      # RestControllers (Admin, Collection, Export…) + SpaController
    │   ├── security/        # JwtAuthenticationFilter, JwtTokenProvider, UserDetailsServiceImpl
    │   └── config/          # SecurityConfig
    └── resources/
        ├── application.properties              # Port 8181
        ├── application-local.properties        # SQLite (dev)
        ├── application-docker.properties       # PostgreSQL (prod)
        └── static/                             # Frontend SPA (servi par Spring Boot)
            ├── index.html
            ├── app.js                          # Router, pages, API, scanner OCR
            └── app.css
```

### Stack technique

| Composant | Technologie |
|-----------|-------------|
| Backend | Java 25 · Spring Boot 4 · Spring Security · Spring Data JPA |
| Base de données | SQLite (local) · PostgreSQL 16 (Docker) |
| Frontend | HTML / CSS / JavaScript vanilla (pas de framework, pas de npm) |
| OCR scanner | Tesseract.js v5 |
| Graphiques | Chart.js 4 |

### Bases de données

| Profil | BDD | Usage |
|--------|-----|-------|
| `local` (défaut) | SQLite `lorcalex.db` | Développement local, zéro configuration |
| `docker` | PostgreSQL 16 | Déploiement Docker / Portainer |

---

## Développement local

### Prérequis

- Java 17+, Maven 3.9+
- **Aucun Node.js / npm requis**

### Lancer le serveur

```bash
mvn spring-boot:run
# Application accessible sur http://localhost:8181
```

### Build du JAR autonome

```bash
mvn clean package -DskipTests
java -jar target/lorcalex-0.0.1-SNAPSHOT.jar
# → http://localhost:8181
```

---

## Déploiement Docker / Portainer

### Variables d'environnement

| Variable | Défaut | Description |
|----------|--------|-------------|
| `APP_USERNAME` | `admin` | Identifiant de connexion |
| `APP_PASSWORD` | `admin` | Mot de passe (**à changer en prod !**) |
| `JWT_SECRET` | *(valeur interne)* | Secret HMAC-SHA256 (**à changer en prod !**, min 32 caractères) |
| `DB_PASSWORD` | `lorcalex` | Mot de passe PostgreSQL |
| `APP_PORT` | `8181` | Port exposé sur l'hôte |

### docker-compose

```bash
# À la racine du projet
docker compose up --build
# → http://localhost:8181
```

### Portainer — Stack

1. Dans Portainer → **Stacks → Add stack**.
2. Coller le contenu de `docker-compose.yml`.
3. Dans l'onglet **Environment variables**, renseigner :

```
APP_USERNAME=admin
APP_PASSWORD=MonMotDePasseSecret
JWT_SECRET=une-chaine-aleatoire-de-32-caracteres-minimum
DB_PASSWORD=monPasswordPostgres
APP_PORT=8181
```

4. Cliquer **Deploy the stack**.

---

## Import du catalogue (LorcaJson)

> **⚠️ Aucune carte n'est pré-chargée au démarrage.** L'import se fait manuellement depuis la page **Administration**.

### Par URL (recommandé)

1. Ouvrir **Administration**.
2. L'URL par défaut `https://lorcanajson.org/files/current/fr/allCards.json` est pré-remplie.
3. Cliquer **🔄 Importer depuis l'URL**.

### Par fichier

1. Télécharger `allCards.json` depuis [lorcanajson.org](https://lorcanajson.org/files/current/fr/allCards.json).
2. Ouvrir **Administration → 📂 Import depuis un fichier**.
3. Sélectionner le fichier téléchargé.

> **Note :** Le calcul d'empreintes visuelles (`/api/admin/compute-hashes`) est conservé comme fonctionnalité de maintenance.

---

## Collection — Tri et affichage

- Cartes triées par **numéro de set croissant**, puis **numéro de carte croissant**.
- Chaque carte affiche son numéro de set (ex : `S1·#42`).
- Le sélecteur de set affiche : **Set 1 — Premier Chapitre**, **Set 2 — L'Ascension des Floodborn**, etc.
- Filtres disponibles : **Toutes**, **Possédées**, **Manquantes**, **✦ Foil**.

### Visualisation de carte

Cliquer sur une carte ouvre un écran détail avec :

- Grande image plein-écran
- Set + numéro + nom + rareté
- Prix de marché et date de dernière mise à jour, lorsqu'ils sont disponibles
- Compteurs **Regular** et **Foil** modifiables indépendamment (boutons + / −)
- Dates de premier ajout et de dernière modification

### Se souvenir de moi

Lors de la connexion, cocher **"Se souvenir de moi (12 mois)"** pour un JWT valide 12 mois.
Sans cette option, la session expire après 24 heures.
Token stocké dans `localStorage` (avec) ou `sessionStorage` (sans).

---

## Scanner de cartes (OCR continu)

### Principe

1. Le scanner capture la carte dans le cadre caméra et extrait la zone de code bas-gauche.
2. Tesseract.js v5 lit le format imprimé : `N/TOTAL • FR • SET`.
3. L'application cherche la carte par son numéro et affine par numéro de set si disponible.
4. Le scan tourne en boucle et s'arrête automatiquement dès qu'une carte est reconnue.

> **Pré-requis :** le catalogue doit avoir été importé au moins une fois.

### Utilisation

1. Ouvrir **Scanner** — le scan continu démarre automatiquement.
2. Présenter la carte dans le cadre.
3. À la détection (bip + vibration) : choisir **◇ Ajouter normal**, **✦ Ajouter foil** ou **↻ Recommencer**.

### Fallback manuel

En cas d'échec OCR (mauvaise luminosité, reflet), utiliser la saisie manuelle du **numéro de carte** et du **set** en bas de l'écran Scanner.

---

## Sauvegarde & Restauration complètes

Accessible depuis **Administration → 💾 Sauvegarde & Restauration complètes**.

Exporte et restaure **l'intégralité de l'application** (catalogue + collection + paramètres) pour migrer vers un nouveau serveur sans re-synchronisation.

### Contenu de la sauvegarde

| Clé JSON | Contenu |
|----------|---------|
| `editions` | Tous les sets (code, nom, numéro, date de sortie, URL logo) |
| `cards` | Catalogue complet (tous les champs, dont `externalId` et `imageHash`) |
| `collection` | Quantités possédées par carte (`quantity`, `foilQuantity`, `foil`, dates) |
| `settings` | Paramètres applicatifs (dont `stats_enabled_sets`) |

### Format du fichier

```json
{
  "backupDate": "2026-03-27T10:00:00",
  "version": "2",
  "totalEditions": 10,
  "totalCards": 2048,
  "totalCollection": 350,
  "editions":   [ { "id": 1, "code": "TFC", "name": "Premier Chapitre", "setNumber": 1 } ],
  "cards":      [ { "externalId": "abc123", "cardNumber": 1, "editionCode": "TFC" } ],
  "collection": [ { "externalId": "abc123", "quantity": 2, "foilQuantity": 1, "foil": false } ],
  "settings":   [ { "key": "stats_enabled_sets", "value": "1,2,3" } ]
}
```

### Sauvegarde

Cliquer **⬇️ Télécharger la sauvegarde complète** → génère `lorcalex-backup-YYYY-MM-DD.json`.

### Restauration

1. Cliquer **🔄 Restaurer depuis une sauvegarde** et sélectionner le fichier.
2. **Confirmer** — l'opération est irréversible.
3. La restauration efface tout, recrée dans le bon ordre et remappe automatiquement les IDs d'éditions dans `stats_enabled_sets`.

> **Utilisation typique :** migration vers un nouveau serveur, récupération après réinitialisation de la base.

---

## Clés API & Export programmable

### Principe

`GET /api/export?apiKey=<clé>` retourne le même payload que la sauvegarde complète, **sans JWT** — idéal pour des outils externes (Home Assistant, scripts, etc.).

### Sécurité

- La clé en clair n'est **jamais stockée** : seul le hash SHA-256 est conservé en base.
- La valeur en clair est affichée **une seule fois** à la création — à copier immédiatement.
- Chaque clé a une **date d'expiration** configurable (7 j / 30 j / 90 j / 180 j / 1 an / 10 ans).
- La **dernière utilisation réussie** est enregistrée automatiquement.

### Créer une clé

1. Ouvrir **Administration → 🔑 Clés API** (section dépliable).
2. Saisir un **nom** (ex : "Home Assistant") et choisir une **durée de validité**.
3. Cliquer **Générer** — copier la clé affichée (bouton 📋).

### Utiliser la clé

```bash
curl "http://localhost:8181/api/export?apiKey=<votre_clé>"
```

### Gérer les clés

Le tableau affiche : nom, préfixe (8 premiers caractères), date d'expiration, dernière utilisation.
Les clés expirées apparaissent en **rouge**. Un bouton **🗑 Supprimer** est disponible par clé.

---

## Import depuis Lorcana Companion

Accessible depuis **Administration → Import depuis Lorcana Companion**.

### Format supporté

Export JSON de l'application Companion contenant la clé `OwnedCardQuantitiesV2`.
Les entrées `Regular` et `Foiled` d'une même carte sont distinguées et stockées séparément dans `quantity` / `foilQuantity`.

### Modes d'import

| Mode | Comportement |
|------|-------------|
| **Fusion** (défaut) | Ajoute les quantités importées aux quantités existantes |
| **Remplacement** | Remplace les quantités existantes par celles du fichier |

### Progression

L'import est asynchrone. La barre de progression dans l'Administration affiche les phases `📄 Analyse Companion` → `📥 Import Companion` → `✅ Terminé` / `❌ Erreur`.

En fin d'import, le statut expose aussi un bloc structuré `details` contenant au minimum :
- `importMode` (`fusion` ou `remplacement`)
- `imported`
- `skippedUnknown`
- `skippedInvalidRows`
- `skippedTotal`

> **Pré-requis :** le mapping Companion repose sur l'`externalId`. Si de nombreuses cartes sont introuvables, relancer d'abord une synchronisation LorcaJson.

---

## Scanner OCR - borne configurable

La validation du format OCR `N/TOTAL • FR • SET` utilise désormais une borne `TOTAL` configurable via le setting `scanner_total_max`.

---

## Synchronisation des prix des cartes

La valorisation des cartes est pilotée par une synchronisation de fond avec garde-fous stricts de quota.

Le moteur de synchronisation parcourt désormais des endpoints paginés provider :

- `GET /episodes?page=n`
- `GET /episodes/{id}/cards?page=n&per_page=100`

### Règles de quota

- Le quota journalier est compté sur les **tentatives d'appel sortant** (pas sur les succès).
- Une tentative consomme 1 unité, quel que soit le résultat provider (`2xx`, `4xx`, `5xx`, timeout, erreur réseau).
- La garde journalière applique un **hard cap** (`pricing_daily_hard_limit`, max 100) et une marge de sécurité (`pricing_daily_safety_margin`).
- Le budget effectif est `pricing_daily_hard_limit - pricing_daily_safety_margin`.
- La limite minute (`pricing_minute_limit`) est strictement bornée à 30 appels/minute maximum.
- Si `usedAttempts >= effectiveDailyBudget`, aucun appel supplémentaire n'est envoyé.
- Le compteur est persisté (`pricing_usage_date`, `pricing_used_attempts`) pour rester fiable après redémarrage.

### Stratégie de refresh

- Priorité 1 : cartes sans `marketPrice`.
- Priorité 2 : cartes avec prix mais `lastPriceAt` datant de plus de 7 jours.
- Priorité 3 : le reste des cartes.

Le mapping provider -> carte locale est déterministe : code d'édition + numéro de carte en priorité, puis fallback contrôlé sur `externalId`.

### Exécution

- Tâche planifiée quotidienne basée sur `pricing_schedule_cron` (modifiable en administration, sans redémarrage).
- Si le cron est invalide, fallback automatique sur `0 0 2 * * *` avec statut de validité exposé.
- Au démarrage, un rattrapage unique (`startup_catchup`) est déclenché si la journée courante n'a pas encore eu de run planifié.
- Déclenchement manuel disponible via `POST /api/admin/pricing/run`.
- Un curseur persistant (phase/page/episode) permet de reprendre la pagination après interruption ou redémarrage.
- Statut opérationnel via `GET /api/admin/pricing/status` (hard cap, marge, budget effectif, minute limiter, curseur, raison d'arrêt, files de traitement, état du schedule et dernier run planifié).

### Onglet Prix

Un onglet `Prix` expose une vue opérationnelle en lecture seule:

- Les 20 dernières cartes du catalogue ayant reçu un prix (`lastPriceAt` décroissant).
- La valorisation par édition suivie (même périmètre que `stats_enabled_sets`).
- Le total global de valorisation collection en EUR.
- Un graphique historique de la valeur totale de la collection calculé à chaque synchronisation pricing.
- Un tableau par édition affichant la valeur courante et les écarts sur 7 jours et 30 jours.

Règle de valorisation par carte:

- `(quantity + foilQuantity) x marketPrice`

Contraintes monétaires:

- Affichage et agrégats en EUR uniquement.
- Les cartes sans prix (`marketPrice` null) ou avec devise non-EUR sont exclues des agrégats.
- Le payload de l'onglet Prix expose des compteurs d'exclusion (`excludedNoPrice`, `excludedNonEur`).

### Historique de valeur et tendances

Après chaque synchronisation pricing réussie, le backend enregistre un snapshot global puis un snapshot par édition.

- `GET /api/pricing/trend` renvoie les points de valeur totale de la collection triés chronologiquement.
- `GET /api/pricing/edition-deltas` renvoie pour chaque édition la valeur courante, la valeur de référence à 7 jours et à 30 jours, ainsi que les variations en pourcentage.
- Les deltas sont calculés à partir des snapshots historiques les plus récents à ou avant les seuils temporels.

Le modèle de données historique est donc aligné sur la chronologie des synchronisations, ce qui permet de tracer la valeur globale sur le temps et de comparer l'évolution par set.

### Paramètres principaux

Les clés de configuration pricing sont stockées dans `app_settings` et modifiables via les endpoints admin settings :

- `pricing_sync_enabled`
- `pricing_daily_hard_limit`
- `pricing_daily_safety_margin`
- `pricing_minute_limit`
- `pricing_daily_budget`
- `pricing_usage_date`
- `pricing_used_attempts`
- `pricing_schedule_cron`
- `pricing_last_scheduled_run_date`
- `pricing_provider`
- `pricing_provider_host`
- `pricing_provider_episodes_path`
- `pricing_provider_episode_cards_path_template`
- `pricing_provider_api_key`
- `pricing_provider_currency`
- `pricing_cursor_phase`
- `pricing_cursor_episode_page`
- `pricing_cursor_episode_id`
- `pricing_cursor_episode_cards_page`
- `pricing_last_stop_reason`

### Import / Export et restauration

Les sauvegardes et exports conservent les champs de valorisation :

- `marketPrice`
- `priceCurrency`
- `priceSource`
- `lastPriceAt`
- `lastPriceStatus`

La conservation de `lastPriceAt` est importante pour maintenir la priorisation de fraîcheur (stale > 7 jours) après restauration.

---

## API REST

Les routes `/api/admin/**`, `/api/collection/**`, `/api/cards/**`, `/api/editions` et `/api/statistics` requièrent un **Bearer JWT**.
Les routes `/api/auth/login`, `/api/health` et `/api/export` sont publiques.

| Méthode | Route | Auth | Description |
|---------|-------|------|-------------|
| `POST` | `/api/auth/login` | — | Authentification → JWT |
| `GET` | `/api/health` | — | Health check |
| `GET` | `/api/export?apiKey=` | Clé API | Export complet (même payload que le backup) |
| `GET` | `/api/editions` | JWT | Liste des sets triés |
| `GET` | `/api/cards?editionId=&q=` | JWT | Cartes filtrées |
| `GET` | `/api/cards/{id}` | JWT | Détail carte |
| `GET` | `/api/cards/lookup?number=&editionId=` | JWT | Lookup scanner |
| `GET` | `/api/cards/fingerprints` | JWT | Empreintes visuelles |
| `GET/POST` | `/api/collection` | JWT | Collection possédée |
| `GET` | `/api/collection/recent?limit=20` | JWT | Derniers scans (10/20/25/50) |
| `PUT` | `/api/collection/{cardId}` | JWT | Modifier quantités |
| `DELETE` | `/api/collection/{cardId}` | JWT | Supprimer de la collection |
| `GET` | `/api/statistics` | JWT | Statistiques globales et par set |
| `GET/PUT` | `/api/admin/settings/{key}` | JWT | Paramètres applicatifs |
| `GET` | `/api/admin/progress` | JWT | Progression des opérations async |
| `POST` | `/api/admin/sync/url` | JWT | Sync LorcaJson depuis URL |
| `POST` | `/api/admin/sync/file` | JWT | Sync LorcaJson depuis fichier |
| `GET` | `/api/admin/lorcajson-url` | JWT | URL LorcaJson configurée |
| `POST` | `/api/admin/compute-hashes` | JWT | Calcul d'empreintes visuelles |
| `GET` | `/api/admin/backup` | JWT | Sauvegarde complète |
| `POST` | `/api/admin/restore` | JWT | Restauration complète |
| `POST` | `/api/admin/import/companion?merge=` | JWT | Import Lorcana Companion |
| `GET` | `/api/admin/apikeys` | JWT | Liste des clés API |
| `POST` | `/api/admin/apikeys` | JWT | Créer une clé API |
| `DELETE` | `/api/admin/apikeys/{id}` | JWT | Supprimer une clé API |

---

## Sécurité

- JWT stateless (HMAC-SHA256), BCrypt pour les mots de passe.
- Les clés API sont stockées en hash SHA-256 uniquement — la valeur en clair n'est jamais persistée.
- L'accès à `/api/export` est validé par un filtre dédié (`ApiKeyAuthFilter`) dans la chaîne Spring Security.
- **Changer `APP_PASSWORD` et `JWT_SECRET` en production.**

---

## Compatibilité import/export

Le contrat de backup/export suit une politique **N/N-1**.

- Les payloads de version courante (N) et version précédente (N-1) doivent rester importables.
- Toute évolution de schéma impose des tests unitaires et d'intégration de compatibilité.
- La CI exécute un gate dédié sur ces tests.

---

## Gouvernance de la vérité produit

- Phase 1 : le code reste la vérité d'exécution pendant la transition.
- Phase 2 : OpenSpec devient la source canonique.
- Toute évolution fonctionnelle doit mettre à jour README + CHANGELOG.

Références internes : `docs/source-of-truth-governance.md`, `docs/technical-decisions.md`

---

## Historique des modifications

Voir [CHANGELOG.md](CHANGELOG.md) pour la liste complète des changements.
