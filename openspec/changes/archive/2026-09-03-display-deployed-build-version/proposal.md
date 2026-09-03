## Why

L'administrateur ne peut pas identifier depuis l'interface quelle version de l'application est réellement déployée. Afficher l'identité du build facilite la vérification d'un déploiement et le diagnostic d'un incident sans accès au conteneur.

## What Changes

- Exposer aux administrateurs l'identité du build déployé: la version Maven et le commit Git ayant produit l'artefact.
- Afficher cette identité dans l'en-tête de la page Administration au format `version - SHA court`.
- Transmettre de façon explicite la révision du build de la chaîne de publication Docker jusqu'à l'application empaquetée.
- Prévoir une valeur de repli explicite lorsque la révision Git est indisponible.

## Capabilities

### New Capabilities
- `deployed-build-identity`: publication et affichage sécurisés de la version et de la révision Git d'un build déployé.

### Modified Capabilities

_Aucune._

## Impact

- [src/main/java/com/alexgit95/controller/AdminController.java](../../../../src/main/java/com/alexgit95/controller/AdminController.java): nouvel endpoint d'administration pour les métadonnées de build.
- [src/main/resources/static/app.js](../../../../src/main/resources/static/app.js): chargement et rendu dans l'en-tête Administration.
- [pom.xml](../../../../pom.xml) et [Dockerfile](../../../../Dockerfile): génération et intégration des métadonnées au JAR.
- [.github/workflows/docker-publish.yml](../../../../.github/workflows/docker-publish.yml): transmission du SHA du workflow au build Docker.
- Tests contrôleur et vérification du paquet Maven.