# Lorcalex — Gestionnaire de collection Lorcana

Application web mobile-first pour gérer votre collection de cartes Disney Lorcana.  
Le frontend HTML/JS/CSS vanilla est **inclus dans le JAR Spring Boot** — un seul binaire à déployer, **aucun npm ni Node.js requis**.

---

## Fonctionnalités

| Écran | Description |
|-------|-------------|
| **Collection** | Toutes les cartes triées par édition & numéro, filtrage possédées/manquantes, recherche, ajout/retrait |
| **Statistiques** | Graphiques (donut, barres empilées) : progression globale, par édition, par rareté |
| **Scanner** | Reconnaissance OCR client (Tesseract.js CDN) du numéro de carte via caméra, bip + vibration à l'ajout |
| **Administration** | Activation/désactivation de l'API externe, synchronisation des cartes, **import/export JSON de la collection** |

---

## Architecture

```
lorcalex-main/
├── backend/                   # Projet Maven / Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/lorcalex/
│       │   ├── model/           # Entités JPA (Card, Edition, UserCollection…)
│       │   ├── repository/      # Interfaces Spring Data
│       │   ├── service/         # Logique métier
│       │   ├── controller/      # RestControllers + SpaController
│       │   ├── security/        # JWT + Spring Security
│       │   └── config/          # SecurityConfig, SpaController
│       └── resources/
│           ├── application.properties              # Port 8181
│           ├── application-local.properties        # SQLite (dev)
│           ├── application-docker.properties       # PostgreSQL (prod)
│           └── static/                             # Frontend vanilla (servi par Spring Boot)
│               ├── index.html
│               ├── app.js                          # Logique SPA complète (router, pages, API)
│               └── app.css                         # Styles
├── Dockerfile                 # Multi-stage : Maven → JRE (sans Node)
├── docker-compose.yml         # App + PostgreSQL
└── README.md
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
cd backend
mvn spring-boot:run
# Application accessible sur http://localhost:8181
```

### Build du JAR autonome

```bash
cd backend
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

### Portainer — Image pré-buildée (recommandé en prod)

```bash
# Sur votre poste de build :
docker build -t lorcalex:latest .
docker tag lorcalex:latest votre-registry/lorcalex:latest
docker push votre-registry/lorcalex:latest
```

Dans `docker-compose.yml`, remplacer le bloc `build:` par :
```yaml
image: votre-registry/lorcalex:latest
```

---

## API REST

Toutes les routes nécessitent un Bearer JWT (sauf `/api/auth/login`).

| Méthode | Route | Description |
|---------|-------|-------------|
| `POST` | `/api/auth/login` | Authentification → JWT |
| `GET` | `/api/editions` | Liste des éditions |
| `GET` | `/api/cards?editionId=&q=` | Cartes filtrées |
| `GET` | `/api/cards/{id}` | Détail carte |
| `GET` | `/api/cards/lookup?number=&editionId=` | Lookup par numéro (scanner) |
| `GET/POST` | `/api/collection` | Collection possédée |
| `PUT` | `/api/collection/{cardId}` | Modifier quantité |
| `DELETE` | `/api/collection/{cardId}` | Supprimer de la collection |
| `GET` | `/api/statistics` | Statistiques complètes |
| `GET/PUT` | `/api/admin/settings/{key}` | Paramètres admin |
| `POST` | `/api/admin/sync` | Sync API externe |
| `GET` | `/api/admin/export` | **Export JSON de la collection** |
| `POST` | `/api/admin/import` | **Import JSON de la collection** |

---

## Import / Export de la collection

Accessible depuis **Administration → Import / Export**.

### Export
Génère un fichier `lorcalex-export-YYYY-MM-DD.json` contenant toutes les cartes possédées :

```json
{
  "exportDate": "2026-03-26T10:00:00",
  "version": "1",
  "totalEntries": 42,
  "collection": [
    { "cardNumber": 1, "editionCode": "TFC", "cardName": "Elsa - Spirit of Winter", "rarity": "Legendary", "quantity": 2 }
  ]
}
```

### Import
Importe un fichier précédemment exporté. Chaque entrée est identifiée par `cardNumber` + `editionCode`.  
- Si la carte est déjà dans la collection → la quantité est **mise à jour**.  
- Si la carte est inconnue (pas encore synchronisée) → l'entrée est **ignorée**.  
- Les cartes non présentes dans le fichier d'import **ne sont pas supprimées**.

---

## Scanner de cartes (OCR client)

1. **Capture** photo via l'API caméra du navigateur (caméra arrière mobile).
2. **Recadrage** automatique du bas de la carte (zone numéro).
3. **OCR** Tesseract.js WASM — **100 % côté client, aucun appel serveur**.
4. **Lookup** backend par numéro détecté.
5. **Ajout automatique** + bip (Web Audio API) + vibration haptique.
6. **Fallback manuel** si l'OCR échoue.

---

## Données Lorcana

Aucune donnée n'est pré-chargée au démarrage (ni éditions, ni cartes).  
Pour renseigner le catalogue : **Admin → Activer l'API externe → Synchroniser les cartes**.  
Source par défaut : `https://api.lorcana-api.com/cards/all`

Vous pouvez aussi importer une collection précédemment exportée via **Admin → Import / Export**.

---

## Sécurité

- JWT stateless (HS256), BCrypt pour les mots de passe.
- **Changer `APP_PASSWORD` et `JWT_SECRET` en production.**








