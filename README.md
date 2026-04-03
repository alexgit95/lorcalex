# Lorcalex — Gestionnaire de collection Lorcana (FR)

Application web mobile-first pour gérer votre collection de cartes Disney Lorcana **en français**.  
Le frontend HTML/JS/CSS vanilla est **inclus dans le JAR Spring Boot** — un seul binaire à déployer, **aucun npm ni Node.js requis**.

---

## Fonctionnalités

| Écran | Description |
|-------|-------------|
| **Collection** | Cartes triées par set et numéro croissants, filtrage possédées/manquantes, recherche, ajout/retrait. Numéro de set affiché sur chaque carte. |
| **Visualisation carte** | Clic sur une carte → grande image plein-écran, compteur de possession, modification de la quantité directement. |
| **Statistiques** | Graphiques (donut, barres empilées) : progression globale, par set, par rareté |
| **Scanner** | Scanner **OCR caméra en continu** : lecture du code bas-gauche (`N/TOTAL • FR • SET`), arrêt automatique à la détection, vue de confirmation, ajout d'exemplaire, reprise rapide du scan. |
| **Administration** | Import du catalogue LorcaJson (URL/fichier), **sauvegarde/restauration complètes** (catalogue + collection + paramètres), export/import JSON de la collection, import **Lorcana Companion** (mode fusion/remplacement) avec barre de progression |
| **Se souvenir de moi** | Option à la connexion pour 12 mois d'authentification sans reconnexion |

---

## Architecture

```
lorcalex-main/
├── pom.xml
└── src/main/
    ├── java/com/alexgit95/
    │   ├── model/           # Entités JPA (Card, Edition, UserCollection…)
    │   ├── repository/      # Interfaces Spring Data
    │   ├── service/         # LorcaJsonService (import + hash), CardService, CollectionService…
    │   ├── controller/      # RestControllers + SpaController
    │   ├── security/        # JWT + Spring Security
    │   └── config/          # SecurityConfig, SpaController
    └── resources/
        ├── application.properties              # Port 8181
        ├── application-local.properties        # SQLite (dev)
        ├── application-docker.properties       # PostgreSQL (prod)
        └── static/                             # Frontend vanilla (servi par Spring Boot)
            ├── index.html
          ├── app.js                          # Logique SPA complète (router, pages, API, scanner OCR continu)
            └── app.css                         # Styles
```

### Bases de données

| Profil | BDD | Usage |
|--------|-----|-------|
| `local` (défaut) | SQLite `lorcalex.db` | Développement local, zero config |
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
| `JWT_SECRET` | *(valeur interne)* | Secret JWT (**à changer en prod !**, min 32 caractères) |
| `DB_PASSWORD` | `lorcalex` | Mot de passe PostgreSQL |
| `APP_PORT` | `8181` | Port exposé sur l'hôte |

### docker-compose (build + lancement)

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

**⚠️ Aucune carte n'est pré-chargée au démarrage.** L'import se fait manuellement depuis la page **Administration**.

### Par URL (recommandé)

1. Ouvrir **Administration**.
2. L'URL par défaut `https://lorcanajson.org/files/current/fr/allCards.json` est pré-remplie.
3. Cliquer **🔄 Importer depuis l'URL**.

### Par fichier

