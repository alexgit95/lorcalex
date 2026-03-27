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
| **Scanner** | Reconnaissance par **empreinte visuelle** (perceptual hash) calculée côté client — aucun OCR, capture de la carte entière. Bip + vibration à l'ajout. |
| **Administration** | Import du catalogue LorcaJson (par URL ou fichier), import/export JSON de la collection |
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
            ├── app.js                          # Logique SPA complète (router, pages, API, scanner hash)
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

> **Note :** Lors de l'import, le serveur télécharge chaque vignette de carte pour calculer son **empreinte visuelle** (perceptual hash). Cette opération peut prendre plusieurs minutes selon le nombre de sets importés.

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

## Scanner de cartes (empreinte visuelle)

### Principe

1. **À l'import** : le serveur calcule un *average hash* 8×8 (64 bits) pour chaque vignette de carte.
2. **Au scan** : le client charge toutes les empreintes (`GET /api/cards/fingerprints`), capture la carte entière via la caméra, calcule son hash et trouve la carte la plus proche (distance de Hamming).
3. Tout le calcul de comparaison s'effectue **100 % côté client** (JavaScript / BigInt).

### Utilisation

1. Ouvrir **Scanner**.
2. Centrer la carte **entière** dans le cadre (portrait, fond uni de préférence).
3. Appuyer sur **📷 Identifier la carte**.
4. La carte reconnue est ajoutée automatiquement (bip + vibration).

> **Pré-requis** : le catalogue doit avoir été importé au moins une fois pour que les empreintes soient disponibles.

### Fallback manuel

Si la reconnaissance échoue (mauvaise luminosité, reflet), utilisez la saisie du **numéro de carte** situé en bas de la zone de champ de la carte.

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
| `GET` | `/api/cards/fingerprints` | Empreintes légères pour scanner client |
| `GET/POST` | `/api/collection` | Collection possédée |
| `PUT` | `/api/collection/{cardId}` | Modifier quantité |
| `DELETE` | `/api/collection/{cardId}` | Supprimer de la collection |
| `GET` | `/api/statistics` | Statistiques complètes |
| `GET/PUT` | `/api/admin/settings/{key}` | Paramètres admin |
| `POST` | `/api/admin/sync/url` | Import LorcaJson depuis URL |
| `POST` | `/api/admin/sync/file` | Import LorcaJson depuis fichier multipart |
| `GET` | `/api/admin/lorcajson-url` | URL LorcaJson configurée |
| `GET` | `/api/admin/export` | Export JSON de la collection |
| `POST` | `/api/admin/import` | Import JSON de la collection |

---

## Import / Export de la collection

Accessible depuis **Administration → Collection — Import / Export**.

### Export
Génère un fichier `lorcalex-export-YYYY-MM-DD.json` :

```json
{
  "exportDate": "2026-03-27T10:00:00",
  "version": "1",
  "totalEntries": 42,
  "collection": [
    { "cardNumber": 1, "editionCode": "1", "cardName": "Ariel - Sur des jambes humaines", "rarity": "Inhabituelle", "quantity": 2 }
  ]
}
```

### Import
Importe un fichier précédemment exporté.  
- Si la carte est déjà dans la collection → quantité **mise à jour**.  
- Si la carte est inconnue → entrée **ignorée**.  
- Les cartes absentes du fichier **ne sont pas supprimées**.


## Sécurité

- JWT stateless (HS256), BCrypt pour les mots de passe.
- **Changer `APP_PASSWORD` et `JWT_SECRET` en production.**