1. Télécharger `allCards.json` depuis [lorcanajson.org](https://lorcanajson.org/files/current/fr/allCards.json).
2. Ouvrir **Administration → 📂 Import depuis un fichier**.
3. Sélectionner le fichier téléchargé.

> **Note :** Le scanner actuel fonctionne via OCR du code imprimé sur la carte. Le calcul d'empreintes visuelles côté serveur est conservé comme fonctionnalité de maintenance/compatibilité.

---

## Se souvenir de moi

Lors de la connexion, cocher **"Se souvenir de moi (12 mois)"** pour générer un JWT valide 12 mois.  
Sans cette option, la session expire après 24 heures.

Le token est stocké dans `localStorage` (avec "Se souvenir de moi") ou `sessionStorage` (sans).

---

## Collection — Tri et affichage

- Cartes toujours triées par **numéro de set croissant**, puis par **numéro de carte croissant**.
- Chaque carte affiche son numéro de set (ex : `S1·#42`).
- Le sélecteur de set affiche : **Set 1 — Premier Chapitre**, **Set 2 — L'Ascension des Floodborn**, etc.

---

## Visualisation de carte

Cliquer sur une carte dans la collection ouvre un écran détail avec :
- **Grande image** de la carte (pleine largeur)
- **Set + numéro de carte + nom + rareté**
- **Compteur de possession** : modifier le nombre d'exemplaires directement (bouton + / −)

---

## Scanner de cartes (OCR continu caméra)

### Principe

1. Le scanner capture la carte dans le cadre caméra puis extrait la zone de code située en bas-gauche.
2. Un OCR (Tesseract.js v5) lit le format imprimé : `N/TOTAL • FR • SET`.
3. L'application cherche la carte via son numéro puis affine par numéro de set quand disponible.
4. En mode continu, la capture tourne en boucle et s'arrête automatiquement dès qu'une carte est reconnue.

### Utilisation

1. Ouvrir **Scanner**.
2. Présenter la carte dans le cadre caméra.
3. Le scan continu démarre automatiquement et s'arrête dès qu'une carte est détectée (bip + vibration).
4. La vue caméra disparaît pour afficher uniquement la carte détectée, la quantité déjà en collection, puis :
  - **Ajouter un exemplaire** : ajoute 1 exemplaire, puis relance automatiquement la caméra + scan continu.
  - **Recommencer** : revient à la caméra et relance le scan.

> **Pré-requis** : le catalogue doit avoir été importé au moins une fois pour que les cartes soient disponibles en base.

### Fallback manuel

Si la reconnaissance échoue (mauvaise luminosité, reflet, code flou), utilisez la saisie manuelle du **numéro de carte** et du **set** dans l'écran scanner.

---

## API REST

Toutes les routes nécessitent un Bearer JWT (sauf `/api/auth/login`).

| Méthode | Route | Description |
|---------|-------|-------------|
| `POST` | `/api/auth/login` | Authentification → JWT (`rememberMe` pour 12 mois) |
| `GET` | `/api/editions` | Liste des sets (triés par numéro) |
| `GET` | `/api/cards?editionId=&q=` | Cartes filtrées (triées set+numéro) |
| `GET` | `/api/cards/{id}` | Détail carte |
| `GET` | `/api/cards/lookup?number=&editionId=` | Lookup par numéro (scanner) |
| `GET` | `/api/cards/fingerprints` | Empreintes visuelles (compatibilité / maintenance) |
| `GET/POST` | `/api/collection` | Collection possédée |
| `PUT` | `/api/collection/{cardId}` | Modifier quantité |
| `DELETE` | `/api/collection/{cardId}` | Supprimer de la collection |
| `GET` | `/api/statistics` | Statistiques complètes |
| `GET/PUT` | `/api/admin/settings/{key}` | Paramètres admin |
| `GET` | `/api/admin/progress` | État de progression des opérations asynchrones (sync/import/hash) |
| `POST` | `/api/admin/sync/url` | Import LorcaJson depuis URL |
| `POST` | `/api/admin/sync/file` | Import LorcaJson depuis fichier multipart |
| `GET` | `/api/admin/lorcajson-url` | URL LorcaJson configurée |
| `GET` | `/api/admin/backup` | Sauvegarde complète (éditions + cartes + collection + paramètres) |
| `POST` | `/api/admin/restore` | Restauration complète depuis une sauvegarde |
| `POST` | `/api/admin/import/companion?merge=true\|false` | Import Companion (asynchrone) avec mode fusion/remplacement |

---

## Sauvegarde & Restauration complètes

Accessible depuis **Administration → 💾 Sauvegarde & Restauration complètes**.

Cette fonctionnalité permet de **sauvegarder l'intégralité de l'application** et de la **restaurer sur une instance vierge**, sans avoir à re-synchroniser le catalogue LorcaJson.

### Contenu de la sauvegarde

| Donnée | Description |
|--------|-------------|
| `editions` | Tous les sets (code, nom, numéro, date de sortie, URL logo…) |
| `cards` | L'intégralité du catalogue de cartes (tous les champs, dont `externalId` et `imageHash`) |
| `collection` | Vos quantités possédées par carte |
| `settings` | Tous les paramètres (dont `stats_enabled_sets` : les sets suivis dans l'onglet Stats) |

### Format du fichier

```json
{
  "backupDate": "2026-03-27T10:00:00",
  "version": "2",
  "totalEditions": 10,
  "totalCards": 2048,
  "totalCollection": 350,
  "editions": [ { "id": 1, "code": "TFC", "name": "Premier Chapitre", "setNumber": 1, ... } ],
  "cards":    [ { "externalId": "abc123", "cardNumber": 1, "editionCode": "TFC", "imageHash": 12345, ... } ],
  "collection": [ { "externalId": "abc123", "cardNumber": 1, "editionCode": "TFC", "quantity": 2 } ],
  "settings": [ { "key": "stats_enabled_sets", "value": "1,2,3", "description": "..." } ]
}
```

### Sauvegarde

Cliquer **⬇️ Télécharger la sauvegarde complète** → génère `lorcalex-backup-YYYY-MM-DD.json`.

### Restauration

1. Cliquer **🔄 Restaurer depuis une sauvegarde** et sélectionner le fichier de backup.
2. **Une confirmation est demandée** — l'opération est irréversible.
3. La restauration :
   - **Efface** tout : collection, cartes, éditions, paramètres.
   - **Recrée** tout depuis le fichier dans le bon ordre.
   - **Remappe automatiquement** les IDs d'éditions dans `stats_enabled_sets` (les IDs peuvent changer après recréation).

> **Utilisation typique** : migration vers un nouveau serveur, récupération après une réinitialisation de la base de données.

---

## Import depuis Lorcana Companion

Accessible depuis **Administration → Import depuis Lorcana Companion**.

### Format supporté

- Export JSON de l'application Companion contenant la clé `OwnedCardQuantitiesV2`.
- Les entrées dupliquées d'une même carte (ex: `Regular` + `Foiled`) sont additionnées.

### Modes d'import

- **Fusion** (par défaut) : ajoute les quantités importées aux quantités déjà présentes.
- **Remplacement** : remplace les quantités existantes par celles du fichier importé.

### Progression

- L'import Companion est asynchrone.
- Une barre de progression est affichée dans l'Administration, avec les phases:
  - `📄 Analyse Companion`
  - `📥 Import Companion`
  - puis `✅ Terminé` / `❌ Erreur`

### Pré-requis important

- Le mapping Companion repose sur l'`externalId` des cartes.
- Si beaucoup de cartes sont indiquées comme non trouvées, relancez d'abord une synchronisation du catalogue LorcaJson en Administration.

---

## Historique des modifications

Voir [CHANGELOG.md](CHANGELOG.md) pour la liste complète de tous les changements, améliorations et corrections.

---

## Sécurité

- JWT stateless (HS256), BCrypt pour les mots de passe.
- **Changer `APP_PASSWORD` et `JWT_SECRET` en production.**









